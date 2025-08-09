package io.github.forky.tasks

import io.github.diskria.dsl.shell.GitShell
import io.github.diskria.utils.kotlin.BracketsType
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.*
import io.github.diskria.utils.kotlin.extensions.common.combineChars
import io.github.diskria.utils.kotlin.extensions.common.fileName
import io.github.diskria.utils.kotlin.extensions.generics.containsIgnoreCase
import io.github.diskria.utils.kotlin.extensions.generics.foldChain
import io.github.diskria.utils.kotlin.extensions.generics.joinByNewLine
import io.github.diskria.utils.kotlin.extensions.primitives.*
import io.github.forky.ForkyPlugin
import io.github.forky.config.ForkyConfig
import io.github.forky.config.models.GlobPattern
import io.github.forky.config.models.MovedFile
import io.github.forky.errors.*
import io.github.forky.parser.ForkyPatterns
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.xml.XMLAttributePattern
import io.github.forky.parser.segments.Segment
import io.github.forky.utils.CodeLink
import kotlinx.coroutines.*
import org.apache.tika.Tika
import org.gradle.kotlin.dsl.support.normaliseLineSeparators
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("NewApi")
abstract class ForkyCheckTask : ForkyTask(
    "Checks that the code conforms to the principles defined by the Forky architecture"
) {
    private val segmentMarker: Char by lazy { config.segmentMarker }

    private val ignoredFilePatterns: List<GlobPattern> by lazy {
        config.scopes.flatMap { it.ignoredPatterns }
    }
    private val deletedFilePatterns: List<GlobPattern> by lazy {
        config.scopes.flatMap { it.deletedPatterns }
    }
    private val movedFiles: List<MovedFile> by lazy {
        config.scopes.flatMap { it.movedFiles }
    }

    private val tika: Tika by lazy { Tika() }

    private val maxWorkers: Int by lazy {
        System.getenv(MAX_WORKERS_ENVIRONMENT_VARIABLE)?.toInt()?.positiveOrNull()
            ?: config.maxWorkers.positiveOrNull()
            ?: Runtime.getRuntime().availableProcessors()
    }
    private val parallel: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(maxWorkers)
    }
    private val errors: ConcurrentLinkedQueue<ForkyCheckError> by lazy {
        ConcurrentLinkedQueue()
    }
    private val binaryFileExtensions: MutableSet<String> by lazy {
        Collections.synchronizedSet(mutableSetOf())
    }
    private val blackList: List<String> by lazy {
        listOf(
            GitShell.DOT_GIT,
        )
    }

    override fun runTask() {
        ForkyConfig.reload()

        prepareUpstream()

        runBlocking {
            runChecker()
        }
    }

    private suspend fun runChecker() = coroutineScope {
        ForkyConfig.upstreamRoot.walk().filterNot { it.name in blackList }.map { upstreamFile ->
            val relativePath = upstreamFile.relativeTo(ForkyConfig.upstreamRoot).toString()

            async(parallel) {
                val forkFile = when (val locationResult = locateForkFile(relativePath)) {
                    is LocatedForkFile -> locationResult.absoluteFile
                    is SkippedForkFile -> return@async
                    is ForkFileLocationError -> {
                        errors.add(locationResult.error)
                        return@async
                    }
                }

                val isDirectoryUpstream = upstreamFile.isDirectory
                val isDirectoryFork = forkFile.isDirectory
                if (isDirectoryFork != isDirectoryUpstream) {
                    errors.add(
                        DirectoryTypeMismatchError(
                            relativePath,
                            isDirectoryUpstream,
                            isDirectoryFork
                        )
                    )
                    return@async
                }
                if (isDirectoryUpstream) {
                    return@async
                }

                val isBinaryUpstream = isBinaryFile(upstreamFile)
                val isBinaryFork = isBinaryFile(forkFile)
                if (isBinaryFork != isBinaryUpstream) {
                    errors.add(FileTypeMismatchError(relativePath, isBinaryUpstream, isBinaryFork))
                    return@async
                }

                if (isBinaryUpstream) {
                    if (config.isBinaryFileExtensionsDebuggingEnabled) {
                        binaryFileExtensions.add(upstreamFile.extension)
                    }

                    val upstreamChecksum = upstreamFile.getChecksum(config.binaryChecksumAlgorithm)
                    val forkChecksum = forkFile.getChecksum(config.binaryChecksumAlgorithm)
                    if (forkChecksum != upstreamChecksum) {
                        errors.add(BinaryChecksumMismatchError(relativePath))
                    }
                    return@async
                }

                if (!config.isCodeChecksSkipped) {
                    compareCode(upstreamFile, forkFile)?.let { errors.add(it) }
                }
            }
        }.toList().awaitAll()

        if (config.isBinaryFileExtensionsDebuggingEnabled) {
            val sortedExtensions = synchronized(binaryFileExtensions) {
                binaryFileExtensions.toSortedSet()
            }
            if (sortedExtensions.isNotEmpty()) {
                println("Detected binary file extensions:")
                println(sortedExtensions)
            }
        }

        if (errors.isNotEmpty()) {
            error(errors.joinByNewLine(linesCount = 2))
        }
        println(
            ForkyPlugin.FORKY_NAME.wrapWithBrackets(BracketsType.SQUARE) + Constants.Char.SPACE +
                    Constants.Emoji.CHECK + Constants.Char.SPACE + "Check passed"
        )
    }

    @Suppress("NewApi")
    private fun prepareForkCode(forkFile: File, shouldEndWithNewLine: Boolean): String? {
        val codeBuilder = StringBuilder()
        val segmentsStack = mutableListOf<Segment>()

        forkFile.readByLinesIndexed { lineIndex, forkCodeLine ->
            val codeLine = forkCodeLine.normaliseLineSeparators()
            if (codeLine.isEmpty() || segmentsStack.isEmpty() && forkyName !in codeLine) {
                codeBuilder.appendLine(codeLine)
                return@readByLinesIndexed
            }
            val segments = ForkyPatterns.findSegments(codeLine, lineIndex)
            if (segments.isEmpty()) {
                codeBuilder.appendLine(codeLine)
                return@readByLinesIndexed
            }

            segments.forEach { segment ->
                val unknownScopes = segment.scopes.filterNot { it.id in ForkyConfig.scopeIds }
                if (unknownScopes.isNotEmpty()) {
                    unknownScopes.forEach { unknownScope ->
                        errors.add(
                            UnknownScopeError(
                                unknownScope.id,
                                CodeLink.of(forkFile, lineIndex, unknownScope.charIndex)
                            )
                        )
                    }
                    return null
                }

                val currentPattern = segment.pattern
                val currentScopeIds = segment.scopes.map { it.id }.toSet()
                val segmentCharIndex = segment.charIndex

                val previousSegment = segmentsStack.lastOrNull()
                val previousPattern = previousSegment?.pattern
                val previousScopeIds = previousSegment?.scopes?.map { it.id }.orEmpty().toSet()

                if (currentPattern is Startable && segment.isStart) {
                    if (previousPattern is Startable &&
                        !previousPattern.isNestedAllowed(currentPattern)
                    ) {
                        val segmentLink = CodeLink.of(forkFile, lineIndex, segmentCharIndex)
                        errors.add(UnexpectedSegmentOpenError(segmentLink))
                        return null
                    }
                    segmentsStack.addLast(segment)
                } else if (currentPattern is Endable && !segment.isStart) {
                    if (previousPattern !is Startable ||
                        !currentPattern.isStartedBy(previousPattern) ||
                        !previousPattern.isEndedBy(currentPattern)
                    ) {
                        val segmentLink = CodeLink.of(forkFile, lineIndex, segmentCharIndex)
                        errors.add(UnexpectedSegmentCloseError(segmentLink))
                        return null
                    }
                    if (currentPattern is XMLAttributePattern &&
                        currentScopeIds != previousScopeIds
                    ) {
                        val firstAttributeLink = CodeLink.of(
                            forkFile, previousSegment.lineIndex, previousSegment.charIndex
                        )
                        val secondAttributeLink = CodeLink.of(forkFile, lineIndex, segmentCharIndex)
                        errors.add(
                            XMLAttributeScopesMismatchError(
                                previousScopeIds,
                                currentScopeIds,
                                firstAttributeLink,
                                secondAttributeLink
                            )
                        )
                        return null
                    }
                    segmentsStack.removeLast()
                }
            }

            val markedCodeLine = segments
                .sortedByDescending { it.charIndex }
                .foldChain(codeLine) { segment ->
                    insertAt(
                        segment.charIndex,
                        combineChars(segmentMarker, getSegmentBoundaryMarker(segment.isStart))
                    )
                }
            codeBuilder.appendLine(markedCodeLine)
        }

        val isEndsWithNewLine = codeBuilder.isEndsWithNewLine()
        if (shouldEndWithNewLine) {
            if (!isEndsWithNewLine) {
                codeBuilder.append(Constants.Char.NEW_LINE)
            }
        } else {
            if (isEndsWithNewLine) {
                codeBuilder.deleteCharAt(codeBuilder.lastIndex)
            }
        }
        return codeBuilder.toString()
    }

    private fun compareCode(upstreamFile: File, forkFile: File): ForkyCheckError? {
        val upstreamCode = upstreamFile.readText().normaliseLineSeparators()
        val forkCode = prepareForkCode(forkFile, upstreamCode.isEndsWithNewLine()) ?: return null

        var upstreamCharIndex = 0
        var forkCharIndex = 0

        var upstreamCodeLink = CodeLink.INITIAL
        var forkCodeLink = CodeLink.INITIAL

        val isInsideSegment = AtomicBoolean()
        val isInsideWhitespacesAfterSegment = AtomicBoolean()

        val moveToNextChar = { isFork: Boolean, char: Char ->
            if (isFork) {
                forkCharIndex++
                forkCodeLink = forkCodeLink.move(char)
            } else {
                upstreamCharIndex++
                upstreamCodeLink = upstreamCodeLink.move(char)
            }
        }

        val findNextNonWhitespaceCharIndex = { isFork: Boolean ->
            val code = if (isFork) forkCode else upstreamCode
            val index = if (isFork) forkCharIndex else upstreamCharIndex
            code.indexOfFirstOrNull(index) { !it.isWhitespace() } ?: code.length
        }

        while (forkCharIndex < forkCode.length) {
            val forkChar = forkCode[forkCharIndex]
            if (forkChar == segmentMarker) {
                when (forkCode.getNextOrNull(forkCharIndex)) {
                    getSegmentBoundaryMarker(true) -> {
                        if (isInsideSegment.isTrue) {
                            return UnexpectedSegmentOpenError(forkCodeLink.toLink(forkFile))
                        }
                        isInsideSegment.setTrue()
                    }

                    getSegmentBoundaryMarker(false) -> {
                        if (isInsideSegment.isFalse) {
                            return UnexpectedSegmentCloseError(forkCodeLink.toLink(forkFile))
                        }
                        isInsideSegment.setFalse()
                        isInsideWhitespacesAfterSegment.setTrue()
                    }

                    else -> return SegmentMarkerConflictError(forkCodeLink.toLink(forkFile))
                }
                forkCharIndex += 2
                continue
            }
            if (isInsideSegment.isTrue) {
                moveToNextChar(true, forkChar)
                continue
            }
            val isForkWhitespace = forkChar.isWhitespace()
            if (upstreamCharIndex >= upstreamCode.length) {
                if (isForkWhitespace) {
                    moveToNextChar(true, forkChar)
                    continue
                }
                return InvalidSegmentError(forkCodeLink.toLink(forkFile))
            }
            val upstreamChar = upstreamCode[upstreamCharIndex]
            val isUpstreamWhitespace = upstreamChar.isWhitespace()

            if (isInsideWhitespacesAfterSegment.isTrue) {
                if (isForkWhitespace || isUpstreamWhitespace) {
                    if (isForkWhitespace) {
                        moveToNextChar(true, forkChar)
                    }
                    if (isUpstreamWhitespace) {
                        moveToNextChar(false, upstreamChar)
                    }
                    continue
                }
                isInsideWhitespacesAfterSegment.setFalse()
            } else if (isForkWhitespace || isUpstreamWhitespace) {
                val nextForkNonWhitespaceCharIndex = findNextNonWhitespaceCharIndex(true)
                val nextForkChar = forkCode.getOrNull(nextForkNonWhitespaceCharIndex)
                if (nextForkChar == segmentMarker) {
                    forkCharIndex = nextForkNonWhitespaceCharIndex
                    if (isUpstreamWhitespace) {
                        upstreamCharIndex
                            .until(findNextNonWhitespaceCharIndex(false))
                            .forEach { charIndex -> moveToNextChar(false, upstreamCode[charIndex]) }
                    }
                    continue
                }
            }
            if (forkChar != upstreamChar) {
                return CharacterMismatchError(
                    upstreamChar,
                    forkChar,
                    upstreamCodeLink.toLink(upstreamFile),
                    forkCodeLink.toLink(forkFile)
                )
            }
            moveToNextChar(true, forkChar)
            moveToNextChar(false, upstreamChar)
        }
        if (isInsideSegment.isTrue) {
            return UnclosedSegmentError(forkCodeLink.toLink(forkFile))
        }
        if (upstreamCharIndex < upstreamCode.length) {
            return ForkTruncatedError(forkCodeLink.toLink(forkFile))
        }
        return null
    }

    private fun locateForkFile(relativePath: String): ForkFileLocationResult {
        val absoluteFile = ForkyConfig.forkRoot.resolve(relativePath)

        if (ignoredFilePatterns.any { it.matches(relativePath, ForkyConfig.forkRoot) }) {
            if (!absoluteFile.exists()) {
                return ForkFileLocationError(IgnoredFileMissingError(relativePath))
            }
            return SkippedForkFile
        }
        if (deletedFilePatterns.any { it.matches(relativePath, ForkyConfig.forkRoot) }) {
            if (absoluteFile.exists()) {
                return ForkFileLocationError(DeletedFileExistsError(relativePath))
            }
            return SkippedForkFile
        }
        movedFiles.find { it.sourcePath == relativePath }?.let { movedFile ->
            if (absoluteFile.exists()) {
                return ForkFileLocationError(MovedSourceFileStillExistsError(movedFile.sourcePath))
            }
            val destinationPath = movedFile.destinationPath
            val destination = ForkyConfig.forkRoot.resolve(destinationPath)
            if (!destination.exists()) {
                return ForkFileLocationError(MovedDestinationFileMissingError(destinationPath))
            }
            return LocatedForkFile(destination)
        }

        if (!absoluteFile.exists()) {
            return ForkFileLocationError(ForkFileMissingError(relativePath))
        }
        return LocatedForkFile(absoluteFile)
    }

    private fun prepareUpstream() {
        if (config.isCloneAllowed && !ForkyConfig.upstreamRoot.isDirectory) {
            if (ForkyConfig.upstreamRoot.isFile) {
                ForkyConfig.upstreamRoot.deleteOrThrow()
            }
            ForkyConfig.forkGitShell.clone(config.upstreamRemoteUrl, ForkyConfig.upstreamRoot)
        }
        ForkyConfig.upstreamGitShell.run {
            getRemoteNames().filter { it != GitShell.ORIGIN_REMOTE_NAME }.forEach {
                removeRemote(it)
            }
            addRemote(config.upstreamRemoteName, config.upstreamRemoteUrl)
            fetch(config.upstreamRemoteName)
            checkout(config.upstreamCommit)
            hardResetToRemoteCommit(config.upstreamRemoteName, config.upstreamCommit)
        }
    }

    private fun isBinaryFile(file: File): Boolean {
        val mimeType = tika.detect(file)
        val (type, subType) = mimeType.splitToPairOrNull(Constants.Char.SLASH) ?: return true
        if (type == "text" ||
            type == "application" && config.textFileMimeSubtypes.containsIgnoreCase(subType)
        ) {
            return false
        }

        return config.textFileExtensions.containsIgnoreCase(file.extension).not()
    }

    private sealed interface ForkFileLocationResult

    @JvmInline
    private value class LocatedForkFile(val absoluteFile: File) : ForkFileLocationResult

    @JvmInline
    private value class ForkFileLocationError(val error: ForkyCheckError) : ForkFileLocationResult

    object SkippedForkFile : ForkFileLocationResult

    companion object {
        const val DEFAULT_SEGMENT_MARKER: Char = ''

        private const val MAX_WORKERS_ENVIRONMENT_VARIABLE: String = "FORKY_MAX_WORKERS"

        fun getConfigFileName(): String =
            fileName(ForkyPlugin.FORKY_NAME, Constants.File.Extension.TOML)

        private fun getSegmentBoundaryMarker(isStart: Boolean): Char =
            BracketsType.ANGLE.run {
                if (isStart) openingChar
                else closingChar
            }
    }
}

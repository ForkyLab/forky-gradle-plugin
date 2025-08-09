package io.github.forky.errors

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.EscapeMode
import io.github.diskria.utils.kotlin.extensions.primitives.escaped
import io.github.forky.config.ForkyToml
import io.github.forky.tasks.ForkyCheckTask

sealed interface ForkyCheckError

class InvalidForkyNameError(private val name: String) : ForkyCheckError {
    override fun toString() = "Invalid ${ForkyToml.FORKY_NAME}: '$name'. " +
            "Only letters and digits allowed"
}

class InvalidForkPathError(private val path: String) : ForkyCheckError {
    override fun toString() = "Invalid ${ForkyToml.FORK_PATH}: '$path'. " +
            "Path must be relative and point to existing directory"
}

class InvalidUpstreamPathError(private val path: String) : ForkyCheckError {
    override fun toString() = "Invalid ${ForkyToml.UPSTREAM_PATH}: '$path'. " +
            "Path must be relative and point to existing directory"
}

object UpstreamMissingError : ForkyCheckError {
    override fun toString() =
        """
        Upstream directory does not exist, and clone is disabled.
        Either:
        - Enable ${ForkyToml.ALLOW_CLONE} in the config, or
        - Manually create the directory and initialize it as a valid git repository.
        """.trimIndent()
}

object InvalidUpstreamGitRepoError : ForkyCheckError {
    override fun toString() = "The upstream path exists but is not a valid git repository."
}

class IgnoredFileMissingError(private val path: String) : ForkyCheckError {
    override fun toString() = "File $path marked as ignored but does not exist"
}

class ForkFileMissingError(private val path: String) : ForkyCheckError {
    override fun toString() = "File $path does not exist in fork"
}

class DeletedFileExistsError(private val path: String) : ForkyCheckError {
    override fun toString() = "File $path marked as deleted but still exists"
}

class MovedSourceFileStillExistsError(private val path: String) : ForkyCheckError {
    override fun toString() = "File $path marked as moved 'from' but still exists"
}

class MovedDestinationFileMissingError(private val path: String) : ForkyCheckError {
    override fun toString() = "File $path marked as moved 'to' but does not exist"
}

class BinaryChecksumMismatchError(private val path: String) : ForkyCheckError {
    override fun toString() =
        """
        Binary file $path has a different checksum, meaning it was likely modified in the fork.
        Since this is not a text file, it cannot be compared.
        Either:
        - You can configure the hashing algorithm used for this check via the
        `${ForkyToml.BINARY_CHECKSUM_ALGORITHM}` option in the config file.

        - If appropriate, you can treat this file as text via:
        `${ForkyToml.TEXT_FILE_MIME_SUBTYPES}` or `${ForkyToml.TEXT_FILE_EXTENSIONS}`.
        """.trimIndent()
}

class FileTypeMismatchError(
    private val filePath: String,
    private val isTextUpstream: Boolean,
    private val isTextFork: Boolean,
) : ForkyCheckError {
    override fun toString(): String {
        val upstreamType = if (isTextUpstream) "text" else "binary"
        val forkType = if (isTextFork) "text" else "binary"
        return "File $filePath is $upstreamType in upstream, but $forkType in fork"
    }
}

class DirectoryTypeMismatchError(
    private val filePath: String,
    private val isDirUpstream: Boolean,
    private val isDirFork: Boolean,
) : ForkyCheckError {
    override fun toString(): String {
        val upstreamType = if (isDirUpstream) "directory" else "file"
        val forkType = if (isDirFork) "directory" else "file"
        return "File $filePath is $upstreamType in upstream, but $forkType in fork"
    }
}

class CharacterMismatchError(
    private val upstreamChar: Char,
    private val forkChar: Char,
    private val upstreamCodeLink: String,
    private val forkCodeLink: String,
) : ForkyCheckError {
    override fun toString(): String {
        val upstreamLiteral = upstreamChar.escaped(EscapeMode.LITERAL)
        val forkLiteral = forkChar.escaped(EscapeMode.LITERAL)
        return """
               Mismatch between fork and upstream characters:
               Upstream character: '$upstreamLiteral' at $upstreamCodeLink
               Fork character:     '$forkLiteral' at $forkCodeLink
               """.trimIndent()
    }
}

class SegmentMarkerConflictError(private val codeLink: String) : ForkyCheckError {
    override fun toString() =
        """
        File contains the character used as the segment marker: $codeLink
        This file was skipped.
        Please set a different marker via `${ForkyToml.SEGMENT_MARKER}`
        in your `${ForkyCheckTask.getConfigFileName()}`.

        It is recommended to choose a character from the Unicode Private Use Area (PUA)
        """.trimIndent()
}

abstract class ForkCodeSegmentError(private val codeLink: String) : ForkyCheckError {

    protected abstract val reason: String

    override fun toString() = reason + Constants.Char.COLON + Constants.Char.SPACE + codeLink
}

class UnknownScopeError(scopeId: String, codeLink: String) : ForkCodeSegmentError(codeLink) {
    override val reason = "Unknown scope with id `$scopeId`"
}

class UnexpectedSegmentOpenError(codeLink: String) : ForkCodeSegmentError(codeLink) {
    override val reason = "Segment started while a previous segment block is still open"
}

class UnexpectedSegmentCloseError(codeLink: String) : ForkCodeSegmentError(codeLink) {
    override val reason = "Segment closed but no segment block was open"
}

class InvalidSegmentError(codeLink: String) : ForkCodeSegmentError(codeLink) {
    override val reason = "Invalid segment"
}

class UnclosedSegmentError(codeLink: String) : ForkCodeSegmentError(codeLink) {
    override val reason = "Segment started but not closed"
}

class ForkTruncatedError(codeLink: String) : ForkCodeSegmentError(codeLink) {
    override val reason = "Fork code ends before upstream"
}

class XMLAttributeScopesMismatchError(
    private val firstScopeIds: Set<String>,
    private val secondScopeIds: Set<String>,
    private val firstAttributeCodeLink: String,
    private val secondAttributeCodeLink: String,
) : ForkyCheckError {

    override fun toString() =
        """
        Scope mismatch in paired XML attributes:
        - First attribute with scopes: $firstScopeIds at $firstAttributeCodeLink 
        - Second attribute with scopes: $secondScopeIds at $secondAttributeCodeLink 
        Both attributes must declare the same scopes.
        """.trimIndent()
}

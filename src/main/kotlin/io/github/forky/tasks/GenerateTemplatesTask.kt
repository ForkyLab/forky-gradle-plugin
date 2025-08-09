package io.github.forky.tasks

import com.notkamui.keval.Keval
import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.combinators.RegexOr
import io.github.diskria.dsl.regex.extensions.*
import io.github.diskria.dsl.regex.groups.RegexGroup
import io.github.diskria.dsl.regex.primitives.RegexAnyChar
import io.github.diskria.dsl.regex.primitives.RegexCharacterClass
import io.github.diskria.dsl.regex.primitives.RegexWhitespace
import io.github.diskria.dsl.regex.ranges.RegexCharacterRange
import io.github.diskria.utils.kotlin.BracketsType
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.delegates.toAutoNamedPair
import io.github.diskria.utils.kotlin.extensions.common.failWithDetails
import io.github.diskria.utils.kotlin.extensions.generics.foldChain
import io.github.diskria.utils.kotlin.extensions.generics.joinByNewLine
import io.github.diskria.utils.kotlin.extensions.primitives.wrapWithSpace
import io.github.diskria.utils.kotlin.extensions.readByLinesIndexed
import io.github.forky.api.TemplateCodegen
import io.github.forky.comments.CommentStyle
import io.github.forky.comments.CommentStyles
import io.github.forky.comments.SingleLineCommentStyle
import io.github.forky.config.ForkyConfig
import kotlinx.coroutines.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Uses user-supplied lambda (templateCodegen)")
abstract class GenerateTemplatesTask : ForkyTask(
    "Generates code of templates"
) {
    @get:Internal
    abstract val templateCodegen: Property<TemplateCodegen>

    private val idTagRegexPattern: RegexPattern by lazy {
        getTagRegexPattern(
            TAG_ID,
            RegexCharacterClass.of(RegexCharacterRange.LATIN).oneOrMore()
        )
    }

    private val linesTagRegexPattern: RegexPattern by lazy {
        getTagRegexPattern(
            TAG_LINES,
            RegexCharacterClass.of(RegexCharacterRange.DIGITS).oneOrMore()
        )
    }

    private val regionStartRegexes: List<Pair<CommentStyle, Regex>> by lazy {
        CommentStyles.singleLineStyles.map { style ->
            style to buildRegexPattern {
                append(style.getRegionStartComment(REGION_LABEL))
                append(Constants.Char.SPACE)
                append(
                    RegexOr.of(
                        idTagRegexPattern,
                        linesTagRegexPattern,
                    )
                )
            }.toRegex()
        }
    }

    private val templateRegexes: Map<SingleLineCommentStyle, Regex> by lazy {
        CommentStyles.singleLineStyles.associateWith { style ->
            style.commentText(RegexGroup.ofCaptured(RegexAnyChar.oneOrMore()).toString()).toRegex()
        }
    }

    private val regionEndFormats: Map<SingleLineCommentStyle, String> by lazy {
        CommentStyles.singleLineStyles.associateWith { style ->
            style.getRegionEndComment(REGION_LABEL)
        }
    }

    private val maxWorkers: Int by lazy {
        Runtime.getRuntime().availableProcessors()
    }
    private val parallel: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(maxWorkers)
    }

    override fun runTask() {
        runBlocking {
            generateTemplates()
        }
    }

    private suspend fun generateTemplates() = coroutineScope {
        ForkyConfig.forkRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            async(parallel) {
                val templates = findTemplates(file)
                if (templates.isNotEmpty()) {
                    val newCode = templates.sortedByDescending { it.replacementLinesRange.first }
                        .foldChain(file.readText().lines()) { template ->
                            generateTemplate(this, file, template)
                        }
                    file.writeText(newCode.joinByNewLine())
                }
            }
        }
    }

    private fun generateTemplate(codeLines: List<String>, file: File, template: Template): List<String> {
        val generatedCode = when (template) {
            is MultiLineTemplate -> template.generate()
            is ParameterizedTemplate -> {
                template.generate { parameterName ->
                    templateCodegen.get().getParameterValue(file, template.id, parameterName)
                }
            }
        }
        return codeLines.toMutableList().apply {
            val start = template.replacementLinesRange.first
            val end = template.replacementLinesRange.last
            if (start < end) {
                subList(start, end).clear()
            }
            addAll(start, generatedCode.lines())
        }
    }

    private fun findTemplates(file: File): List<Template> {
        var currentRegion: TemplateRegion? = null

        val templates = mutableListOf<Template>()
        file.readByLinesIndexed { lineIndex, line ->
            currentRegion?.let { start ->
                if (lineIndex == start.startLineIndex.inc()) {
                    val regex = templateRegexes[start.commentStyle] ?: return@readByLinesIndexed
                    val (pattern) = line.findSingleMatchGroupValuesOrNull(regex) ?: return@readByLinesIndexed

                    start.pattern = pattern
                } else {
                    if (start.replacementStartLineIndex == null) {
                        start.replacementStartLineIndex = lineIndex
                    }
                    if (line.trim() == regionEndFormats[start.commentStyle]) {
                        val pattern = start.pattern ?: return@readByLinesIndexed
                        val replacementStartLineIndex = start.replacementStartLineIndex
                            ?: return@readByLinesIndexed

                        val replacementLines = replacementStartLineIndex..lineIndex
                        templates.add(
                            when (start) {
                                is ParameterizedTemplateRegion -> {
                                    ParameterizedTemplate(start.id, pattern, replacementLines)
                                }

                                is MultiLineTemplateRegion -> {
                                    MultiLineTemplate(start.linesCount, pattern, replacementLines)
                                }
                            }
                        )
                        currentRegion = null
                    }
                }
                return@readByLinesIndexed
            }

            regionStartRegexes.forEach { (style, regex) ->
                line.findSingleMatchGroupValuesOrNull(regex)?.let { (id, linesCount) ->
                    val isParameterized = id != null
                    val isMultiLineTemplate = linesCount != null
                    if (isParameterized == isMultiLineTemplate) {
                        return@let
                    }
                    if (isParameterized) {
                        currentRegion = ParameterizedTemplateRegion(
                            id,
                            style,
                            lineIndex
                        )
                    } else if (linesCount != null) {
                        currentRegion = MultiLineTemplateRegion(
                            linesCount.toInt(),
                            style,
                            lineIndex
                        )
                    }
                }
            }
        }
        return templates
    }

    private fun getTagRegexPattern(name: String, valueType: RegexPattern): RegexPattern =
        buildRegexPattern {
            append(name)
            append(Constants.Char.EQUAL_SIGN.wrapWithSpace())
            append(RegexGroup.ofCaptured(valueType))
        }.wrapWithBrackets(BracketsType.ANGLE)

    sealed class TemplateRegion(
        val commentStyle: CommentStyle,
        val startLineIndex: Int,
        var pattern: String? = null,
        var replacementStartLineIndex: Int? = null,
    )

    class ParameterizedTemplateRegion(
        val id: String,
        commentStyle: CommentStyle,
        startLineIndex: Int,
    ) : TemplateRegion(commentStyle, startLineIndex)

    class MultiLineTemplateRegion(
        val linesCount: Int,
        commentStyle: CommentStyle,
        startLineIndex: Int,
    ) : TemplateRegion(commentStyle, startLineIndex)

    sealed class Template(val pattern: String, val replacementLinesRange: IntRange) {

        fun generate(evaluate: (String) -> String): String =
            pattern.replaceRegex(expressionRegexPattern) { _, (expression) ->
                evaluate(expression ?: run {
                    val pattern by pattern.toAutoNamedPair()
                    failWithDetails(pattern)
                })
            }

        companion object {
            private val expressionRegexPattern: RegexPattern by lazy {
                RegexGroup.ofCaptured(RegexAnyChar.oneOrMore(isLazy = true))
                    .wrap(RegexWhitespace.zeroOrMore().toString())
                    .wrapWithBrackets(BracketsType.CURLY, count = 2)
            }
        }
    }

    class ParameterizedTemplate(
        val id: String,
        pattern: String,
        replacementLinesRange: IntRange
    ) : Template(pattern, replacementLinesRange)

    class MultiLineTemplate(
        val linesCount: Int,
        pattern: String,
        replacementLinesRange: IntRange
    ) : Template(pattern, replacementLinesRange) {

        fun generate(): String =
            (0 until linesCount).joinByNewLine { lineIndex ->
                generate { mathExpressionString ->
                    Keval.eval(
                        mathExpressionString.replace(
                            LINE_INDEX_PLACEHOLDER,
                            lineIndex.toString()
                        )
                    ).toInt().toString()
                }
            }
    }

    companion object {
        private const val REGION_LABEL = "Template"
        private const val LINE_INDEX_PLACEHOLDER = "lineIndex"

        private const val TAG_ID = "id"
        private const val TAG_LINES = "lines"
    }
}

package io.github.forky.config.models

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.extensions.component1
import io.github.diskria.dsl.regex.extensions.replaceRegex
import io.github.diskria.dsl.regex.extensions.toRegexPattern
import io.github.diskria.dsl.regex.groups.RegexGroup
import io.github.diskria.dsl.regex.primitives.RegexAnyChar
import io.github.diskria.dsl.regex.primitives.RegexCharacterClass
import io.github.diskria.dsl.regex.ranges.RegexCharacterRange
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.*
import io.github.diskria.utils.kotlin.extensions.generics.indexOfFirstOrNull
import io.github.diskria.utils.kotlin.extensions.generics.joinToString
import io.github.diskria.utils.kotlin.extensions.primitives.repeat
import io.github.diskria.utils.kotlin.serialization.SerializableValue
import io.github.diskria.utils.kotlin.serialization.valueSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

@Serializable(with = GlobPattern.Serializer::class)
class GlobPattern(val pattern: String) : SerializableValue<String> {

    private val isGlobPattern: Boolean =
        pattern.contains(WILDCARD) || pattern.contains(NEGATED_MASK)
    private val isNegated: Boolean = pattern.startsWith(NEGATED_MASK)

    private val staticPrefix: String by lazy { extractStaticPrefix(pattern) }
    private val normalizedPattern: String by lazy { normalizePattern(pattern) }

    fun matches(relativePath: String, baseDirectory: File): Boolean {
        if (isGlobPattern) {
            val absolutePath = baseDirectory.resolve(relativePath).canonicalPath.toUnixPath()
            return matchesInternal(absolutePath, baseDirectory) != isNegated
        }
        return relativePath == pattern
    }

    override fun getRawValue(): String = pattern

    override fun toString(): String = pattern

    private fun extractStaticPrefix(pattern: String): String {
        val cleaned = pattern.removePrefix(NEGATED_MASK).removeSuffix(PATH_SEPARATOR)
        val segments = cleaned.split(PATH_SEPARATOR)

        val cutoffIndex = segments.indexOfFirstOrNull { WILDCARD in it } ?: run {
            if (segments.size <= 1) {
                return Constants.Char.EMPTY
            }
            segments.lastIndex
        }
        return segments.take(cutoffIndex).joinToString(PATH_SEPARATOR)
    }

    private fun normalizePattern(pattern: String): String {
        val cleanedPattern = pattern.removePrefix(NEGATED_MASK).removeSuffix(PATH_SEPARATOR)
            .replaceRegex(
                buildRegexPattern {
                    append(
                        RegexGroup
                            .of(PATH_SEPARATOR + RECURSIVE_WILDCARD.regexEscaped())
                            .countsRange(min = 2)
                    )
                    append(PATH_SEPARATOR)
                },
            ) { PATH_SEPARATOR + RECURSIVE_WILDCARD + PATH_SEPARATOR }
            .collapseRepeating(RECURSIVE_WILDCARD + PATH_SEPARATOR)
            .collapseRepeating(PATH_SEPARATOR + RECURSIVE_WILDCARD)

        if (cleanedPattern.startsWith(WILDCARD) && !cleanedPattern.contains(PATH_SEPARATOR)) {
            return RECURSIVE_WILDCARD + PATH_SEPARATOR + cleanedPattern
        }
        val result = if (cleanedPattern.endsWith(PATH_SEPARATOR + RECURSIVE_WILDCARD)) {
            cleanedPattern
        } else {
            cleanedPattern.collapseJoin(RECURSIVE_WILDCARD, PATH_SEPARATOR)
        }
        return result
    }

    private fun matchesInternal(absolutePath: String, baseDirectory: File): Boolean {
        val canonicalBase = when {
            staticPrefix.isEmpty() -> {
                if (normalizedPattern.startsWith(PATH_SEPARATOR)) PATH_SEPARATOR.toString()
                else baseDirectory.path
            }

            staticPrefix.startsWith(PATH_SEPARATOR) -> staticPrefix
            else -> baseDirectory.path + PATH_SEPARATOR + staticPrefix
        }.replaceRegex(RegexPattern.of(PATH_SEPARATOR).oneOrMore()) { PATH_SEPARATOR.toString() }

        val remainingPattern =
            normalizedPattern.removePrefix(staticPrefix).removePrefix(PATH_SEPARATOR)

        if (!staticPrefix.startsWith(PATH_SEPARATOR) &&
            !absolutePath.startsWith(canonicalBase + PATH_SEPARATOR) &&
            absolutePath != canonicalBase
        ) {
            return false
        }
        val relativePath = when {
            absolutePath == canonicalBase -> Constants.Char.EMPTY
            absolutePath.startsWith(canonicalBase + PATH_SEPARATOR) -> absolutePath.removePrefix(
                canonicalBase + PATH_SEPARATOR
            )

            else -> absolutePath.removePrefix(canonicalBase).removePrefix(PATH_SEPARATOR.toString())
        }
        val states = remainingPattern.split(PATH_SEPARATOR).filter { it.isNotEmpty() }.map { segment ->
            if (segment == RECURSIVE_WILDCARD) {
                PatternState(RegexAnyChar.zeroOrMore(), true)
            } else {
                val regexPattern = segment.replaceRegex(
                    RegexGroup.ofCaptured(
                        RegexCharacterClass.ofNegated(
                            RegexCharacterRange.LATIN + RegexCharacterRange.DIGITS,
                            Constants.Char.SPACE, WILDCARD
                        )
                    )
                ) { _, (segmentToEscape) ->
                    segmentToEscape?.regexEscaped().orEmpty()
                }.replace(WILDCARD, "[^/]*").toRegexPattern()
                PatternState(regexPattern, false)
            }
        }

        val lastIndex = states.lastIndex
        var current = BitSet(states.size + 1).apply { set(0) }

        relativePath.split(PATH_SEPARATOR).filter { it.isNotEmpty() }.forEach { pathSegment ->
            val next = BitSet(states.size + 1)
            states.forEachIndexed { index, state ->
                if (!current[index]) {
                    return@forEachIndexed
                }

                if (state.matches(pathSegment)) {
                    next.set(index + 1)
                }
                if (state.isOptional) {
                    next.set(index)
                    current.set(index + 1)
                }
            }
            if (next.isEmpty) {
                return false
            }
            current = next
        }

        return current[lastIndex + 1] ||
                lastIndex >= 0 && current[lastIndex] && states[lastIndex].isOptional
    }

    private data class PatternState(
        private val regexPattern: RegexPattern,
        val isOptional: Boolean,
    ) {
        private val regex: Regex by lazy { regexPattern.toRegex() }

        fun matches(input: String): Boolean =
            regex.matches(input)
    }

    companion object Serializer : KSerializer<GlobPattern> by valueSerializer(::GlobPattern) {

        private const val WILDCARD: Char = Constants.Char.ASTERISK
        private const val NEGATED_MASK: Char = Constants.Char.EXCLAMATION_MARK
        private const val PATH_SEPARATOR: Char = Constants.Char.SLASH

        private val RECURSIVE_WILDCARD: String by lazy { WILDCARD.repeat(2) }
    }
}

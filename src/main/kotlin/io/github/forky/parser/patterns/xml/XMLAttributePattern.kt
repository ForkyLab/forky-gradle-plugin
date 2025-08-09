package io.github.forky.parser.patterns.xml

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.combinators.RegexBetween
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.primitives.RegexWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.ForkyPattern
import io.github.forky.parser.patterns.common.ScopesProvider
import io.github.forky.parser.segments.SegmentPlacementType
import org.gradle.kotlin.dsl.provideDelegate

abstract class XMLAttributePattern(
    namespace: String = ForkyConfig.forkyName,
    name: String,
    segmentPlacementType: SegmentPlacementType,
) : ForkyPattern(segmentPlacementType) {

    protected val attributeRegexPattern: RegexPattern by lazy {
        buildRegexPattern {
            append(namespace)
            append(Constants.Char.COLON)
            append(name)
        }
    }

    fun getFullRegexPattern(): RegexPattern =
        buildRegexPattern {
            append(RegexWhitespace.zeroOrMore())
            append(attributeRegexPattern)
            if (this@XMLAttributePattern is ScopesProvider) {
                append(getScopesRegexPattern())
            }
            append(RegexWhitespace.zeroOrMore())
            append(Constants.Char.EQUAL_SIGN)
            append(RegexWhitespace.zeroOrMore())
            append(RegexBetween.of(Constants.Char.DOUBLE_QUOTE))
            append(RegexWhitespace.zeroOrMore())
        }

    open fun getRegexPattern(): RegexPattern = getFullRegexPattern()

    final override fun getRegexPatterns(): List<RegexPattern> =
        listOf(getRegexPattern())
}

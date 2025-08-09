package io.github.forky.parser.patterns.xml.comment

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.primitives.RegexWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.xml.ScopedXMLAttributePattern
import io.github.forky.parser.segments.SegmentPlacementType

object XMLCommentAttributeNamePattern : ScopedXMLAttributePattern(
    attributeName = "comment-attr-name",
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
), Startable {

    override fun getRegexPattern(): RegexPattern =
        buildRegexPattern {
            append(RegexWhitespace.zeroOrMore())
            append(attributeRegexPattern)
            append(getScopesRegexPattern())
            append(RegexWhitespace.zeroOrMore())
            append(Constants.Char.EQUAL_SIGN)
            append(RegexWhitespace.zeroOrMore())
            append(Constants.Char.DOUBLE_QUOTE)
        }

    override fun isEndedBy(endable: Endable) = endable is XMLCommentAttributeValuePattern
}

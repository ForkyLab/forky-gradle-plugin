package io.github.forky.parser.patterns.xml.comment

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.primitives.RegexWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.xml.ScopedXMLAttributePattern
import io.github.forky.parser.segments.SegmentPlacementType

object XMLCommentAttributeValuePattern : ScopedXMLAttributePattern(
    attributeName = "comment-attr-value",
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
), Endable {

    override fun getRegexPattern(): RegexPattern =
        buildRegexPattern {
            append(Constants.Char.DOUBLE_QUOTE)
            append(RegexWhitespace.zeroOrMore())
            append(attributeRegexPattern)
            append(getScopesRegexPattern())
            append(RegexWhitespace.zeroOrMore())
        }

    override fun isStartedBy(startable: Startable) = startable is XMLCommentAttributeNamePattern
}

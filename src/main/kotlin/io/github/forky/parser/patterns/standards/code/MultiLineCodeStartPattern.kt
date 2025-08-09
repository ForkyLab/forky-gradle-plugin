package io.github.forky.parser.patterns.standards.code

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.combinators.RegexLine
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.primitives.RegexWhitespace
import io.github.forky.comments.CommentStyle
import io.github.forky.comments.CommentStyles
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.standards.ScopedStandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object MultiLineCodeStartPattern : ScopedStandardPattern<CommentStyle>(
    segmentPlacementType = SegmentPlacementType.UPSTREAM_BEFORE,
    commentText = "${ForkyConfig.forkyName} code start",
    commentStyles = CommentStyles.allStyles,
), Startable {

    override fun getCommentRegexPattern(style: CommentStyle, text: String): RegexPattern =
        RegexLine.of(
            buildRegexPattern {
                append(RegexWhitespace.zeroOrMore())
                append(style.commentText(text))
            }
        )

    override fun isEndedBy(endable: Endable) = endable is MultiLineCodeEndPattern
}

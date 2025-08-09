package io.github.forky.parser.patterns.standards.comment

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.combinators.RegexLine
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.primitives.RegexWhitespace
import io.github.forky.comments.CommentStyles
import io.github.forky.comments.MultiLineCommentStyle
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.standards.ScopedStandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object MultiLineCommentStartPattern : ScopedStandardPattern<MultiLineCommentStyle>(
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
    commentText = "${ForkyConfig.forkyName} comment start",
    commentStyles = CommentStyles.multiLineStyles,
), Startable {

    override fun getCommentRegexPattern(style: MultiLineCommentStyle, text: String): RegexPattern =
        RegexLine.of(
            buildRegexPattern {
                append(RegexWhitespace.zeroOrMore())
                append(style.getCommentPrefix(text))
            }
        )

    override fun isEndedBy(endable: Endable) = endable is MultiLineCommentEndPattern
}

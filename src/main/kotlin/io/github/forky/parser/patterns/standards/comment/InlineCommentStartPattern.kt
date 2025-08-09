package io.github.forky.parser.patterns.standards.comment

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.patterns.RegexLookahead
import io.github.diskria.dsl.regex.primitives.RegexNonWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.comments.CommentStyles
import io.github.forky.comments.MultiLineCommentStyle
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.standards.ScopedStandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object InlineCommentStartPattern : ScopedStandardPattern<MultiLineCommentStyle>(
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
    commentText = "${ForkyConfig.forkyName} comment start",
    commentStyles = CommentStyles.multiLineStyles,
), Startable {

    override fun getCommentRegexPattern(style: MultiLineCommentStyle, text: String): RegexPattern =
        buildRegexPattern {
            append(style.getCommentPrefix(text))
            append(Constants.Char.SPACE)
            append(RegexLookahead.of(RegexNonWhitespace))
        }

    override fun isEndedBy(endable: Endable) = endable is InlineCommentEndPattern
}

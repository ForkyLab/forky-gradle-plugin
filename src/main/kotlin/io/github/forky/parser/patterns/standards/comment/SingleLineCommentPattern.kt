package io.github.forky.parser.patterns.standards.comment

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.patterns.RegexLookahead
import io.github.diskria.dsl.regex.primitives.RegexAnyChar
import io.github.diskria.dsl.regex.primitives.RegexNonWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.comments.CommentStyles
import io.github.forky.comments.SingleLineCommentStyle
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.standards.ScopedStandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object SingleLineCommentPattern : ScopedStandardPattern<SingleLineCommentStyle>(
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
    commentText = "${ForkyConfig.forkyName} comment line",
    commentStyles = CommentStyles.singleLineStyles,
) {
    override fun getCommentRegexPattern(style: SingleLineCommentStyle, text: String): RegexPattern =
        buildRegexPattern {
            append(style.commentText(text))
            append(Constants.Char.SPACE)
            append(
                RegexLookahead.of(
                    buildRegexPattern {
                        append(RegexNonWhitespace)
                        append(RegexAnyChar.zeroOrMore())
                    }
                )
            )
        }
}

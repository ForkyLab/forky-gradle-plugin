package io.github.forky.parser.patterns.standards.code

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.primitives.RegexAnyChar
import io.github.diskria.dsl.regex.primitives.RegexNonWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.comments.CommentStyle
import io.github.forky.comments.CommentStyles
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.standards.ScopedStandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object SingleLineCodePattern : ScopedStandardPattern<CommentStyle>(
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
    commentText = "${ForkyConfig.forkyName} code line",
    commentStyles = CommentStyles.allStyles,
) {
    override fun getCommentRegexPattern(style: CommentStyle, text: String): RegexPattern =
        buildRegexPattern {
            append(RegexAnyChar.zeroOrMore())
            append(RegexNonWhitespace)
            append(Constants.Char.SPACE)
            append(style.commentText(text))
        }
}

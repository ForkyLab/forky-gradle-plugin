package io.github.forky.parser.patterns.standards.code

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.diskria.dsl.regex.patterns.RegexLookbehind
import io.github.diskria.dsl.regex.primitives.RegexNonWhitespace
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.comments.CommentStyles
import io.github.forky.comments.MultiLineCommentStyle
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.standards.StandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object InlineCodeEndPattern : StandardPattern<MultiLineCommentStyle>(
    segmentPlacementType = SegmentPlacementType.UPSTREAM_AFTER,
    commentText = "${ForkyConfig.forkyName} code end",
    commentStyles = CommentStyles.multiLineStyles,
), Endable {

    override fun getCommentRegexPattern(style: MultiLineCommentStyle, text: String): RegexPattern =
        buildRegexPattern {
            append(
                RegexLookbehind.of(
                    buildRegexPattern {
                        append(RegexNonWhitespace)
                        append(Constants.Char.SPACE)
                    }
                )
            )
            append(style.commentText(text))
        }

    override fun isStartedBy(startable: Startable) = startable is InlineCodeStartPattern
}

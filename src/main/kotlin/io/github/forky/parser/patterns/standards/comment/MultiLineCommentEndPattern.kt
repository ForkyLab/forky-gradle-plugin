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
import io.github.forky.parser.patterns.standards.StandardPattern
import io.github.forky.parser.segments.SegmentPlacementType

object MultiLineCommentEndPattern : StandardPattern<MultiLineCommentStyle>(
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
    commentText = "${ForkyConfig.forkyName} comment end",
    commentStyles = CommentStyles.multiLineStyles,
), Endable {

    override fun getCommentRegexPattern(style: MultiLineCommentStyle, text: String): RegexPattern =
        RegexLine.of(
            buildRegexPattern {
                append(RegexWhitespace.zeroOrMore())
                append(style.getCommentSuffix(text))
            }
        )

    override fun isStartedBy(startable: Startable) = startable is MultiLineCommentStartPattern
}

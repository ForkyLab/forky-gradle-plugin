package io.github.forky.parser.patterns.standards

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.buildRegexPattern
import io.github.forky.comments.CommentStyle
import io.github.forky.parser.patterns.ForkyPattern
import io.github.forky.parser.patterns.common.ScopesProvider
import io.github.forky.parser.segments.SegmentPlacementType

abstract class StandardPattern<T : CommentStyle>(
    segmentPlacementType: SegmentPlacementType,
    private val commentText: String,
    private val commentStyles: List<T>,
) : ForkyPattern(segmentPlacementType) {

    private val commentBodyRegexPattern: RegexPattern by lazy {
        buildRegexPattern {
            append(commentText)
            if (this@StandardPattern is ScopesProvider) {
                append(getScopesRegexPattern())
            }
        }
    }

    abstract fun getCommentRegexPattern(style: T, text: String): RegexPattern

    final override fun getRegexPatterns(): List<RegexPattern> =
        commentStyles.map { style ->
            getCommentRegexPattern(style, commentBodyRegexPattern.toString())
        }
}

package io.github.forky.comments

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.regexEscaped

class SingleLineCommentStyle(val prefix: String) : CommentStyle {

    fun getRegionStartComment(regionName: String): String =
        commentText(
            "region" + Constants.Char.SPACE + regionName,
            shouldInsertSpace = true,
            shouldRegexEscape = false,
        )

    fun getRegionEndComment(regionName: String): String =
        commentText(
            "endregion" + Constants.Char.SPACE + regionName,
            shouldInsertSpace = true,
            shouldRegexEscape = false,
        )

    private val prefixPattern: String =
        prefix.regexEscaped()

    override fun commentText(
        text: String,
        shouldInsertSpace: Boolean,
        shouldRegexEscape: Boolean,
    ): String =
        buildString {
            append(
                if (shouldRegexEscape) prefixPattern
                else prefix
            )
            if (shouldInsertSpace) {
                append(Constants.Char.SPACE)
            }
            append(text)
        }
}

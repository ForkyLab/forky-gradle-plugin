package io.github.forky.comments

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.regexEscaped

class MultiLineCommentStyle(prefixToSuffix: Pair<String, String>) : CommentStyle {

    private val prefix: String = prefixToSuffix.first
    private val suffix: String = prefixToSuffix.second

    private val prefixPattern: String = prefix.regexEscaped()
    private val suffixPattern: String = suffix.regexEscaped()

    fun getCommentPrefix(
        text: String,
        shouldInsertSpace: Boolean = true,
        shouldRegexEscape: Boolean = true,
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

    fun getCommentSuffix(
        text: String,
        shouldInsertSpace: Boolean = true,
        shouldRegexEscape: Boolean = true,
    ): String =
        buildString {
            append(text)
            if (shouldInsertSpace) {
                append(Constants.Char.SPACE)
            }
            append(
                if (shouldRegexEscape) suffixPattern
                else suffix
            )
        }

    override fun commentText(
        text: String,
        shouldInsertSpace: Boolean,
        shouldRegexEscape: Boolean,
    ): String =
        buildString {
            append(getCommentPrefix(text, shouldInsertSpace, shouldRegexEscape))
            append(getCommentSuffix(Constants.Char.EMPTY, shouldInsertSpace, shouldRegexEscape))
        }
}

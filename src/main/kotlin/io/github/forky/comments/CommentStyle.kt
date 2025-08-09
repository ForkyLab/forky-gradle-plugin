package io.github.forky.comments

interface CommentStyle {
    fun commentText(
        text: String,
        shouldInsertSpace: Boolean = true,
        shouldRegexEscape: Boolean = true,
    ): String
}

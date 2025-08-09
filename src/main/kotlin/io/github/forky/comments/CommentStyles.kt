package io.github.forky.comments

object CommentStyles {

    val DOUBLE_SLASH_COMMENT_STYLE = SingleLineCommentStyle("//")

    val singleLineStyles: List<SingleLineCommentStyle> =
        listOf(
            SingleLineCommentStyle("#"),
            DOUBLE_SLASH_COMMENT_STYLE,
        )

    val multiLineStyles: List<MultiLineCommentStyle> =
        listOf(
            MultiLineCommentStyle("/*" to "*/"),
            MultiLineCommentStyle("<!--" to "-->"),
        )

    val allStyles: List<CommentStyle> =
        singleLineStyles + multiLineStyles
}

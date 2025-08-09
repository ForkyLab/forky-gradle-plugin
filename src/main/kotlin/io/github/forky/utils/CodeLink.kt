package io.github.forky.utils

import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.common.packIntsToLong
import io.github.diskria.utils.kotlin.extensions.primitives.unpackHighInt
import io.github.diskria.utils.kotlin.extensions.primitives.unpackLowInt
import java.io.File

@JvmInline
value class CodeLink(private val packedIndexes: Long) {

    val lineIndex: Int get() = packedIndexes.unpackHighInt()
    val charIndex: Int get() = packedIndexes.unpackLowInt()

    val nextLineIndex: Int get() = lineIndex.inc()
    val nextCharIndex: Int get() = charIndex.inc()

    fun move(nextChar: Char): CodeLink =
        if (nextChar == Constants.Char.NEW_LINE) of(nextLineIndex)
        else of(lineIndex, nextCharIndex)

    fun toLink(file: File): String =
        listOf(
            file.absolutePath,
            nextLineIndex.toString(),
            nextCharIndex.toString()
        ).joinToString(Constants.Char.COLON.toString())

    companion object {
        val INITIAL: CodeLink = CodeLink(0.toLong())

        fun of(lineIndex: Int, charIndex: Int = 0): CodeLink =
            CodeLink(packIntsToLong(lineIndex, charIndex))

        fun of(file: File, lineIndex: Int, charIndex: Int = 0): String =
            CodeLink(packIntsToLong(lineIndex, charIndex)).toLink(file)
    }
}

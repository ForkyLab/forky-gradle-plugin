package io.github.forky.parser

import io.github.forky.parser.patterns.ForkyPattern
import io.github.forky.parser.patterns.standards.StandardPattern
import io.github.forky.parser.patterns.standards.code.*
import io.github.forky.parser.patterns.standards.comment.*
import io.github.forky.parser.patterns.xml.XMLAttributePattern
import io.github.forky.parser.patterns.xml.code.XMLAttributesEndPattern
import io.github.forky.parser.patterns.xml.code.XMLAttributesStartPattern
import io.github.forky.parser.patterns.xml.code.XMLNamespacePattern
import io.github.forky.parser.patterns.xml.comment.XMLCommentAttributeNamePattern
import io.github.forky.parser.patterns.xml.comment.XMLCommentAttributeValuePattern
import io.github.forky.parser.segments.Segment

object ForkyPatterns {

    val standardPatterns: List<StandardPattern<*>> =
        listOf(
            SingleLineCodePattern,
            SingleLineCommentPattern,

            InlineCodeStartPattern, InlineCodeEndPattern,
            InlineCommentStartPattern, InlineCommentEndPattern,

            MultiLineCodeStartPattern, MultiLineCodeEndPattern,
            MultiLineCommentStartPattern, MultiLineCommentEndPattern,
        )

    val xmlPatterns: List<XMLAttributePattern> =
        listOf(
            XMLNamespacePattern,

            XMLAttributesStartPattern, XMLAttributesEndPattern,

            XMLCommentAttributeNamePattern, XMLCommentAttributeValuePattern,
        )

    private val allPatterns: List<ForkyPattern> =
        standardPatterns + xmlPatterns

    fun findSegments(codeLine: String, lineIndex: Int): List<Segment> =
        allPatterns
            .flatMap { pattern -> pattern.findSegments(codeLine, lineIndex) }
            .sortedBy { segment -> segment.charIndex }
}

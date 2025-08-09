package io.github.forky.parser.patterns

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.extensions.component1
import io.github.diskria.dsl.regex.extensions.component2
import io.github.forky.parser.patterns.common.ScopesProvider
import io.github.forky.parser.segments.Segment
import io.github.forky.parser.segments.SegmentPlacementType

abstract class ForkyPattern(private val segmentPlacementType: SegmentPlacementType) {

    private val regexes: List<Regex> by lazy {
        getRegexPatterns().map { pattern -> pattern.toRegex() }
    }

    protected abstract fun getRegexPatterns(): List<RegexPattern>

    fun findSegments(codeLine: String, lineIndex: Int): List<Segment> =
        regexes.flatMap { regex ->
            regex.findAll(codeLine).mapNotNull { (segmentGroup, scopesGroup) ->
                if (segmentGroup == null) {
                    return@mapNotNull null
                }

                val firstCharIndex = segmentGroup.range.first
                val lastCharIndex = segmentGroup.range.last.inc().coerceIn(0, codeLine.length)

                val scopes = (this as? ScopesProvider)?.parseScopes(scopesGroup).orEmpty()

                when (segmentPlacementType) {
                    SegmentPlacementType.UPSTREAM_BEFORE -> {
                        listOf(Segment(this, true, lineIndex, firstCharIndex, scopes))
                    }

                    SegmentPlacementType.UPSTREAM_AFTER -> {
                        listOf(Segment(this, false, lineIndex, lastCharIndex, scopes))
                    }

                    SegmentPlacementType.EMBEDDED -> {
                        listOf(
                            Segment(this, true, lineIndex, firstCharIndex, scopes),
                            Segment(this, false, lineIndex, lastCharIndex, scopes)
                        )
                    }
                }
            }.flatten()
        }
}

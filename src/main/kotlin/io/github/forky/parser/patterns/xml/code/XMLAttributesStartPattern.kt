package io.github.forky.parser.patterns.xml.code

import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.xml.ScopedXMLAttributePattern
import io.github.forky.parser.segments.SegmentPlacementType

object XMLAttributesStartPattern : ScopedXMLAttributePattern(
    attributeName = "start-attrs",
    segmentPlacementType = SegmentPlacementType.UPSTREAM_BEFORE,
), Startable {
    override fun isEndedBy(endable: Endable) = endable is XMLAttributesEndPattern
}

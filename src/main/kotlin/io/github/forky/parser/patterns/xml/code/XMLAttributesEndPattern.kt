package io.github.forky.parser.patterns.xml.code

import io.github.forky.parser.patterns.common.Endable
import io.github.forky.parser.patterns.common.Startable
import io.github.forky.parser.patterns.xml.ScopedXMLAttributePattern
import io.github.forky.parser.segments.SegmentPlacementType

object XMLAttributesEndPattern : ScopedXMLAttributePattern(
    attributeName = "end-attrs",
    segmentPlacementType = SegmentPlacementType.UPSTREAM_AFTER,
), Endable {
    override fun isStartedBy(startable: Startable) = startable is XMLAttributesStartPattern
}

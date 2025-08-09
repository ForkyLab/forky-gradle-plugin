package io.github.forky.parser.patterns.xml.code

import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.xml.XMLAttributePattern
import io.github.forky.parser.segments.SegmentPlacementType

object XMLNamespacePattern : XMLAttributePattern(
    namespace = "xmlns",
    name = ForkyConfig.forkyName,
    segmentPlacementType = SegmentPlacementType.EMBEDDED,
)

package io.github.forky.parser.patterns.xml

import io.github.diskria.utils.kotlin.BracketsType
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.config.ForkyConfig
import io.github.forky.parser.patterns.common.ScopesProvider
import io.github.forky.parser.segments.SegmentPlacementType

abstract class ScopedXMLAttributePattern(
    namespace: String = ForkyConfig.forkyName,
    attributeName: String,
    segmentPlacementType: SegmentPlacementType,
) : XMLAttributePattern(
    namespace = namespace,
    name = attributeName,
    segmentPlacementType = segmentPlacementType,
), ScopesProvider {
    override val scopesPrefix: Char = Constants.Char.UNDERSCORE
    override val scopesSeparator: String by lazy { scopesPrefix.toString() }
    override val scopesBracketsType: BracketsType? = null
}

package io.github.forky.parser.patterns.standards

import io.github.diskria.utils.kotlin.BracketsType
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.comments.CommentStyle
import io.github.forky.parser.patterns.common.ScopesProvider
import io.github.forky.parser.segments.SegmentPlacementType

abstract class ScopedStandardPattern<T : CommentStyle>(
    segmentPlacementType: SegmentPlacementType,
    commentText: String,
    commentStyles: List<T>,
) : StandardPattern<T>(
    segmentPlacementType = segmentPlacementType,
    commentText = commentText,
    commentStyles = commentStyles
), ScopesProvider {
    override val scopesPrefix: Char = Constants.Char.SPACE
    override val scopesSeparator: String by lazy { Constants.Char.COMMA.toString() + scopesPrefix }
    override val scopesBracketsType: BracketsType? = BracketsType.SQUARE
}

package io.github.forky.parser.segments

/**
 * Describes the logical placement of a fork segment relative to upstream code.
 */
enum class SegmentPlacementType {

    /**
     * Upstream code appears before the segment.
     * The segment itself and the code after it belong to the fork.
     *
     * ```
     * [Upstream] [Segment -> Fork]
     * ```
     */
    UPSTREAM_BEFORE,

    /**
     * Fork code appears before and includes the segment.
     * Code after the segment belongs to upstream.
     *
     * ```
     * [Fork <- Segment] [Upstream]
     * ```
     */
    UPSTREAM_AFTER,

    /**
     * Upstream code appears both before and after the segment.
     * The segment itself is isolated fork code.
     *
     * ```
     * [Upstream] [Fork] [Upstream]
     * ```
     */
    EMBEDDED;
}

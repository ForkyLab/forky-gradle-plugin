package io.github.forky.parser.segments

import io.github.forky.parser.patterns.ForkyPattern
import io.github.forky.parser.scopes.Scope

class Segment(
    val pattern: ForkyPattern,
    val isStart: Boolean,
    val lineIndex: Int,
    val charIndex: Int,
    val scopes: Set<Scope>,
)

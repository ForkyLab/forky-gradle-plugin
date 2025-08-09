package io.github.forky.parser.patterns.common

import io.github.forky.parser.patterns.ForkyPattern

interface Startable {
    fun isEndedBy(endable: Endable): Boolean
    fun isNestedAllowed(pattern: ForkyPattern): Boolean = false
}

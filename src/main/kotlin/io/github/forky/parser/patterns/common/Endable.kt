package io.github.forky.parser.patterns.common

interface Endable {
    fun isStartedBy(startable: Startable): Boolean
}

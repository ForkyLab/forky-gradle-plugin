package io.github.forky.tasks

import io.github.forky.ForkyPlugin
import io.github.forky.config.ForkyConfig
import io.github.forky.config.ForkyToml
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

sealed class ForkyTask(@Internal val taskDescription: String) : DefaultTask() {

    @get:Internal
    protected val config: ForkyToml get() = ForkyConfig.config

    @get:Internal
    protected val forkyName: String get() = ForkyConfig.forkyName

    final override fun getDescription(): String = taskDescription

    final override fun getGroup(): String = ForkyPlugin.FORKY_NAME

    protected abstract fun runTask()

    @TaskAction
    fun action() {
        runTask()
    }
}

package io.github.forky.config

import com.akuleshov7.ktoml.Toml
import io.github.diskria.dsl.shell.GitShell
import io.github.diskria.utils.kotlin.extensions.asFileOrThrow
import io.github.diskria.utils.kotlin.extensions.common.initializeFirst
import io.github.diskria.utils.kotlin.extensions.toFile
import io.github.forky.errors.*
import io.github.forky.tasks.ForkyCheckTask
import org.gradle.api.Project
import java.io.File

object ForkyConfig {

    val config: ForkyToml by lazy { forkyTomlInstance ?: initializeFirst() }
    val projectRoot: File by lazy { projectRootInstance ?: initializeFirst() }

    val upstreamPath: String by lazy { config.upstreamPath }
    val forkPath: String by lazy { config.forkPath }

    val upstreamRoot: File by lazy { projectRoot.resolve(upstreamPath).normalize() }
    val forkRoot: File by lazy { projectRoot.resolve(forkPath).normalize() }

    val forkyName: String by lazy { config.forkyName }
    val scopeIds: List<String> by lazy { config.scopes.map { it.id } }

    val forkGitShell: GitShell by lazy { GitShell.open(projectRoot) }
    val upstreamGitShell: GitShell by lazy { GitShell.open(upstreamRoot) }

    private var projectRootInstance: File? = null
    private var forkyTomlInstance: ForkyToml? = null

    fun init(project: Project) {
        if (projectRootInstance == null) {
            projectRootInstance = project.rootDir
        }
        if (forkyTomlInstance == null) {
            loadConfig()
        }
    }

    fun reload() {
        forkyTomlInstance = null
        loadConfig()
    }

    private fun loadConfig() {
        forkyTomlInstance = Toml.decodeFromString(
            ForkyToml.serializer(),
            projectRoot.resolve(ForkyCheckTask.getConfigFileName()).asFileOrThrow().readText()
        )
        validateConfig()
    }

    private fun validateConfig() {
        when {
            config.upstreamPath.toFile().isAbsolute -> {
                error(InvalidUpstreamPathError(config.upstreamPath))
            }

            !config.isCloneAllowed && !upstreamRoot.isDirectory -> {
                error(UpstreamMissingError)
            }

            config.isCloneAllowed && upstreamGitShell.isInvalidRepository() -> {
                error(InvalidUpstreamGitRepoError)
            }

            forkPath.toFile().isAbsolute || !forkRoot.isDirectory -> {
                error(InvalidForkPathError(forkPath))
            }

            !forkyName.all { it.isLetterOrDigit() } -> {
                error(InvalidForkyNameError(forkyName))
            }
        }
    }
}

package io.github.forky.config

import io.github.diskria.dsl.shell.GitShell
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.ForkyPlugin
import io.github.forky.config.models.Scope
import io.github.forky.tasks.ForkyCheckTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForkyToml(

    @SerialName(UPSTREAM_PATH)
    val upstreamPath: String,

    @SerialName("upstream-remote-name")
    val upstreamRemoteName: String = GitShell.UPSTREAM_REMOTE_NAME,

    @SerialName("upstream-remote-url")
    val upstreamRemoteUrl: String,

    @SerialName("upstream-commit")
    val upstreamCommit: String,

    @SerialName(ALLOW_CLONE)
    val isCloneAllowed: Boolean = false,

    @SerialName(FORK_PATH)
    val forkPath: String = Constants.File.CURRENT_DIRECTORY,

    @SerialName(FORKY_NAME)
    val forkyName: String = ForkyPlugin.FORKY_NAME,

    @SerialName(SEGMENT_MARKER)
    val segmentMarker: Char = ForkyCheckTask.DEFAULT_SEGMENT_MARKER,

    @SerialName("max-workers")
    val maxWorkers: Int = 0,

    @SerialName(BINARY_CHECKSUM_ALGORITHM)
    val binaryChecksumAlgorithm: String = "SHA-256",

    @SerialName("skip-code")
    val isCodeChecksSkipped: Boolean = false,

    @SerialName("debug-binary-extensions")
    val isBinaryFileExtensionsDebuggingEnabled: Boolean = false,

    @SerialName(TEXT_FILE_MIME_SUBTYPES)
    val textFileMimeSubtypes: Set<String> = setOf(
        "json",
        "ld+json",
        "xml",
        "xhtml+xml",
        "svg+xml",
        "x-sh",
        "x-csh",
        "x-plist",
        "x-pem-file",
        "x-httpd-php",
        "x-properties",
        "x-msdos-program",
    ),

    @SerialName(TEXT_FILE_EXTENSIONS)
    val textFileExtensions: Set<String> = setOf(
        "svg",
        "bat",
        "crt",
        "pem",
        "plist",
    ),

    @SerialName("scopes")
    val scopes: List<Scope> = emptyList(),
) {
    companion object {
        const val UPSTREAM_PATH = "upstream-path"
        const val ALLOW_CLONE = "allow-clone"

        const val FORKY_NAME = "forky-name"
        const val FORK_PATH = "fork-path"
        const val SEGMENT_MARKER = "segment-marker"

        const val BINARY_CHECKSUM_ALGORITHM = "binary-checksum-algorithm"
        const val TEXT_FILE_MIME_SUBTYPES = "text-file-mime-subtypes"
        const val TEXT_FILE_EXTENSIONS = "text-file-extensions"
    }
}

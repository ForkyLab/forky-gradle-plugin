package io.github.forky.config.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Scope(

    @SerialName("id")
    val id: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("url")
    val url: String? = null,

    @SerialName("ignored")
    val ignoredPatterns: List<GlobPattern> = emptyList(),

    @SerialName("deleted")
    val deletedPatterns: List<GlobPattern> = emptyList(),

    @SerialName("moved")
    val movedFiles: List<MovedFile> = emptyList(),
)

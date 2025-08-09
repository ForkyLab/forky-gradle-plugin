package io.github.forky.config.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovedFile(
    @SerialName("from") val sourcePath: String,
    @SerialName("to") val destinationPath: String,
)

package io.github.forky.api

import java.io.File

fun interface TemplateCodegen {
    fun getParameterValue(file: File, templateId: String, parameterName: String): String
}

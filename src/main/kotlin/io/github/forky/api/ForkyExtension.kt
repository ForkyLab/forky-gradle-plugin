package io.github.forky.api

import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject

abstract class ForkyExtension @Inject constructor() {

    abstract val templateCodegen: Property<TemplateCodegen>

    fun templateCodegen(action: (file: File, templateId: String, key: String) -> String) {
        templateCodegen.set(TemplateCodegen(action))
    }
}

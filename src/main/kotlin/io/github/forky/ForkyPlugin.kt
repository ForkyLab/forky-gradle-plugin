package io.github.forky

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import io.github.diskria.utils.gradle.extensions.getExtension
import io.github.diskria.utils.gradle.extensions.ifAndroid
import io.github.diskria.utils.kotlin.delegates.toAutoNamedPair
import io.github.diskria.utils.kotlin.extensions.common.failWithDetails
import io.github.forky.api.ForkyExtension
import io.github.forky.api.TemplateCodegen
import io.github.forky.config.ForkyConfig
import io.github.forky.tasks.ForkyCheckTask
import io.github.forky.tasks.GenerateTemplatesTask
import io.github.forky.tasks.StripForkyXMLAttributesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class ForkyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        ForkyConfig.init(project)
        val extension = project.extensions.create<ForkyExtension>(FORKY_NAME)

        project.tasks.register<GenerateTemplatesTask>("generateTemplates") {
            templateCodegen.set(extension.templateCodegen)
            templateCodegen.convention(TemplateCodegen { file, templateId, parameterName ->
                val file by file.toAutoNamedPair()
                val templateId by templateId.toAutoNamedPair()
                val parameterName by parameterName.toAutoNamedPair()
                failWithDetails(
                    "templateCodegen is not configured but a parameterized template was found",
                    file,
                    templateId,
                    parameterName
                )
            })
        }
        project.tasks.register<ForkyCheckTask>("forkyCheck")
        project.ifAndroid {
            registerStripAndroidTasks(project)
        }
    }

    private fun registerStripAndroidTasks(project: Project) {
        val componentsExtension = project.getExtension(AndroidComponentsExtension::class.java)
            ?: error("AndroidComponentsExtension not found")
        componentsExtension.onVariants(componentsExtension.selector().all()) { variant ->
            val stripManifestTask = project.tasks.register(
                "stripForky${variant.name.capitalized()}Manifest",
                StripForkyXMLAttributesTask::class.java
            )
            variant.artifacts.use(stripManifestTask).wiredWithFiles(
                StripForkyXMLAttributesTask::inputFile,
                StripForkyXMLAttributesTask::outputFile
            ).toTransform(SingleArtifact.MERGED_MANIFEST)
        }
    }

    companion object {
        const val FORKY_NAME = "forky"
    }
}

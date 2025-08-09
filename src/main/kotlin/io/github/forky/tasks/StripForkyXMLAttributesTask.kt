package io.github.forky.tasks

import io.github.diskria.dsl.regex.extensions.replaceRegex
import io.github.diskria.utils.kotlin.Constants
import io.github.diskria.utils.kotlin.extensions.generics.foldChain
import io.github.forky.parser.ForkyPatterns
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class StripForkyXMLAttributesTask : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun runTask() {
        val inputXmlCode = inputFile.get().asFile.readText()
        val outputXmlCode = ForkyPatterns.xmlPatterns.foldChain(inputXmlCode) { pattern ->
            replaceRegex(pattern.getFullRegexPattern()) { Constants.Char.SPACE.toString() }
        }
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(outputXmlCode)
        }
    }
}

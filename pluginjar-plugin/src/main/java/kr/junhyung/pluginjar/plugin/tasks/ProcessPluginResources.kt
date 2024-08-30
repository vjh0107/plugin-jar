package kr.junhyung.pluginjar.plugin.tasks

import kr.junhyung.pluginjar.plugin.service.PluginMetaService
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import javax.inject.Inject

open class ProcessPluginResources(
    project: Project,
    private val pluginMetaService: PluginMetaService
) : DefaultTask() {

    @Suppress("unused")
    @Inject
    constructor(project: Project) : this(project, project.extensions.getByType<PluginMetaService>())

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun generatePluginMetaYaml() {
        pluginMetaService.generatePluginMetaFile(outputDirectory.file("plugin.yml").get().asFile)
    }
}
package kr.junhyung.pluginjar.plugin

import kr.junhyung.pluginjar.core.service.PluginMetaService
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
open class ProcessPluginResources(
    project: Project,
    private val pluginMetaService: PluginMetaService
) : DefaultTask() {

    @Suppress("unused")
    @Inject
    constructor(project: Project) : this(project, project.extensions.getByType<PluginMetaService>())

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    override fun getOutputs(): TaskOutputsInternal {
        return super.getOutputs().apply {
            upToDateWhen { false }
        }
    }

    @TaskAction
    fun generatePluginMetaYaml() {
        pluginMetaService.generatePluginMetaFile(outputDirectory.file("plugin.yml").get().asFile)
    }
}
package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.extensions.PaperPluginExtension
import kr.junhyung.pluginjar.gradle.tasks.GeneratePaperPluginYml
import kr.junhyung.pluginjar.gradle.tasks.ResolvePluginMarker
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class PaperPluginJarSupport : AbstractPlatformSupport(
    artifactName = "plugin-jar-paper",
    dependencyConfiguration = "runtimeOnly"
) {

    companion object {
        const val EXTENSION_NAME = "paperPlugin"
        const val TASK_GENERATE = "generatePaperPluginYml"
    }

    override fun registerExtension(project: Project) {
        project.extensions.create<PaperPluginExtension>(EXTENSION_NAME)
    }

    override fun isApplicable(project: Project): Boolean =
        project.configurations.getByName("compileClasspath")
            .resolvedConfiguration
            .resolvedArtifacts
            .any { it.moduleVersion.id.group == "io.papermc.paper" && it.moduleVersion.id.name == "paper-api" }

    override fun configureExtension(
        project: Project,
        pluginMeta: PluginMeta?,
        resolvePluginMarker: TaskProvider<ResolvePluginMarker>
    ) {
        val extension = project.extensions.getByType<PaperPluginExtension>()
        extension.apply {
            if (!main.isPresent) {
                main.convention(project.provider {
                    pluginMeta?.mainClass ?: resolvePluginMarker.get().readPluginMeta().mainClass
                })
            }
            if (!name.isPresent) {
                name.convention(project.provider { pluginMeta?.name ?: project.name })
            }
            if (!version.isPresent) {
                version.convention(project.version.toString())
            }
            if (!apiVersion.isPresent) {
                apiVersion.convention(project.provider { extractApiVersion(project) })
            }
            if (!description.isPresent && project.description != null) {
                description.convention(project.description)
            }
            if (!loader.isPresent) {
                loader.convention("kr.junhyung.pluginjar.paper.PluginJarPluginLoader")
            }
        }
    }

    override fun registerTasks(
        project: Project,
        resolvePluginMarker: TaskProvider<ResolvePluginMarker>
    ) {
        val extension = project.extensions.getByType<PaperPluginExtension>()

        val generatePaperYml = project.tasks.register<GeneratePaperPluginYml>(TASK_GENERATE) {
            outputFile.convention(project.layout.buildDirectory.file("pluginjar/paper-plugin.yml"))
            this.extension.set(extension)
            dependsOn(resolvePluginMarker)
        }

        project.tasks.named<ProcessResources>("processResources") {
            from(generatePaperYml.map { it.outputFile })
            dependsOn(generatePaperYml)
        }
    }

    private fun extractApiVersion(project: Project): String {
        val paperApiDependency = project.configurations
            .getByName("compileClasspath")
            .resolvedConfiguration
            .resolvedArtifacts
            .find { it.moduleVersion.id.group == "io.papermc.paper" && it.moduleVersion.id.name == "paper-api" }

        if (paperApiDependency != null) {
            val version = paperApiDependency.moduleVersion.id.version
            val match = Regex("^(\\d+\\.\\d+(?:\\.\\d+)?)").find(version)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return "1.21"
    }
}

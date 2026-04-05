package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.extensions.VelocityPluginExtension
import kr.junhyung.pluginjar.gradle.tasks.GenerateVelocityPluginJson
import kr.junhyung.pluginjar.gradle.tasks.ResolvePluginMarker
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class VelocityPluginJarSupport : AbstractPlatformSupport(
    artifactName = "plugin-jar-velocity",
    dependencyConfiguration = "implementation"
) {

    companion object {
        const val EXTENSION_NAME = "velocityPlugin"
        const val TASK_GENERATE = "generateVelocityPluginJson"
    }

    override fun registerExtension(project: Project) {
        project.extensions.create<VelocityPluginExtension>(EXTENSION_NAME)
    }

    override fun isApplicable(project: Project): Boolean =
        project.configurations.getByName("compileClasspath")
            .resolvedConfiguration
            .resolvedArtifacts
            .any { it.moduleVersion.id.group == "com.velocitypowered" && it.moduleVersion.id.name == "velocity-api" }

    override fun configureExtension(
        project: Project,
        pluginMeta: PluginMeta?,
        resolvePluginMarker: TaskProvider<ResolvePluginMarker>
    ) {
        val extension = project.extensions.getByType<VelocityPluginExtension>()
        extension.apply {
            if (!main.isPresent) {
                main.convention(project.provider {
                    pluginMeta?.mainClass ?: resolvePluginMarker.get().readPluginMeta().mainClass
                })
            }
            if (!name.isPresent) {
                name.convention(project.provider { pluginMeta?.name ?: project.name })
            }
            if (!id.isPresent) {
                id.convention(project.name.lowercase().replace(Regex("[^a-z0-9]"), ""))
            }
            if (!version.isPresent) {
                version.convention(project.version.toString())
            }
            if (!description.isPresent && project.description != null) {
                description.convention(project.description)
            }
        }
    }

    override fun registerTasks(
        project: Project,
        resolvePluginMarker: TaskProvider<ResolvePluginMarker>
    ) {
        val extension = project.extensions.getByType<VelocityPluginExtension>()

        val generateVelocityJson = project.tasks.register<GenerateVelocityPluginJson>(TASK_GENERATE) {
            outputFile.convention(project.layout.buildDirectory.file("pluginjar/velocity-plugin.json"))
            this.extension.set(extension)
            dependsOn(resolvePluginMarker)
        }

        project.tasks.named<ProcessResources>("processResources") {
            from(generateVelocityJson.map { it.outputFile })
            dependsOn(generateVelocityJson)
        }
    }
}

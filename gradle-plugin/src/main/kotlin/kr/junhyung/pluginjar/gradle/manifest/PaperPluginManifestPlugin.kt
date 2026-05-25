package kr.junhyung.pluginjar.gradle.manifest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

abstract class PaperPluginManifestPlugin : Plugin<Project> {

    companion object {
        internal const val PAPER_RUNTIME_ARTIFACT = "plugin-jar-paper-plugin-loader"

        val pluginVersion: String by lazy {
            PaperPluginManifestPlugin::class.java.`package`.implementationVersion
                ?: System.getProperty("pluginjar.version")
                ?: error("Could not locate plugin version")
        }

        val pluginGroup: String by lazy {
            PaperPluginManifestPlugin::class.java.`package`.implementationVendor
                ?: System.getProperty("pluginjar.group")
                ?: error("Could not locate plugin group")
        }
    }

    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            val extension = project.extensions.create<PluginExtension>("paperPlugin")
            val resolveMarker = ResolvePluginMarker.register(project)
            configurePluginConventions(project, extension)
            PluginRuntimeClasspath.register(project, project.configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).get())

            val generateYml = GeneratePaperPluginYml.register(project, extension, resolveMarker)
            val mainSourceSet = project.extensions.getByType<SourceSetContainer>()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            mainSourceSet.resources.srcDir(generateYml.map { it.outputFile.get().asFile.parentFile })
        }
    }

    private fun configurePluginConventions(project: Project, extension: PluginExtension) {
        extension.version.convention(project.provider { project.version.toString() })
        extension.description.convention(
            project.provider { project.description.orEmpty() }.filter { it.isNotEmpty() }
        )
    }
}

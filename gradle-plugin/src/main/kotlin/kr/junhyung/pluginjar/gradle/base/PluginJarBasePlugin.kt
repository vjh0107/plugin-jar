package kr.junhyung.pluginjar.gradle.base

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType

abstract class PluginJarBasePlugin : Plugin<Project> {

    companion object {
        private const val ANNOTATIONS_ARTIFACT = "plugin-jar-annotations"
        internal const val PAPER_RUNTIME_ARTIFACT = "plugin-jar-paper-plugin-loader"

        val pluginVersion: String by lazy {
            PluginJarBasePlugin::class.java.`package`.implementationVersion
                ?: System.getProperty("pluginjar.version")
                ?: error("Could not locate plugin version")
        }

        val pluginGroup: String by lazy {
            PluginJarBasePlugin::class.java.`package`.implementationVendor
                ?: System.getProperty("pluginjar.group")
                ?: error("Could not locate plugin group")
        }
    }

    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            val pluginExtension = project.extensions.create<PluginExtension>("paperPlugin")
            wireDependencies(project)
            ResolvePluginMarker.register(project)
            configurePluginConventions(project, pluginExtension)
            PluginRuntimeClasspath.register(project, project.configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).get())
        }
    }

    private fun wireDependencies(project: Project) {
        val factory = project.dependencyFactory
        val dependencyHandler = project.dependencies
        dependencyHandler.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, factory.create(pluginGroup, ANNOTATIONS_ARTIFACT, pluginVersion))
        dependencyHandler.add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, factory.create(pluginGroup, PAPER_RUNTIME_ARTIFACT, pluginVersion))
    }

    private fun configurePluginConventions(project: Project, extension: PluginExtension) {
        extension.version.convention(project.provider { project.version.toString() })
        extension.description.convention(
            project.provider { project.description.orEmpty() }.filter { it.isNotEmpty() }
        )
    }
}

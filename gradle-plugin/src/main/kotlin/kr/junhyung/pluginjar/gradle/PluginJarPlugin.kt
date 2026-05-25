package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.image.PluginImageBuildPlugin
import kr.junhyung.pluginjar.gradle.manifest.PaperPluginManifestPlugin
import kr.junhyung.pluginjar.gradle.nested.NestedPluginJarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.withType

abstract class PluginJarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(PaperPluginManifestPlugin::class.java)
        project.plugins.withType<JavaPlugin> {
            wireDependencies(project)
        }
        project.plugins.apply(NestedPluginJarPlugin::class.java)
        project.plugins.apply(PluginImageBuildPlugin::class.java)
    }

    private fun wireDependencies(project: Project) {
        val factory = project.dependencyFactory
        val dependencies = project.dependencies
        dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, factory.create(PaperPluginManifestPlugin.pluginGroup, ANNOTATIONS_ARTIFACT, PaperPluginManifestPlugin.pluginVersion))
        dependencies.add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, factory.create(PaperPluginManifestPlugin.pluginGroup, PaperPluginManifestPlugin.PAPER_RUNTIME_ARTIFACT, PaperPluginManifestPlugin.pluginVersion))
    }

    companion object {
        private const val ANNOTATIONS_ARTIFACT = "plugin-jar-annotations"
    }
}

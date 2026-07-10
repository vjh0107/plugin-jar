package kr.junhyung.pluginjar.gradle.velocity

import kr.junhyung.pluginjar.annotations.PluginMarker
import kr.junhyung.pluginjar.gradle.PluginJarArtifacts
import kr.junhyung.pluginjar.gradle.PluginJarBasePlugin
import kr.junhyung.pluginjar.gradle.ResolveMainClass
import kr.junhyung.pluginjar.gradle.image.PluginImageBuildPlugin
import kr.junhyung.pluginjar.gradle.nested.NestedPluginJarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

abstract class VelocityPluginJarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(VelocityPluginManifestPlugin::class.java)
        project.plugins.apply(NestedPluginJarPlugin::class.java)
        project.plugins.apply(PluginImageBuildPlugin::class.java)
        project.plugins.withType<JavaPlugin> {
            wireDependencies(project)
            applyMarkerConventions(project)
        }
    }

    private fun applyMarkerConventions(project: Project) {
        val marker = ResolveMainClass.register(project, PluginMarker::class.java.name)
        val meta = marker.flatMap { it.outputFile }
            .map { ResolveMainClass.PluginMeta.readFrom(it.asFile) }
        val extension = project.extensions.getByType<VelocityPluginExtension>()
        extension.main.convention(meta.map { it.mainClass })
        extension.id.convention(meta.map { it.id.orEmpty() }.filter { it.isNotEmpty() })
    }

    private fun wireDependencies(project: Project) {
        val dependencies = project.dependencies
        dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, PluginJarArtifacts.annotations)
        dependencies.add(PluginJarBasePlugin.BOOTSTRAP_CONFIGURATION_NAME, PluginJarArtifacts.velocityLoader)
    }
}

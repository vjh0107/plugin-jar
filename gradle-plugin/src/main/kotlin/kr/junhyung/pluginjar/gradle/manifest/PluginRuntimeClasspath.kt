package kr.junhyung.pluginjar.gradle.manifest

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal class PluginRuntimeClasspath private constructor(configuration: Configuration) {

    val pluginJarLibraries: FileCollection = configuration.incoming.artifactView {
        componentFilter { isPluginJar(it) }
    }.files

    val externalLibraries: FileCollection = configuration.incoming.artifactView {
        componentFilter { !isPluginJar(it) && it !is ProjectComponentIdentifier }
    }.files

    val projectArtifacts: Provider<Set<ResolvedArtifactResult>> = configuration.incoming.artifactView {
        componentFilter { it is ProjectComponentIdentifier }
    }.artifacts.resolvedArtifacts

    private fun isPluginJar(id: ComponentIdentifier): Boolean =
        id is ModuleComponentIdentifier &&
            id.group == PaperPluginManifestPlugin.pluginGroup

    companion object {
        private const val EXTENSION_NAME = "pluginRuntimeClasspath"

        internal fun register(project: Project, configuration: Configuration): PluginRuntimeClasspath {
            val classpath = PluginRuntimeClasspath(configuration)
            project.extensions.add(PluginRuntimeClasspath::class.java, EXTENSION_NAME, classpath)
            return classpath
        }

        internal fun of(project: Project): PluginRuntimeClasspath =
            project.extensions.getByType()
    }
}

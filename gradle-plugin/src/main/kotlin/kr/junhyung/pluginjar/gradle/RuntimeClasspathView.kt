package kr.junhyung.pluginjar.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal class RuntimeClasspathView private constructor(
    runtimeClasspath: Configuration,
    bootstrapClasspath: Configuration,
) {

    val bootstrapLibraries: FileCollection = bootstrapClasspath.incoming.artifactView {
        componentFilter { it !is ProjectComponentIdentifier }
    }.files

    val externalLibraries: FileCollection = runtimeClasspath.incoming.artifactView {
        componentFilter { it !is ProjectComponentIdentifier }
    }.files.minus(bootstrapLibraries)

    val projectArtifacts: Provider<Set<ResolvedArtifactResult>> = runtimeClasspath.incoming.artifactView {
        componentFilter { it is ProjectComponentIdentifier }
    }.artifacts.resolvedArtifacts

    companion object {
        private const val EXTENSION_NAME = "runtimeClasspathView"

        internal fun register(
            project: Project,
            runtimeClasspath: Configuration,
            bootstrapClasspath: Configuration,
        ): RuntimeClasspathView {
            val artifacts = RuntimeClasspathView(runtimeClasspath, bootstrapClasspath)
            project.extensions.add(RuntimeClasspathView::class.java, EXTENSION_NAME, artifacts)
            return artifacts
        }

        internal fun of(project: Project): RuntimeClasspathView =
            project.extensions.getByType()
    }
}

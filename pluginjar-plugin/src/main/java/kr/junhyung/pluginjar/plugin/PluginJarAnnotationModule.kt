package kr.junhyung.pluginjar.plugin

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.slf4j.Logger

class PluginJarAnnotationModule(
    private val logger: Logger,
    private val dependencyHandler: DependencyHandler
) {

    private companion object {
        private const val GROUP = "kr.junhyung"
        private const val API_MODULE = "pluginjar-annotation"
    }

    fun addToCompileClasspath(project: Project) {
        val apiModule = PluginJarAnnotationModule(project.logger, project.dependencies)
        project.dependencies.add("compileOnly", apiModule.getDependency())
    }

    private fun getDependency(): Dependency {
        val dependencyNotation = "${GROUP}:${API_MODULE}:${getImplementationVersion()}"
        logger.trace("Dependency notation: $dependencyNotation")
        return dependencyHandler.create(dependencyNotation)
    }

    private fun getImplementationVersion(): String {
        val implementationVersion = javaClass.`package`.implementationVersion
        if (implementationVersion == null) {
            throw IllegalStateException("Implementation version not found.")
        } else {
            logger.trace("Implementation version: $implementationVersion")
            return implementationVersion
        }
    }
}
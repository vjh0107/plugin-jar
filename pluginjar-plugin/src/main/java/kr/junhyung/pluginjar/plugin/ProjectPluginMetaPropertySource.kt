package kr.junhyung.pluginjar.plugin

import kr.junhyung.pluginjar.core.PluginMainClass
import kr.junhyung.pluginjar.core.PluginMainClassResolver
import kr.junhyung.pluginjar.core.PluginMeta
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

class ProjectPluginMetaPropertySource(
    private val project: Project,
    private val pluginMainClassResolver: PluginMainClassResolver
) : PluginMetaPropertySource {

    override fun getName(): String {
        val name = findExtension()?.name
        return name ?: project.rootProject.name
    }

    override fun findMain(): String? {
        val main = findExtension()?.main
        if (main != null) {
            return main
        }
        val mainSourceDirectories = project
            .extensions
            .getByType<SourceSetContainer>()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .output
            .classesDirs
            .filter { file -> file.exists() }

        var pluginMainClass: PluginMainClass? = null
        for (directory in mainSourceDirectories) {
            val mainClass = pluginMainClassResolver.resolve(directory)
            if (mainClass != null) {
                pluginMainClass = mainClass
                break
            }
        }
        if (pluginMainClass == null) {
            return null
        }
        return pluginMainClass.qualifiedClassName
    }

    override fun getVersion(): String {
        val version = findExtension()?.version
        return version ?: project.version.toString()
    }

    override fun findApiVersion(): String? {
        val apiVersion = findExtension()?.apiVersion
        if (apiVersion != null) {
            return apiVersion
        }
        val compileClasspathDependencies = project
            .configurations
            .getByName("compileClasspath")
            .allDependencies
        val implementation = compileClasspathDependencies.find { dependency ->
            BukkitImplementations.values().any { implementation ->
                implementation.group == dependency.group && implementation.artifact == dependency.name
            }
        } ?: return null
        val implementationVersion = implementation.version ?: return null
        return BukkitVersion.parse(implementationVersion).getApiVersion()
    }

    override fun findDescription(): String? {
        val description = findExtension()?.description
        return description ?: project.description
    }

    override fun findLoad(): PluginMeta.Load? {
        return findExtension()?.load
    }

    override fun findAuthor(): String? {
        return findExtension()?.author
    }

    override fun getAuthors(): List<String> {
        return findExtension()?.authors ?: emptyList()
    }

    override fun findWebsite(): String? {
        return findExtension()?.website
    }

    override fun findPrefix(): String? {
        return findExtension()?.prefix
    }

    override fun getDepend(): List<String> {
        return findExtension()?.depend ?: emptyList()
    }

    override fun getSoftDepend(): List<String> {
        return findExtension()?.softDepend ?: emptyList()
    }

    override fun getLoadBefore(): List<String> {
        return findExtension()?.loadBefore ?: emptyList()
    }

    override fun getLibraries(): List<String> {
        val libraries = findExtension()?.libraries
        if (!libraries.isNullOrEmpty()) {
            return libraries
        }
        val result = mutableSetOf<String>()
        collectDependencies(project, result)
        return result.toList()
    }

    private fun findExtension(): PluginMeta? {
        return project.extensions.findByType<PluginMeta>()
    }

    private fun collectDependencies(project: Project, result: MutableSet<String>) {
        project
            .configurations
            .getByName("runtimeClasspath")
            .allDependencies
            .map { dependency ->
                when (dependency) {
                    is ProjectDependency -> collectDependencies(dependency.dependencyProject, result)
                    is ExternalModuleDependency -> result.add(dependency.group + ":" + dependency.name + ":" + dependency.version)
                    else -> {
                        project.logger.warn("Unsupported dependency type: ${dependency.javaClass.name}")
                    }
                }
            }
    }
}
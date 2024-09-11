package kr.junhyung.pluginjar.plugin

import kr.junhyung.pluginjar.core.PluginMainClassResolver
import kr.junhyung.pluginjar.core.PluginMeta
import kr.junhyung.pluginjar.core.PluginMetaSerializer
import kr.junhyung.pluginjar.plugin.dsl.internal.runtimeClasspath
import kr.junhyung.pluginjar.plugin.service.PluginMetaService
import kr.junhyung.pluginjar.plugin.service.SerializedPluginMetaService
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.util.GradleVersion
import java.util.*

@Suppress("unused")
class PluginJarPlugin : Plugin<Project> {

    companion object {
        const val PLUGIN_JAR_TASK_NAME = "pluginJar"
        const val PLUGIN_META_EXTENSION_NAME = "pluginJar"
        const val PROCESS_PLUGIN_RESOURCES_TASK_NAME = "processPluginResources"

        const val OUTPUT_DIRECTORY = "generated/pluginJar"
        const val PLUGIN_META_FILE_NAME = "plugin.yml"
    }

    override fun apply(project: Project) {
        if (project.plugins.hasPlugin(JavaPlugin::class.java)) {
            project.pluginManager.apply(JavaPlugin::class.java)
        }
        if (GradleVersion.current() < GradleVersion.version("8.3")) {
            throw GradleException("This version of PluginJar supports Gradle 8.3+ only. Please upgrade.")
        }
        addPluginJarAnnotationsModule(project)

        project.extensions.add(PLUGIN_META_EXTENSION_NAME, PluginMeta())
        configureTasks(project)
        project.afterEvaluate(BukkitDependencyAdvisor())
    }

    private fun configureTasks(project: Project) {
        registerPluginMetaService(project)
        val outputDirectory = project.layout.buildDirectory.dir(OUTPUT_DIRECTORY).get()
        val resourceTask =
            project.tasks.register(PROCESS_PLUGIN_RESOURCES_TASK_NAME, ProcessPluginResources::class.java) {
                dependsOn("classes")
                this.outputDirectory.set(outputDirectory)
            }
        project.tasks.register(PLUGIN_JAR_TASK_NAME, Jar::class.java) {
            group = "build"
            dependsOn(resourceTask)
            from(getSourceSetOutput(project))
            getProjectDependencies(project).forEach { dependencyProject ->
                from(getSourceSetOutput(dependencyProject))
            }
            from(outputDirectory)
        }
    }

    private fun registerPluginMetaService(project: Project) {
        val source = ProjectPluginMetaPropertySource(project, getPluginMainClassResolver())
        val service = SerializedPluginMetaService(getPluginMetaSerializer(), source)
        project.extensions.add(PluginMetaService::class.java, "pluginMetaService", service)
    }

    private fun addPluginJarAnnotationsModule(project: Project) {
        val apiModule = PluginJarAnnotationModule(project.logger, project.dependencies)
        apiModule.addToCompileClasspath(project)
    }

    private fun getPluginMainClassResolver(): PluginMainClassResolver {
        return ServiceLoader.load(PluginMainClassResolver::class.java).single()
    }

    private fun getPluginMetaSerializer(): PluginMetaSerializer {
        return ServiceLoader.load(PluginMetaSerializer::class.java).single()
    }

    private fun getSourceSetOutput(project: Project): FileCollection {
        return project
            .extensions
            .getByType<SourceSetContainer>()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .output
    }

    private fun getProjectDependencies(project: Project): Set<Project> {
        val result = mutableSetOf<Project>()
        collectProjectDependencies(project, result)
        return result
    }

    private fun collectProjectDependencies(project: Project, state: MutableSet<Project>) {
        project
            .configurations
            .runtimeClasspath
            .allDependencies
            .map { dependency ->
                when (dependency) {
                    is ProjectDependency -> {
                        state.add(dependency.dependencyProject)
                        collectProjectDependencies(dependency.dependencyProject, state)
                    }
                    else -> return@map
                }
            }
    }
}
package kr.junhyung.pluginjar.plugin

import kr.junhyung.pluginjar.core.PluginMainClassResolver
import kr.junhyung.pluginjar.core.PluginMeta
import kr.junhyung.pluginjar.core.PluginMetaSerializer
import kr.junhyung.pluginjar.plugin.service.PluginMetaService
import kr.junhyung.pluginjar.plugin.service.YamlPluginMetaService
import kr.junhyung.pluginjar.plugin.tasks.PluginJar
import kr.junhyung.pluginjar.plugin.tasks.ProcessPluginResources
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*

@Suppress("unused")
class PluginJarPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        addPluginJarAnnotationsModule(project)

        project.extensions.add("pluginJar", PluginMeta())
        configureTasks(project)
    }

    private fun configureTasks(project: Project) {
        registerPluginMetaService(project)
        val outputDirectory = project.layout.buildDirectory.dir("generated/plugin-jar").get()
        val resourceTask = project.tasks.register("processPluginResources", ProcessPluginResources::class.java) {
            dependsOn("classes")
            this.outputDirectory.set(outputDirectory)
        }
        project.tasks.register("pluginJar", PluginJar::class.java) {
            dependsOn(resourceTask)
            from(outputDirectory)
        }
    }

    private fun registerPluginMetaService(project: Project) {
        val source = ProjectPluginMetaPropertySource(project, getPluginMainClassResolver())
        val service = YamlPluginMetaService(getPluginMetaSerializer(), source)
        project.extensions.add(PluginMetaService::class.java, "pluginMetaService", service)
    }

    private fun addPluginJarAnnotationsModule(project: Project) {
        val apiModule = PluginJarAnnotationsModule(project.logger, project.dependencies)
        apiModule.addToCompileClasspath(project)
    }

    private fun getPluginMainClassResolver(): PluginMainClassResolver {
        return ServiceLoader.load(PluginMainClassResolver::class.java).single()
    }

    private fun getPluginMetaSerializer(): PluginMetaSerializer {
        return ServiceLoader.load(PluginMetaSerializer::class.java).single()
    }
}
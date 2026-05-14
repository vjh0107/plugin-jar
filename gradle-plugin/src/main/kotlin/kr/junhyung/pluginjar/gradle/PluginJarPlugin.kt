package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.image.PluginImageBuildPlugin
import kr.junhyung.pluginjar.gradle.nested.NestedPluginJarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

abstract class PluginJarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(NestedPluginJarPlugin::class.java)
        project.plugins.apply(PluginImageBuildPlugin::class.java)
    }
}

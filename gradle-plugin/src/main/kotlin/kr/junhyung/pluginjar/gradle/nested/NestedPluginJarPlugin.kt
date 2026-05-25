package kr.junhyung.pluginjar.gradle.nested

import kr.junhyung.pluginjar.gradle.manifest.PluginRuntimeClasspath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.withType

abstract class NestedPluginJarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            val classpath = PluginRuntimeClasspath.of(project)
            NestedPluginJar.register(project, classpath)
        }
    }
}

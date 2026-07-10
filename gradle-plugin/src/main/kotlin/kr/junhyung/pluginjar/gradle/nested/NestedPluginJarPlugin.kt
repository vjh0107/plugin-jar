package kr.junhyung.pluginjar.gradle.nested

import kr.junhyung.pluginjar.gradle.PluginJarBasePlugin
import kr.junhyung.pluginjar.gradle.RuntimeClasspathView
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.withType

abstract class NestedPluginJarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(PluginJarBasePlugin::class.java)
        project.plugins.withType<JavaPlugin> {
            val artifacts = RuntimeClasspathView.of(project)
            NestedPluginJar.register(project, artifacts)
        }
    }
}

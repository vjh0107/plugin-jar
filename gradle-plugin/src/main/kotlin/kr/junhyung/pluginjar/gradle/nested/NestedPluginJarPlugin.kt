package kr.junhyung.pluginjar.gradle.nested

import kr.junhyung.pluginjar.gradle.base.PluginExtension
import kr.junhyung.pluginjar.gradle.base.PluginJarBasePlugin
import kr.junhyung.pluginjar.gradle.base.PluginRuntimeClasspath
import kr.junhyung.pluginjar.gradle.base.ResolvePluginMarker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

abstract class NestedPluginJarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(PluginJarBasePlugin::class.java)
        project.plugins.withType<JavaPlugin> {
            val pluginExtension = project.extensions.getByType<PluginExtension>()
            val resolveMarker = project.tasks.named<ResolvePluginMarker>(ResolvePluginMarker.TASK_NAME)
            val classpath = PluginRuntimeClasspath.of(project)

            NestedPluginJar.register(project, pluginExtension, classpath, resolveMarker)
        }
    }
}

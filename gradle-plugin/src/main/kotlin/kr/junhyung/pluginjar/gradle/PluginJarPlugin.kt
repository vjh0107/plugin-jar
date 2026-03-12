package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.tasks.PluginJar
import kr.junhyung.pluginjar.gradle.tasks.ResolvePluginMarker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

abstract class PluginJarPlugin : Plugin<Project> {

    companion object {
        const val TASK_RESOLVE_PLUGIN_MARKER = "resolvePluginMarker"
        const val TASK_PLUGIN_JAR = "pluginJar"

        private const val ANNOTATIONS_ARTIFACT = "plugin-jar-annotations"

        val pluginVersion: String by lazy {
            PluginJarPlugin::class.java.`package`.implementationVersion
                ?: System.getProperty("pluginjar.version")
                ?: error("Could not locate plugin version")
        }

        val pluginGroup: String by lazy {
            PluginJarPlugin::class.java.`package`.implementationVendor
                ?: System.getProperty("pluginjar.group")
                ?: error("Could not locate plugin group")
        }
    }

    private val platforms: List<AbstractPlatformSupport> = listOf(
        PaperPluginJarSupport(),
        VelocityPluginJarSupport()
    )

    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            platforms.forEach { it.registerExtension(project) }
            registerCommonTasks(project)

            project.dependencies {
                add("compileOnly", "$pluginGroup:$ANNOTATIONS_ARTIFACT:$pluginVersion")
            }

            project.afterEvaluate {
                platforms
                    .filter { it.isApplicable(this) }
                    .forEach { it.configure(this) }
            }
        }
    }

    private fun registerCommonTasks(project: Project) {
        val sourceSets = project.extensions.getByType<SourceSetContainer>()
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

        project.tasks.register<ResolvePluginMarker>(TASK_RESOLVE_PLUGIN_MARKER) {
            classesDirs.from(mainSourceSet.output.classesDirs)
            outputFile.convention(project.layout.buildDirectory.file("pluginjar/plugin-meta.properties"))
            dependsOn(project.tasks.named("compileJava"))
        }

        project.tasks.register<PluginJar>(TASK_PLUGIN_JAR) {
            runtimeConfiguration.convention(project.configurations.named("runtimeClasspath"))
            dependsOn(project.tasks.withType<ProcessResources>())
        }
    }
}

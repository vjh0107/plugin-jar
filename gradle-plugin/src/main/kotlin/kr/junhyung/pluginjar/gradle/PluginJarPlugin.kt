package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.extensions.PaperPluginExtension
import kr.junhyung.pluginjar.gradle.tasks.GeneratePaperPluginYml
import kr.junhyung.pluginjar.gradle.tasks.PluginJar
import kr.junhyung.pluginjar.gradle.tasks.ResolvePluginMarker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.util.Properties

abstract class PluginJarPlugin : Plugin<Project> {

    companion object {
        const val TASK_RESOLVE_PLUGIN_MARKER = "resolvePluginMarker"
        const val TASK_PLUGIN_JAR = "pluginJar"
        const val TASK_GENERATE_PAPER_PLUGIN_YML = "generatePaperPluginYml"

        private const val ANNOTATIONS_ARTIFACT = "plugin-jar-annotations"
        private const val PAPER_RUNTIME_ARTIFACT = "plugin-jar-paper-plugin-loader"
        private const val DEFAULT_PAPER_LOADER = "kr.junhyung.pluginjar.paper.PluginJarPluginLoader"
        private const val DEFAULT_PAPER_API_VERSION = "1.21"

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

    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            val extension = project.extensions.create<PaperPluginExtension>("paperPlugin")

            project.dependencies {
                add("compileOnly", "$pluginGroup:$ANNOTATIONS_ARTIFACT:$pluginVersion")
                add("runtimeOnly", "$pluginGroup:$PAPER_RUNTIME_ARTIFACT:$pluginVersion")
            }

            val resolveMarker = registerResolvePluginMarker(project)
            registerPluginJar(project)
            val generateYml = registerGeneratePaperPluginYml(project, extension, resolveMarker)
            wireProcessResources(project, generateYml)
            configureConventions(project, extension, resolveMarker)
        }
    }

    private fun registerResolvePluginMarker(project: Project): TaskProvider<ResolvePluginMarker> {
        val mainSourceSet = project.extensions.getByType<SourceSetContainer>()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        return project.tasks.register<ResolvePluginMarker>(TASK_RESOLVE_PLUGIN_MARKER) {
            classesDirs.from(mainSourceSet.output.classesDirs)
            outputFile.convention(project.layout.buildDirectory.file("pluginjar/plugin-meta.properties"))
            dependsOn(project.tasks.named("compileJava"))
        }
    }

    private fun registerPluginJar(project: Project): TaskProvider<PluginJar> {
        return project.tasks.register<PluginJar>(TASK_PLUGIN_JAR) {
            runtimeConfiguration.convention(project.configurations.named("runtimeClasspath"))
            dependsOn(project.tasks.withType<ProcessResources>())
        }
    }

    private fun registerGeneratePaperPluginYml(
        project: Project,
        extension: PaperPluginExtension,
        resolveMarker: TaskProvider<ResolvePluginMarker>,
    ): TaskProvider<GeneratePaperPluginYml> {
        return project.tasks.register<GeneratePaperPluginYml>(TASK_GENERATE_PAPER_PLUGIN_YML) {
            outputFile.convention(project.layout.buildDirectory.file("pluginjar/paper-plugin.yml"))
            this.extension.set(extension)
            dependsOn(resolveMarker)
        }
    }

    private fun wireProcessResources(
        project: Project,
        generateYml: TaskProvider<GeneratePaperPluginYml>,
    ) {
        project.tasks.named<ProcessResources>("processResources") {
            from(generateYml.map { it.outputFile })
        }
    }

    private fun configureConventions(
        project: Project,
        extension: PaperPluginExtension,
        resolveMarker: TaskProvider<ResolvePluginMarker>,
    ) {
        val pluginMeta = resolveMarker.map { task -> readPluginMeta(task.outputFile.get().asFile) }

        extension.main.convention(pluginMeta.map { it.mainClass })
        extension.name.convention(
            pluginMeta.map { it.name ?: project.name }.orElse(project.name)
        )
        extension.version.convention(project.provider { project.version.toString() })
        extension.apiVersion.convention(
            project.configurations.named("compileClasspath").map { extractApiVersion(it) }
        )
        extension.description.convention(
            project.provider { project.description.orEmpty() }.filter { it.isNotEmpty() }
        )
        extension.loader.convention(DEFAULT_PAPER_LOADER)
    }

    private fun readPluginMeta(file: File): PluginMeta {
        val properties = Properties().apply {
            file.inputStream().use { load(it) }
        }
        return PluginMeta(
            mainClass = properties.getProperty("mainClass"),
            name = properties.getProperty("name"),
        )
    }

    private fun extractApiVersion(config: Configuration): String {
        val paperApiDependency = config.resolvedConfiguration.resolvedArtifacts
            .find { it.moduleVersion.id.group == "io.papermc.paper" && it.moduleVersion.id.name == "paper-api" }

        if (paperApiDependency != null) {
            val version = paperApiDependency.moduleVersion.id.version
            val match = Regex("^(\\d+\\.\\d+(?:\\.\\d+)?)").find(version)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return DEFAULT_PAPER_API_VERSION
    }
}

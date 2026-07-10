package kr.junhyung.pluginjar.gradle.velocity

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper

@CacheableTask
abstract class GenerateVelocityPluginJson : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val main: Property<String>

    @get:Input
    @get:Optional
    abstract val id: Property<String>

    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    internal abstract val extensionSpec: Property<VelocityPluginExtensionSpec>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "plugin"
    }

    companion object {
        const val TASK_NAME = "generateVelocityPluginJson"
        private const val OUTPUT_PATH = "generated/pluginjar/velocity-plugin.json"
        private val ID_PATTERN = Regex("[a-z][a-z0-9-_]{0,63}")

        internal fun register(
            project: Project,
            extension: VelocityPluginExtension,
        ): TaskProvider<GenerateVelocityPluginJson> =
            project.tasks.register<GenerateVelocityPluginJson>(TASK_NAME) {
                outputFile.convention(project.layout.buildDirectory.file(OUTPUT_PATH))
                projectName.set(project.name)
                main.set(extension.main)
                id.set(extension.id)
                extensionSpec.set(project.provider { VelocityPluginExtensionSpec.from(extension) })
            }
    }

    @TaskAction
    fun generate() {
        val mapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build()
        mapper.writeValue(outputFile.get().asFile, buildManifest())
    }

    private fun buildManifest(): VelocityPluginManifest {
        val spec = extensionSpec.get()
        val resolvedId = id.orNull ?: projectName.get()
        require(ID_PATTERN.matches(resolvedId)) {
            "Velocity plugin id '$resolvedId' is invalid. It must match '${ID_PATTERN.pattern}'. " +
                "Set a valid id via @PluginMarker(id = \"...\") or the velocityPlugin extension."
        }
        val resolvedMain = main.orNull
            ?: error(
                "Plugin main class is not resolved. Annotate the main class with @PluginMarker " +
                    "or set 'velocityPlugin.main'."
            )

        return VelocityPluginManifest(
            id = resolvedId,
            name = spec.name,
            version = spec.version.takeIf { it.isNotEmpty() },
            description = spec.description?.takeIf { it.isNotEmpty() },
            url = spec.url,
            authors = spec.authors?.takeIf { it.isNotEmpty() },
            dependencies = spec.dependencies
                .map { VelocityPluginManifest.Dependency(it.id, it.optional) }
                .takeIf { it.isNotEmpty() },
            main = resolvedMain,
        )
    }
}

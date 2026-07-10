package kr.junhyung.pluginjar.gradle.paper

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
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature

@CacheableTask
abstract class GeneratePaperPluginYml : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val main: Property<String>

    @get:Input
    @get:Optional
    abstract val pluginName: Property<String>

    @get:Input
    @get:Optional
    abstract val apiVersion: Property<String>

    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    internal abstract val extensionSpec: Property<PaperPluginExtensionSpec>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "plugin"
    }

    companion object {
        const val TASK_NAME = "generatePaperPluginYml"
        private const val FALLBACK_API_VERSION = "1.21"
        private const val OUTPUT_PATH = "generated/pluginjar/paper-plugin.yml"
        internal const val PLUGIN_LOADER = "kr.junhyung.pluginjar.paper.PluginJarPluginLoader"

        internal fun register(
            project: Project,
            extension: PaperPluginExtension,
        ): TaskProvider<GeneratePaperPluginYml> =
            project.tasks.register<GeneratePaperPluginYml>(TASK_NAME) {
                outputFile.convention(project.layout.buildDirectory.file(OUTPUT_PATH))
                projectName.set(project.name)
                main.set(extension.main)
                pluginName.set(extension.name)
                apiVersion.set(extension.apiVersion)
                extensionSpec.set(project.provider { PaperPluginExtensionSpec.from(extension) })
            }
    }

    @TaskAction
    fun generate() {
        val mapper = YAMLMapper.builder()
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .build()
        mapper.writeValue(outputFile.get().asFile, buildManifest())
    }

    private fun buildManifest(): PaperPluginManifest {
        val spec = extensionSpec.get()
        val resolvedMain = main.orNull
            ?: error(
                "Plugin main class is not resolved. Annotate the main class with @PluginMarker " +
                    "or set 'paperPlugin.main'."
            )

        return PaperPluginManifest(
            main = resolvedMain,
            name = pluginName.orNull ?: projectName.get(),
            version = spec.version,
            apiVersion = apiVersion.orNull ?: FALLBACK_API_VERSION,
            description = spec.description?.takeIf { it.isNotEmpty() },
            author = spec.author,
            authors = spec.authors?.takeIf { it.isNotEmpty() },
            website = spec.website,
            prefix = spec.prefix,
            defaultPermission = spec.defaultPermission,
            bootstrapper = spec.bootstrapper,
            loader = PLUGIN_LOADER,
            hasOpenClassloader = spec.hasOpenClassloader,
            dependencies = buildDependencies(spec),
        )
    }

    private fun buildDependencies(spec: PaperPluginExtensionSpec): Map<String, Map<String, PaperPluginManifest.DependencyDescriptor>>? = buildMap {
        if (spec.serverDependencies.isNotEmpty()) {
            put("server", spec.serverDependencies.associate { it.name to toDescriptor(it) })
        }
        if (spec.bootstrapDependencies.isNotEmpty()) {
            put("bootstrap", spec.bootstrapDependencies.associate { it.name to toDescriptor(it) })
        }
    }.takeIf { it.isNotEmpty() }

    private fun toDescriptor(entry: PaperPluginExtensionSpec.DependencyEntry): PaperPluginManifest.DependencyDescriptor =
        PaperPluginManifest.DependencyDescriptor(
            required = entry.required,
            load = entry.load,
            joinClasspath = entry.joinClasspath,
        )
}

package kr.junhyung.pluginjar.gradle.manifest

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature

@CacheableTask
abstract class GeneratePaperPluginYml : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val markerFile: RegularFileProperty

    @get:Input
    internal abstract val extensionSpec: Property<PaperPluginExtensionSpec>

    @get:Input
    abstract val projectName: Property<String>

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
            extension: PluginExtension,
            resolveMarker: TaskProvider<ResolvePluginMarker>,
        ): TaskProvider<GeneratePaperPluginYml> =
            project.tasks.register<GeneratePaperPluginYml>(TASK_NAME) {
                outputFile.convention(project.layout.buildDirectory.file(OUTPUT_PATH))
                markerFile.set(resolveMarker.flatMap { it.outputFile })
                extensionSpec.set(project.provider { PaperPluginExtensionSpec.from(extension) })
                projectName.set(project.name)
            }
    }

    @TaskAction
    fun generate() {
        val marker = ResolvePluginMarker.PluginMeta.readFrom(markerFile.get().asFile)
        val manifest = buildManifest(marker)
        val mapper = YAMLMapper.builder()
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .build()
        mapper.writeValue(outputFile.get().asFile, manifest)
    }

    private fun buildManifest(marker: ResolvePluginMarker.PluginMeta): PaperPluginManifest {
        val spec = extensionSpec.get()
        val name = chooseExclusive("name", spec.name, marker.name) ?: projectName.get()
        val apiVersion = chooseExclusive("apiVersion", spec.apiVersion, marker.apiVersion)
            ?: FALLBACK_API_VERSION

        return PaperPluginManifest(
            main = spec.main ?: marker.mainClass,
            name = name,
            version = spec.version,
            apiVersion = apiVersion,
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

    private fun chooseExclusive(field: String, fromExtension: String?, fromMarker: String?): String? {
        if (fromExtension != null && fromMarker != null) {
            error(
                "'$field' is declared both in @PluginMarker and the paperPlugin extension. " +
                    "Specify it in only one place."
            )
        }
        return fromExtension ?: fromMarker
    }

    private fun toDescriptor(entry: PaperPluginExtensionSpec.DependencyEntry): PaperPluginManifest.DependencyDescriptor =
        PaperPluginManifest.DependencyDescriptor(
            required = entry.required,
            load = entry.load,
            joinClasspath = entry.joinClasspath,
        )
}

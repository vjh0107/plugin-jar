package kr.junhyung.pluginjar.gradle.tasks

import kr.junhyung.pluginjar.gradle.PluginMeta
import kr.junhyung.pluginjar.gradle.PluginMetaResolver
import kr.junhyung.pluginjar.gradle.annotation.AnnotationPluginMetaResolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.util.Properties

@DisableCachingByDefault(because = "Not worth caching")
abstract class ResolvePluginMarker : DefaultTask() {

    @get:Optional
    @get:Input
    abstract val configuredMainClass: Property<String>

    @get:Optional
    @get:Input
    abstract val configuredName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Internal
    var pluginMetaResolver: PluginMetaResolver = AnnotationPluginMetaResolver()

    init {
        description = "Resolves plugin info from @PluginMarker annotation"
        group = "plugin"
    }

    @TaskAction
    fun resolve() {
        val meta = resolvePluginMeta()
        val properties = Properties().apply {
            setProperty("mainClass", meta.mainClass)
            meta.name?.let { setProperty("name", it) }
        }
        outputFile.get().asFile.outputStream().use {
            properties.store(it, null)
        }
    }

    private fun resolvePluginMeta(): PluginMeta {
        val resolved = pluginMetaResolver.resolve(classesDirs.files)

        val mainClass = when {
            configuredMainClass.isPresent -> configuredMainClass.get()
            resolved != null -> resolved.mainClass
            else -> throw IllegalStateException(
                "Cannot find plugin main class. Annotate your main class with @PluginMarker or set 'mainClass' manually."
            )
        }

        val name = when {
            configuredName.isPresent -> configuredName.get()
            else -> resolved?.name
        }

        return PluginMeta(mainClass, name)
    }

    fun readPluginMeta(): PluginMeta {
        val properties = Properties().apply {
            outputFile.get().asFile.inputStream().use { load(it) }
        }
        return PluginMeta(
            mainClass = properties.getProperty("mainClass"),
            name = properties.getProperty("name")
        )
    }
}

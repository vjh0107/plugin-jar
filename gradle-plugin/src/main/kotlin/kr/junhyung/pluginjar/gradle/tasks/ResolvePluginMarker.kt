package kr.junhyung.pluginjar.gradle.tasks

import kr.junhyung.pluginjar.gradle.annotation.AnnotationPluginMetaResolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.Properties

@DisableCachingByDefault(because = "Not worth caching")
abstract class ResolvePluginMarker : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Resolves plugin info from @PluginMarker annotation"
        group = "plugin"
    }

    @TaskAction
    fun resolve() {
        val meta = AnnotationPluginMetaResolver.resolve(classesDirs.files)
            ?: throw IllegalStateException(
                "Cannot find plugin main class. Annotate your main class with @PluginMarker."
            )
        val properties = Properties().apply {
            setProperty("mainClass", meta.mainClass)
            meta.name?.let { setProperty("name", it) }
        }
        outputFile.get().asFile.outputStream().use {
            properties.store(it, null)
        }
    }
}

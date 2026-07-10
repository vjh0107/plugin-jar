package kr.junhyung.pluginjar.gradle.image

import kr.junhyung.pluginjar.gradle.RuntimeClasspathView
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

@CacheableTask
abstract class PluginImageBootstrap : Jar() {

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bootstrapLibraries: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mainJar: RegularFileProperty

    init {
        group = "plugin"

        from(bootstrapLibraries.elements.map { entries ->
            entries.map { archiveOperations.zipTree(it.asFile) }
        })

        from(archiveOperations.zipTree(mainJar)) {
            exclude("META-INF/MANIFEST.MF")
        }
    }

    companion object {
        const val TASK_NAME = "pluginImageBootstrap"

        private const val PLUGIN_DIR_LOCATION = "docker-context/plugin"

        internal fun register(
            project: Project,
            extension: PluginImageExtension,
            artifacts: RuntimeClasspathView,
        ): TaskProvider<PluginImageBootstrap> {
            val mainJarTask = project.tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME)
            return project.tasks.register<PluginImageBootstrap>(TASK_NAME) {
                archiveBaseName.set(extension.pluginName.orElse(project.name))
                destinationDirectory.set(project.layout.buildDirectory.dir(PLUGIN_DIR_LOCATION))
                bootstrapLibraries.from(artifacts.bootstrapLibraries)
                mainJar.set(mainJarTask.flatMap { it.archiveFile })
            }
        }
    }
}

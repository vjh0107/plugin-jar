package kr.junhyung.pluginjar.gradle.image

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.TarImage
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

@CacheableTask
abstract class PluginImageTar : AbstractPluginImageBuild() {

    @get:OutputFile
    abstract val tarOutputFile: RegularFileProperty

    companion object {
        const val TASK_NAME = "pluginImageTar"

        private const val TAR_PATH_TEMPLATE = "pluginjar/%s.tar"

        internal fun register(
            project: Project,
            imageExtension: PluginImageExtension,
            bootstrap: TaskProvider<PluginImageBootstrap>,
            libs: TaskProvider<Sync>,
            modules: TaskProvider<Sync>,
        ): TaskProvider<PluginImageTar> {
            val defaultTarFile = project.layout.buildDirectory.file(
                bootstrap.flatMap { it.archiveBaseName.map { name -> TAR_PATH_TEMPLATE.format(name) } }
            )
            return project.tasks.register<PluginImageTar>(TASK_NAME) {
                configureFrom(imageExtension, bootstrap, libs, modules)
                tarOutputFile.set(imageExtension.tarOutputFile.orElse(defaultTarFile))
            }
        }
    }

    override fun createContainerizer(): Containerizer {
        val tarPath = tarOutputFile.get().asFile.toPath()
        return Containerizer.to(TarImage.at(tarPath).named(parseTargetImage()))
    }
}

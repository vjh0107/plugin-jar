package kr.junhyung.pluginjar.gradle.image

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Loads an image into the local Docker daemon; not a cacheable local artifact")
abstract class PluginImageBuildLocal : AbstractPluginImageBuild() {

    companion object {
        const val TASK_NAME = "pluginImageBuildLocal"

        internal fun register(
            project: Project,
            imageExtension: PluginImageExtension,
            bootstrap: TaskProvider<PluginImageBootstrap>,
            libs: TaskProvider<Sync>,
            modules: TaskProvider<Sync>,
        ): TaskProvider<PluginImageBuildLocal> = project.tasks.register<PluginImageBuildLocal>(TASK_NAME) {
            configureFrom(imageExtension, bootstrap, libs, modules)
        }
    }

    override fun createContainerizer(): Containerizer =
        Containerizer.to(DockerDaemonImage.named(parseTargetImage()))
}

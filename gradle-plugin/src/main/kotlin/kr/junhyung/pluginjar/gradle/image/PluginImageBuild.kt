package kr.junhyung.pluginjar.gradle.image

import com.google.cloud.tools.jib.api.Containerizer
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Pushes an image to a remote registry; not a cacheable local artifact")
abstract class PluginImageBuild : AbstractPluginImageBuild() {

    @get:Internal
    abstract val username: Property<String>

    @get:Internal
    abstract val password: Property<String>

    companion object {
        const val TASK_NAME = "pluginImageBuild"

        internal fun register(
            project: Project,
            imageExtension: PluginImageExtension,
            bootstrap: TaskProvider<PluginImageBootstrap>,
            libs: TaskProvider<Sync>,
        ): TaskProvider<PluginImageBuild> = project.tasks.register<PluginImageBuild>(TASK_NAME) {
            configureFrom(imageExtension, bootstrap, libs)
            username.set(imageExtension.username)
            password.set(imageExtension.password)
        }
    }

    override fun createContainerizer(): Containerizer {
        val ref = parseTargetImage()
        val image = registryImageWithCredentials(ref, username.orNull, password.orNull)
        return Containerizer.to(image)
    }
}

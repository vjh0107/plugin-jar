package kr.junhyung.pluginjar.gradle

import kr.junhyung.pluginjar.gradle.tasks.ResolvePluginMarker
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

abstract class AbstractPlatformSupport(
    private val artifactName: String,
    private val dependencyConfiguration: String
) {

    abstract fun registerExtension(project: Project)

    abstract fun isApplicable(project: Project): Boolean

    fun configure(project: Project) {
        project.dependencies.add(
            dependencyConfiguration,
            "${PluginJarPlugin.pluginGroup}:$artifactName:${PluginJarPlugin.pluginVersion}"
        )

        val resolvePluginMarker = project.tasks.named<ResolvePluginMarker>(PluginJarPlugin.TASK_RESOLVE_PLUGIN_MARKER)
        val pluginMeta = resolvePluginMarker.get().let { task ->
            if (task.outputFile.get().asFile.exists()) task.readPluginMeta() else null
        }

        configureExtension(project, pluginMeta, resolvePluginMarker)
        registerTasks(project, resolvePluginMarker)
    }

    protected abstract fun configureExtension(
        project: Project,
        pluginMeta: PluginMeta?,
        resolvePluginMarker: TaskProvider<ResolvePluginMarker>
    )

    protected abstract fun registerTasks(
        project: Project,
        resolvePluginMarker: TaskProvider<ResolvePluginMarker>
    )
}

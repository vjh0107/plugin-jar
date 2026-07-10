package kr.junhyung.pluginjar.gradle.nested

import kr.junhyung.pluginjar.gradle.RuntimeClasspathView
import kr.junhyung.pluginjar.core.NestedLibraryExtractor
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

@CacheableTask
abstract class NestedPluginJar : Jar() {

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mainSourceOutput: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectModuleOutputs: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bootstrapLibraries: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val externalLibraries: ConfigurableFileCollection

    init {
        group = "plugin"

        archiveClassifier.convention("plugin")

        from(mainSourceOutput)
        from(projectModuleOutputs)
        from(bootstrapLibraries.elements.map { entries ->
            entries.map { archiveOperations.zipTree(it.asFile) }
        })

        into(NestedLibraryExtractor.LIBRARIES_DIR) {
            from(externalLibraries)
        }
    }

    companion object {
        const val TASK_NAME = "pluginJar"

        internal fun register(
            project: Project,
            artifacts: RuntimeClasspathView,
        ): TaskProvider<NestedPluginJar> =
            project.tasks.register<NestedPluginJar>(TASK_NAME) {
                mainSourceOutput.from(mainSourceOutputOf(project))
                projectModuleOutputs.from(artifacts.projectArtifacts.map { resolved ->
                    resolved.flatMap { artifact ->
                        val id = artifact.id.componentIdentifier as ProjectComponentIdentifier
                        mainSourceOutputOrNull(project.rootProject.project(id.projectPath))?.files.orEmpty()
                    }
                })
                bootstrapLibraries.from(artifacts.bootstrapLibraries)
                externalLibraries.from(artifacts.externalLibraries)
            }

        private fun mainSourceOutputOf(project: Project): FileCollection =
            project.extensions.getByType(SourceSetContainer::class.java)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME).output

        private fun mainSourceOutputOrNull(project: Project): FileCollection? {
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java) ?: return null
            return sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)?.output
        }
    }
}

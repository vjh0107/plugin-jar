package kr.junhyung.pluginjar.gradle.nested

import kr.junhyung.pluginjar.gradle.base.PluginExtension
import kr.junhyung.pluginjar.gradle.base.PluginRuntimeClasspath
import kr.junhyung.pluginjar.gradle.base.ResolvePluginMarker
import kr.junhyung.pluginjar.gradle.manifest.GeneratePaperPluginYml
import kr.junhyung.pluginjar.paper.LibraryExtractor
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
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
    abstract val pluginJarLibraries: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val externalLibraries: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val paperPluginYml: RegularFileProperty

    init {
        group = "plugin"

        archiveClassifier.convention("plugin")

        manifest.attributes(mapOf(PLUGIN_JAR_TYPE_HEADER to TYPE_NESTED))

        from(mainSourceOutput)
        from(projectModuleOutputs)
        from(pluginJarLibraries.elements.map { entries ->
            entries.map { archiveOperations.zipTree(it.asFile) }
        })
        from(paperPluginYml)

        into(LibraryExtractor.LIBRARIES_DIR) {
            from(externalLibraries)
        }
    }

    companion object {
        const val TASK_NAME = "pluginJar"

        private const val PLUGIN_JAR_TYPE_HEADER = "Plugin-Jar-Type"
        private const val TYPE_NESTED = "nested"
        private const val YML_TASK_NAME = "generateNestedJarPaperPluginYml"
        private const val YML_OUTPUT_PATH = "pluginjar/nested-jar/paper-plugin.yml"

        internal fun register(
            project: Project,
            extension: PluginExtension,
            classpath: PluginRuntimeClasspath,
            resolveMarker: TaskProvider<ResolvePluginMarker>,
        ): TaskProvider<NestedPluginJar> {
            val yml = GeneratePaperPluginYml.register(
                project, YML_TASK_NAME, YML_OUTPUT_PATH, extension, resolveMarker,
            )
            return project.tasks.register<NestedPluginJar>(TASK_NAME) {
                mainSourceOutput.from(mainSourceOutputOf(project))
                projectModuleOutputs.from(classpath.projectArtifacts.map { artifacts ->
                    artifacts.flatMap { artifact ->
                        val id = artifact.id.componentIdentifier as ProjectComponentIdentifier
                        mainSourceOutputOrNull(project.rootProject.project(id.projectPath))?.files.orEmpty()
                    }
                })
                pluginJarLibraries.from(classpath.pluginJarLibraries)
                externalLibraries.from(classpath.externalLibraries)
                paperPluginYml.set(yml.flatMap { it.outputFile })
            }
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

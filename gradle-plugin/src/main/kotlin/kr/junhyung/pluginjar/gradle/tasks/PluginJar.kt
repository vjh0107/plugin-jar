package kr.junhyung.pluginjar.gradle.tasks

import kr.junhyung.pluginjar.core.LibraryExtractor
import kr.junhyung.pluginjar.gradle.PluginJarPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.CacheableTask
import org.gradle.jvm.tasks.Jar
import java.io.File

@CacheableTask
abstract class PluginJar : Jar() {

    @get:Internal
    abstract val runtimeConfiguration: Property<Configuration>

    init {
        group = "plugin"
        description = "Builds a JAR with all dependencies bundled in BOOT-INF/lib"

        archiveBaseName.convention(project.rootProject.name)
        archiveClassifier.convention("plugin")
        destinationDirectory.convention(project.layout.buildDirectory.dir("libs"))

        from(project.mainSourceSetOutput())
        from(project.provider { resolveProjectOutputs() })
        from(project.provider { resolvePluginJarLibraries().map { project.zipTree(it) } })

        into(LibraryExtractor.LIBRARIES_DIR) {
            from(project.provider { resolveExternalLibraries() })
        }
    }

    private fun resolveProjectOutputs(): List<FileCollection> {
        return runtimeConfiguration.get()
            .incoming
            .resolutionResult
            .allComponents
            .mapNotNull { component ->
                val id = component.id
                if (id is ProjectComponentIdentifier) {
                    project.rootProject.project(id.projectPath).mainSourceSetOutput()
                } else {
                    null
                }
            }
    }

    private fun resolvePluginJarLibraries(): List<File> {
        return runtimeConfiguration.get()
            .incoming
            .artifacts
            .artifacts
            .filter { artifact ->
                val id = artifact.id.componentIdentifier
                id is ModuleComponentIdentifier && id.group == PluginJarPlugin.pluginGroup
            }
            .map { it.file }
    }

    private fun resolveExternalLibraries(): List<File> {
        return runtimeConfiguration.get()
            .incoming
            .artifacts
            .artifacts
            .filter { artifact ->
                val id = artifact.id.componentIdentifier
                id !is ProjectComponentIdentifier && !(id is ModuleComponentIdentifier && id.group == PluginJarPlugin.pluginGroup)
            }
            .map { it.file }
    }

    private fun Project.mainSourceSetOutput(): FileCollection? {
        val sourceSets = extensions.findByType(SourceSetContainer::class.java) ?: return null
        return sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)?.output
    }

}

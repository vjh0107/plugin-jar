package kr.junhyung.pluginjar.gradle.paper

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

abstract class PaperPluginManifestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            val extension = project.extensions.create<PaperPluginExtension>("paperPlugin")
            extension.version.convention(project.provider { project.version.toString() })
            extension.description.convention(
                project.provider { project.description.orEmpty() }.filter { it.isNotEmpty() },
            )
            val manifestDir = GeneratePaperPluginYml.register(project, extension)
                .map { it.outputFile.get().asFile.parentFile }
            project.extensions.getByType<SourceSetContainer>()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .resources.srcDir(manifestDir)
        }
    }
}

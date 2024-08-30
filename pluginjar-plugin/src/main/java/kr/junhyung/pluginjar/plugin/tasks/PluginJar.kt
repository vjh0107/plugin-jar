package kr.junhyung.pluginjar.plugin.tasks

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType

open class PluginJar : Jar() {
    init {
        group = "build"
        configureRuntimeClasspathInclusion()
    }

    private fun configureRuntimeClasspathInclusion() {
        from(getSourceSetOutput(project))
        project.configurations
            .getByName("runtimeClasspath")
            .allDependencies
            .filterIsInstance<ProjectDependency>()
            .forEach { projectDependency ->
                from(getSourceSetOutput(projectDependency.dependencyProject))
            }
    }

    private fun getSourceSetOutput(project: Project): FileCollection {
        return project
            .extensions
            .getByType<SourceSetContainer>()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .output
    }
}
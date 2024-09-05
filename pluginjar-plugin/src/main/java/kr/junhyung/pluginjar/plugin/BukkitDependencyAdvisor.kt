package kr.junhyung.pluginjar.plugin

import kr.junhyung.pluginjar.plugin.dsl.internal.compileClasspath
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.cc.base.logger
import org.gradle.kotlin.dsl.getByType

class BukkitDependencyAdvisor : Action<Project> {
    override fun execute(project: Project) {
        val projectVersion = getProjectVersion(project)
        project
            .configurations
            .compileClasspath
            .allDependencies
            .filterIsInstance<BukkitDependency>()
            .forEach { bukkitDependency ->
                warnBukkitDependencyCompatibility(projectVersion, bukkitDependency)
            }
    }

    private fun warnBukkitDependencyCompatibility(projectVersion: JavaVersion, bukkitDependency: BukkitDependency) {
        val versionNotation = bukkitDependency.version ?: throw IllegalStateException("Bukkit dependency version is not set.")
        val bukkitVersion = BukkitVersion.parse(versionNotation)
        if (!bukkitVersion.isCompatibleWith(projectVersion)) {
            val minimumCompatibleVersion = bukkitVersion.getMinimumCompatibleVersion()
            logger.error("Bukkit version $versionNotation is not compatible with project target compatibility version $projectVersion. Minimum compatible version is Java $minimumCompatibleVersion.")
            throw UnsupportedClassVersionError("Bukkit version $versionNotation is not compatible with project target compatibility version $projectVersion. Minimum compatible version is Java $minimumCompatibleVersion.")
        }
    }

    private fun getProjectVersion(project: Project): JavaVersion {
        return project.extensions.getByType<JavaPluginExtension>().sourceCompatibility
    }
}
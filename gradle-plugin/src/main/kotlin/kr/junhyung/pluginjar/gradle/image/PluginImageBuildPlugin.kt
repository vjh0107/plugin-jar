package kr.junhyung.pluginjar.gradle.image

import kr.junhyung.pluginjar.gradle.base.PluginExtension
import kr.junhyung.pluginjar.gradle.base.PluginJarBasePlugin
import kr.junhyung.pluginjar.gradle.base.PluginRuntimeClasspath
import kr.junhyung.pluginjar.gradle.base.ResolvePluginMarker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

abstract class PluginImageBuildPlugin : Plugin<Project> {

    companion object {
        private const val LIBS_LOCATION = "docker-context/libs"
        private const val MODULES_LOCATION = "docker-context/modules"
        private const val COLLECT_LIBS_TASK = "pluginImageCollectLibs"
        private const val COLLECT_MODULES_TASK = "pluginImageCollectModules"
    }

    override fun apply(project: Project) {
        project.plugins.apply(PluginJarBasePlugin::class.java)
        project.plugins.withType<JavaPlugin> {
            val pluginExtension = project.extensions.getByType<PluginExtension>()
            val imageExtension = project.extensions.create<PluginImageExtension>("paperPluginImage")
            val resolveMarker = project.tasks.named<ResolvePluginMarker>(ResolvePluginMarker.TASK_NAME)

            val classpath = PluginRuntimeClasspath.of(project)
            val bootstrap = PluginImageBootstrap.register(project, pluginExtension, classpath, resolveMarker)
            val collectLibs = registerCollectLibs(project, classpath)
            val collectModules = registerCollectModules(project, classpath)

            val build = PluginImageBuild.register(project, imageExtension, bootstrap, collectLibs, collectModules)
            val buildLocal = PluginImageBuildLocal.register(project, imageExtension, bootstrap, collectLibs, collectModules)
            val tar = PluginImageTar.register(project, imageExtension, bootstrap, collectLibs, collectModules)
            buildLocal.configure { mustRunAfter(build) }
            tar.configure { mustRunAfter(build, buildLocal) }
        }
    }

    private fun registerCollectLibs(
        project: Project,
        classpath: PluginRuntimeClasspath,
    ): TaskProvider<Sync> {
        val location = project.layout.buildDirectory.dir(LIBS_LOCATION)
        return project.tasks.register<Sync>(COLLECT_LIBS_TASK) {
            group = "plugin"
            duplicatesStrategy = DuplicatesStrategy.FAIL
            from(classpath.externalLibraries)
            into(location)
        }
    }

    private fun registerCollectModules(
        project: Project,
        classpath: PluginRuntimeClasspath,
    ): TaskProvider<Sync> {
        val location = project.layout.buildDirectory.dir(MODULES_LOCATION)
        val mainJar = project.tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME)
        return project.tasks.register<Sync>(COLLECT_MODULES_TASK) {
            group = "plugin"
            duplicatesStrategy = DuplicatesStrategy.FAIL
            from(mainJar.flatMap { it.archiveFile })
            from(classpath.projectArtifacts.map { artifacts -> artifacts.map { it.file } })
            into(location)
        }
    }
}

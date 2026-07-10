package kr.junhyung.pluginjar.gradle.image

import kr.junhyung.pluginjar.gradle.PluginJarBasePlugin
import kr.junhyung.pluginjar.gradle.RuntimeClasspathView
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

abstract class PluginImageBuildPlugin : Plugin<Project> {

    companion object {
        private const val LIBS_LOCATION = "docker-context/libs"
        private const val MODULES_LOCATION = "docker-context/modules"
        private const val COLLECT_LIBS_TASK = "pluginImageCollectLibs"
        private const val COLLECT_MODULES_TASK = "pluginImageCollectModules"

        private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
        private const val SNAPSHOT_TAG = "snapshot"
        private const val LATEST_TAG = "latest"

        private const val HARBOR_USERNAME_ENV = "HARBOR_BOT_NAME"
        private const val HARBOR_SECRET_ENV = "HARBOR_BOT_SECRET"

        private fun buildDefaultTags(version: String, gitId: String, pushMovableTags: Boolean): List<String> {
            val tags = mutableListOf(version)
            when {
                version.endsWith(SNAPSHOT_SUFFIX) -> {
                    tags.add("$version.$gitId")
                    if (pushMovableTags) tags.add(SNAPSHOT_TAG)
                }
                version.contains('-') -> Unit // pre-release: immutable exact version only
                else -> if (pushMovableTags) tags.add(LATEST_TAG)
            }
            return tags
        }
    }

    private fun defaultTags(project: Project, extension: PluginImageExtension): Provider<List<String>> {
        val providers = project.providers
        val gitId = providers.of(GitDescribeValueSource::class.java) {
            parameters.projectDirectory.set(project.layout.projectDirectory)
        }
        val pushMovableTags = extension.pushMovableTags
            .orElse(providers.environmentVariable("BUILD_NUMBER").map { true })
            .orElse(false)
        val version = providers.provider { project.version.toString() }
        return version.zip(gitId, ::Pair).zip(pushMovableTags) { (resolvedVersion, resolvedGitId), movable ->
            buildDefaultTags(resolvedVersion, resolvedGitId, movable)
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply(PluginJarBasePlugin::class.java)
        project.plugins.withType<JavaPlugin> {
            val imageExtension = project.extensions.create<PluginImageExtension>("pluginImage")
            imageExtension.tags.convention(defaultTags(project, imageExtension))
            imageExtension.username.convention(project.providers.environmentVariable(HARBOR_USERNAME_ENV))
            imageExtension.password.convention(project.providers.environmentVariable(HARBOR_SECRET_ENV))

            val artifacts = RuntimeClasspathView.of(project)
            val bootstrap = PluginImageBootstrap.register(project, imageExtension, artifacts)
            val collectLibs = registerCollectLibs(project, artifacts)
            val collectModules = registerCollectModules(project, artifacts)

            val build = PluginImageBuild.register(project, imageExtension, bootstrap, collectLibs, collectModules)
            val buildLocal = PluginImageBuildLocal.register(project, imageExtension, bootstrap, collectLibs, collectModules)
            val tar = PluginImageTar.register(project, imageExtension, bootstrap, collectLibs, collectModules)
            buildLocal.configure { mustRunAfter(build) }
            tar.configure { mustRunAfter(build, buildLocal) }
        }
    }

    private fun registerCollectLibs(
        project: Project,
        artifacts: RuntimeClasspathView,
    ): TaskProvider<Sync> {
        val location = project.layout.buildDirectory.dir(LIBS_LOCATION)
        return project.tasks.register<Sync>(COLLECT_LIBS_TASK) {
            group = "plugin"
            duplicatesStrategy = DuplicatesStrategy.FAIL
            from(artifacts.externalLibraries)
            into(location)
        }
    }

    private fun registerCollectModules(
        project: Project,
        artifacts: RuntimeClasspathView,
    ): TaskProvider<Sync> {
        val location = project.layout.buildDirectory.dir(MODULES_LOCATION)
        return project.tasks.register<Sync>(COLLECT_MODULES_TASK) {
            group = "plugin"
            duplicatesStrategy = DuplicatesStrategy.FAIL
            from(artifacts.projectArtifacts.map { resolved -> resolved.map { it.file } })
            into(location)
        }
    }
}

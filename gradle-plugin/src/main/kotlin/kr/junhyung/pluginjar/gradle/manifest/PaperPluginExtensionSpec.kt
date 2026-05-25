package kr.junhyung.pluginjar.gradle.manifest

import java.io.Serializable

internal data class PaperPluginExtensionSpec(
    val main: String?,
    val name: String?,
    val version: String,
    val apiVersion: String?,
    val description: String?,
    val author: String?,
    val authors: List<String>?,
    val website: String?,
    val prefix: String?,
    val defaultPermission: String?,
    val bootstrapper: String?,
    val hasOpenClassloader: Boolean?,
    val serverDependencies: List<DependencyEntry>,
    val bootstrapDependencies: List<DependencyEntry>,
) : Serializable {

    data class DependencyEntry(
        val name: String,
        val required: Boolean?,
        val load: String?,
        val joinClasspath: Boolean?,
    ) : Serializable

    companion object {
        fun from(extension: PluginExtension): PaperPluginExtensionSpec = PaperPluginExtensionSpec(
            main = extension.main.orNull,
            name = extension.name.orNull,
            version = extension.version.get(),
            apiVersion = extension.apiVersion.orNull,
            description = extension.description.orNull,
            author = extension.author.orNull,
            authors = extension.authors.orNull,
            website = extension.website.orNull,
            prefix = extension.prefix.orNull,
            defaultPermission = extension.defaultPermission.orNull,
            bootstrapper = extension.bootstrapper.orNull,
            hasOpenClassloader = extension.hasOpenClassloader.orNull,
            serverDependencies = extension.serverDependencies.map { toEntry(it) },
            bootstrapDependencies = extension.bootstrapDependencies.map { toEntry(it) },
        )

        private fun toEntry(definition: PluginExtension.DependencyDefinition): DependencyEntry = DependencyEntry(
            name = definition.name,
            required = definition.required.orNull,
            load = definition.load.orNull?.name,
            joinClasspath = definition.joinClasspath.orNull,
        )
    }
}

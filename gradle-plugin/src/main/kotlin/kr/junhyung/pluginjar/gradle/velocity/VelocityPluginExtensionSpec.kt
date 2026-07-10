package kr.junhyung.pluginjar.gradle.velocity

import java.io.Serializable

internal data class VelocityPluginExtensionSpec(
    val name: String?,
    val version: String,
    val description: String?,
    val url: String?,
    val authors: List<String>?,
    val dependencies: List<DependencyEntry>,
) : Serializable {

    data class DependencyEntry(
        val id: String,
        val optional: Boolean,
    ) : Serializable

    companion object {
        fun from(extension: VelocityPluginExtension): VelocityPluginExtensionSpec = VelocityPluginExtensionSpec(
            name = extension.name.orNull,
            version = extension.version.get(),
            description = extension.description.orNull,
            url = extension.url.orNull,
            authors = extension.authors.orNull,
            dependencies = extension.dependencies.map { DependencyEntry(it.name, it.optional.getOrElse(false)) },
        )
    }
}

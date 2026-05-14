package kr.junhyung.pluginjar.gradle.image

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class PluginImageExtension @Inject constructor(layout: ProjectLayout) {

    abstract val targetImage: Property<String>

    abstract val username: Property<String>

    abstract val password: Property<String>

    abstract val applicationLayersCache: DirectoryProperty

    abstract val baseImageLayersCache: DirectoryProperty

    abstract val tarOutputFile: RegularFileProperty

    init {
        applicationLayersCache.convention(layout.buildDirectory.dir(APPLICATION_LAYERS_CACHE))
        baseImageLayersCache.convention(layout.buildDirectory.dir(BASE_IMAGE_LAYERS_CACHE))
    }

    private companion object {
        const val APPLICATION_LAYERS_CACHE = "pluginjar/jib-cache/application"
        const val BASE_IMAGE_LAYERS_CACHE = "pluginjar/jib-cache/base"
    }
}

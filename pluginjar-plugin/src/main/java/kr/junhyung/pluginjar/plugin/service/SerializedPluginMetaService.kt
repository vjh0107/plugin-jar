package kr.junhyung.pluginjar.plugin.service

import kr.junhyung.pluginjar.core.PluginMeta
import kr.junhyung.pluginjar.core.PluginMetaSerializer
import kr.junhyung.pluginjar.plugin.PluginMetaPropertySource
import java.io.File

class SerializedPluginMetaService(
    private val pluginMetaSerializer: PluginMetaSerializer,
    private val pluginMetaPropertySource: PluginMetaPropertySource
) : PluginMetaService {
    override fun generatePluginMetaFile(output: File) {
        val pluginMeta = createPluginMeta()
        pluginMetaSerializer.serialize(pluginMeta, output)
    }

    private fun createPluginMeta(): PluginMeta {
        val mainProperty = pluginMetaPropertySource.findMain()
            ?: throw IllegalStateException("Cannot find an annotated main class with @Plugin. Please specify a main class.")
        return PluginMeta().apply {
            name = pluginMetaPropertySource.getName()
            main = mainProperty
            version = pluginMetaPropertySource.getVersion()
            apiVersion = pluginMetaPropertySource.findApiVersion()
            description = pluginMetaPropertySource.findDescription()
            load = pluginMetaPropertySource.findLoad()
            author = pluginMetaPropertySource.findAuthor()
            authors = pluginMetaPropertySource.getAuthors()
            website = pluginMetaPropertySource.findWebsite()
            prefix = pluginMetaPropertySource.findPrefix()
            depend = pluginMetaPropertySource.getDepend()
            softDepend = pluginMetaPropertySource.getSoftDepend()
            loadBefore = pluginMetaPropertySource.getLoadBefore()
            libraries = pluginMetaPropertySource.getLibraries()
        }
    }
}
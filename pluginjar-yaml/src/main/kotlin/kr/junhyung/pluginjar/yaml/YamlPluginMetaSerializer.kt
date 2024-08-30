package kr.junhyung.pluginjar.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auto.service.AutoService
import kr.junhyung.pluginjar.core.PluginMeta
import kr.junhyung.pluginjar.core.PluginMetaSerializer
import java.io.File

@Suppress("unused")
@AutoService(PluginMetaSerializer::class)
class YamlPluginMetaSerializer : PluginMetaSerializer {
    private val objectMapper: ObjectMapper

    @Suppress("unused") // used by ServiceLoader
    constructor() : this(PluginMetaObjectMapperFactory.create())

    constructor(objectMapper: ObjectMapper) {
        this.objectMapper = objectMapper
    }

    override fun serialize(pluginMeta: PluginMeta, output: File) {
        objectMapper.writeValue(output, pluginMeta)
    }
}
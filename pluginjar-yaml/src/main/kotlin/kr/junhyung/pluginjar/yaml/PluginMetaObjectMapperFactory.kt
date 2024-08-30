package kr.junhyung.pluginjar.yaml

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object PluginMetaObjectMapperFactory {
    fun create(): ObjectMapper {
        val factory = YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS)

        val kotlinModule = KotlinModule.Builder()
            .enable(KotlinFeature.NullToEmptyCollection)
            .enable(KotlinFeature.NullToEmptyMap)
            .enable(KotlinFeature.NullIsSameAsDefault)
            .enable(KotlinFeature.SingletonSupport)
            .enable(KotlinFeature.StrictNullChecks)
            .build()

        val mapper = ObjectMapper(factory)
            .registerModule(kotlinModule)
            .setPropertyNamingStrategy(PluginMetaPropertyNamingStrategy())
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)

        val visibility = mapper.serializationConfig
            .defaultVisibilityChecker
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)

        mapper.setVisibility(visibility)
        return mapper
    }
}
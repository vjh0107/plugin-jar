package kr.junhyung.pluginjar.yaml

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedField
import kr.junhyung.pluginjar.core.PluginMetaPropertyIntrospector

class PluginMetaPropertyNamingStrategy : PropertyNamingStrategy() {
    override fun nameForField(config: MapperConfig<*>, field: AnnotatedField, defaultName: String): String {
        return PluginMetaPropertyIntrospector.findName(field.annotated) ?: defaultName
    }
}
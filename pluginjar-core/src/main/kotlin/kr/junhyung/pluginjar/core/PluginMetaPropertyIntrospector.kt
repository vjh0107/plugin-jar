package kr.junhyung.pluginjar.core

import java.lang.reflect.Field

object PluginMetaPropertyIntrospector {
    fun findName(field: Field): String? {
        return field.getAnnotation(PluginMetaProperty::class.java)?.name
    }
}
package kr.junhyung.pluginjar.gradle.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

class GradlePropertyModule : SimpleModule() {

    init {
        addSerializer(Property::class.java, PropertySerializer())
        addSerializer(ListProperty::class.java, ListPropertySerializer())
    }

    private class PropertySerializer : ValueSerializer<Property<*>>() {
        override fun serialize(value: Property<*>, gen: JsonGenerator, ctxt: SerializationContext) {
            val unwrapped = value.orNull
            if (unwrapped != null) {
                val ser = ctxt.findValueSerializer(unwrapped.javaClass)
                ser.serialize(unwrapped, gen, ctxt)
            } else {
                gen.writeNull()
            }
        }

        override fun isEmpty(ctxt: SerializationContext, value: Property<*>): Boolean {
            return !value.isPresent
        }
    }

    private class ListPropertySerializer : ValueSerializer<ListProperty<*>>() {
        override fun serialize(value: ListProperty<*>, gen: JsonGenerator, ctxt: SerializationContext) {
            val list = value.orNull
            if (list != null && list.isNotEmpty()) {
                val ser = ctxt.findValueSerializer(list.javaClass)
                ser.serialize(list, gen, ctxt)
            } else {
                gen.writeNull()
            }
        }

        override fun isEmpty(ctxt: SerializationContext, value: ListProperty<*>): Boolean {
            return !value.isPresent || value.get().isEmpty()
        }
    }
}

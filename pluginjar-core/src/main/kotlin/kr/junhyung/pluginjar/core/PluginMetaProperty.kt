package kr.junhyung.pluginjar.core

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class PluginMetaProperty(val name: String)
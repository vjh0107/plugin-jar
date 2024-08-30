package kr.junhyung.pluginjar.core

@Suppress("SpellCheckingInspection")
class PluginMeta {

    @PluginMetaProperty("name")
    var name: String? = null

    @PluginMetaProperty("main")
    var main: String? = null

    @PluginMetaProperty("version")
    var version: String? = null

    @PluginMetaProperty("api-version")
    var apiVersion: String? = null

    @PluginMetaProperty("description")
    var description: String? = null

    @PluginMetaProperty("load")
    var load: Load? = null

    @PluginMetaProperty("author")
    var author: String? = null

    @PluginMetaProperty("authors")
    var authors: List<String> = listOf()

    @PluginMetaProperty("website")
    var website: String? = null

    @PluginMetaProperty("prefix")
    var prefix: String? = null

    @PluginMetaProperty("depend")
    var depend: List<String> = listOf()

    @PluginMetaProperty("softdepend")
    var softDepend: List<String> = listOf()

    @PluginMetaProperty("loadbefore")
    var loadBefore: List<String> = listOf()

    @PluginMetaProperty("libraries")
    var libraries: List<String> = listOf()

    @Suppress("unused")
    enum class Load {
        STARTUP,
        POSTWORLD
    }
}
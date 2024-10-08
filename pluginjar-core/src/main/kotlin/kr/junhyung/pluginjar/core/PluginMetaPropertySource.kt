package kr.junhyung.pluginjar.core

interface PluginMetaPropertySource {

    fun getName(): String

    fun findMain(): String?

    fun getVersion(): String

    fun findApiVersion(): String?

    fun findDescription(): String?

    fun findLoad(): PluginMeta.Load?

    fun findAuthor(): String?

    fun getAuthors(): List<String>

    fun findWebsite(): String?

    fun findPrefix(): String?

    fun getDepend(): List<String>

    fun getSoftDepend(): List<String>

    fun getLoadBefore(): List<String>

    fun getLibraries(): List<String>
}
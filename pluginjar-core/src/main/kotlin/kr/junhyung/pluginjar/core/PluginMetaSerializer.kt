package kr.junhyung.pluginjar.core

import java.io.File

interface PluginMetaSerializer {
    fun serialize(pluginMeta: PluginMeta, output: File)
}
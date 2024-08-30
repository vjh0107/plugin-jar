package kr.junhyung.pluginjar.plugin.service

import java.io.File

interface PluginMetaService {
    fun generatePluginMetaFile(output: File)
}
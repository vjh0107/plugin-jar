package kr.junhyung.pluginjar.core.service

import java.io.File

interface PluginMetaService {
    fun generatePluginMetaFile(output: File)
}
package kr.junhyung.pluginjar.core

import java.io.File

interface PluginMainClassResolver {
    fun resolve(rootDirectory: File): PluginMainClass?
}
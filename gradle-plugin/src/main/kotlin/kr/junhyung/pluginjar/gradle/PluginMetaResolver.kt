package kr.junhyung.pluginjar.gradle

import java.io.File

interface PluginMetaResolver {

    fun resolve(classesDirs: Iterable<File>): PluginMeta?

}
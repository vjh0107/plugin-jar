package kr.junhyung.pluginjar.asm

interface PluginMainClassDescriptor {
    fun isMainClass(): Boolean

    fun getName(): String
}
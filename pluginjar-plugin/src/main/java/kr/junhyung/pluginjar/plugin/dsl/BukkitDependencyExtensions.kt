package kr.junhyung.pluginjar.plugin.dsl

import kr.junhyung.pluginjar.plugin.BukkitDependency
import kr.junhyung.pluginjar.plugin.BukkitVersion
import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.spigot(version: String): Any {
    return buildBukkitDependency(BukkitDependency.SPIGOT, version)
}

fun DependencyHandler.paper(version: String): Any {
    return buildBukkitDependency(BukkitDependency.PAPER, version)
}

fun DependencyHandler.folia(version: String): Any {
    return buildBukkitDependency(BukkitDependency.FOLIA, version)
}

private fun buildBukkitDependency(dependency: BukkitDependency, version: String): Any {
    val bukkitVersion = BukkitVersion.parse(version)

    dependency.version {
        prefer(bukkitVersion.getVersion())
    }
    return dependency
}
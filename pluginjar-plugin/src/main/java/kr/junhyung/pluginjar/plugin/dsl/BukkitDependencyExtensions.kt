package kr.junhyung.pluginjar.plugin.dsl

import kr.junhyung.pluginjar.plugin.BukkitDependency
import kr.junhyung.pluginjar.plugin.BukkitVersion
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.getByType

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
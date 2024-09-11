package kr.junhyung.pluginjar.plugin

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

@Suppress("unused")
enum class BukkitDependency(
    group: String,
    artifact: String
) : ExternalModuleDependency by DefaultExternalModuleDependency(group, artifact, "") {
    SPIGOT("org.spigotmc", "spigot-api"),
    PAPER("io.papermc.paper", "paper-api"),
    FOLIA("dev.folia", "folia-api");
}
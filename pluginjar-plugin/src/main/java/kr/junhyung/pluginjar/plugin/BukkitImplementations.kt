package kr.junhyung.pluginjar.plugin

@Suppress("unused")
enum class BukkitImplementations(val group: String, val artifact: String) {
    SPIGOT("org.spigotmc", "spigot-api"),
    PAPER("io.papermc.paper", "paper-api"),
    FOLIA("dev.folia", "folia-api")
}
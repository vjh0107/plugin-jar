package kr.junhyung.pluginjar.plugin

import org.gradle.api.JavaVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BukkitVersionTests {
    @DisplayName("Bukkit versions 1.20.4 or lower are compatible with Java 17+")
    @Test
    fun bukkit_version_compatibility_test1() {
        val bukkitVersion17 = BukkitVersion.parse("1.17.1")
        assert(!bukkitVersion17.isCompatibleWith(JavaVersion.VERSION_16))
        assert(bukkitVersion17.isCompatibleWith(JavaVersion.VERSION_17))
        assert(bukkitVersion17.isCompatibleWith(JavaVersion.VERSION_21))

        val bukkitVersion20dot4 = BukkitVersion.parse("1.20.4")
        assert(!bukkitVersion20dot4.isCompatibleWith(JavaVersion.VERSION_16))
        assert(bukkitVersion20dot4.isCompatibleWith(JavaVersion.VERSION_17))
        assert(bukkitVersion20dot4.isCompatibleWith(JavaVersion.VERSION_21))
    }

    @DisplayName("Bukkit versions 1.20.5 or higher are compatible with Java 21+")
    @Test
    fun bukkit_version_compatibility_test2() {
        val bukkitVersion20dot5 = BukkitVersion.parse("1.20.5")
        assert(!bukkitVersion20dot5.isCompatibleWith(JavaVersion.VERSION_16))
        assert(!bukkitVersion20dot5.isCompatibleWith(JavaVersion.VERSION_17))
        assert(bukkitVersion20dot5.isCompatibleWith(JavaVersion.VERSION_21))

        val bukkitVersion21 = BukkitVersion.parse("1.21")
        assert(!bukkitVersion21.isCompatibleWith(JavaVersion.VERSION_16))
        assert(!bukkitVersion21.isCompatibleWith(JavaVersion.VERSION_17))
        assert(bukkitVersion21.isCompatibleWith(JavaVersion.VERSION_21))
    }
}
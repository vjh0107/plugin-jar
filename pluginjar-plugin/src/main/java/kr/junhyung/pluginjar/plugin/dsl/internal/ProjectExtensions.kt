package kr.junhyung.pluginjar.plugin.dsl.internal

import org.gradle.api.artifacts.ConfigurationContainer

internal val ConfigurationContainer.runtimeClasspath get() = getByName("runtimeClasspath")

internal val ConfigurationContainer.compileClasspath get() = getByName("compileClasspath")

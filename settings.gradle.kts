include("pluginjar-plugin")
include("pluginjar-asm")
include("pluginjar-annotations")
include("pluginjar-core")
include("pluginjar-yaml")

includeBuild("build-logic")

pluginManagement {
    plugins {
        kotlin("jvm") version embeddedKotlinVersion apply false
        kotlin("kapt") version embeddedKotlinVersion apply false
    }

    repositories {
        maven("https://junhyung.nexus/")
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        maven("https://junhyung.nexus/")
    }

    versionCatalogs.create("libs")
}

rootProject.name = "pluginjar"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        maven("https://junhyung.nexus/")
    }
}
includeBuild("build-logic")

include(":core")
include(":annotations")

include(":gradle-plugin")

include(":paper-loader")
include(":velocity-loader")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        maven("https://junhyung.nexus/")
    }
}

rootProject.name = "plugin-jar"
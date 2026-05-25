pluginManagement {
    repositories {
        maven("https://junhyung.nexus/")
    }
}
includeBuild("build-logic")

fun includeProject(name: String, path: String) {
    include(name)
    project(":$name").projectDir = file(path)
}

includeProject("plugin-jar-core", "core")
includeProject("plugin-jar-gradle-plugin", "gradle-plugin")
includeProject("plugin-jar-annotations", "annotations")
includeProject("plugin-jar-paper-plugin-loader", "paper-plugin-loader")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        maven("https://junhyung.nexus/")
    }
}

rootProject.name = "plugin-jar"
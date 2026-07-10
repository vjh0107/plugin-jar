package kr.junhyung.pluginjar.gradle

object PluginJarArtifacts {

    val group: String by lazy {
        PluginJarArtifacts::class.java.`package`.implementationVendor
            ?: System.getProperty("pluginjar.group")
            ?: error("Could not locate plugin group")
    }

    val version: String by lazy {
        PluginJarArtifacts::class.java.`package`.implementationVersion
            ?: System.getProperty("pluginjar.version")
            ?: error("Could not locate plugin version")
    }

    val annotations: String get() = notation("annotations")

    val paperLoader: String get() = notation("paper-loader")

    val velocityLoader: String get() = notation("velocity-loader")

    private fun notation(artifact: String): String = "$group:$artifact:$version"
}

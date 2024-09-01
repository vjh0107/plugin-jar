plugins {
    `kotlin-dsl`
    alias(libs.plugins.gradle.publish)
    id("pluginjar.publish")
}

dependencies {
    implementation(projects.pluginjarCore)
    runtimeOnly(projects.pluginjarAsm)
    runtimeOnly(projects.pluginjarYaml)
}

gradlePlugin {
    plugins {
        register("pluginjar") {
            id = "kr.junhyung.pluginjar"
            implementationClass = "kr.junhyung.pluginjar.plugin.PluginJarPlugin"
        }
    }
}
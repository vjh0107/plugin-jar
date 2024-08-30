plugins {
    id("pluginjar.auto-service")
}

dependencies {
    implementation(projects.pluginjar.pluginjarAnnotations)
    implementation(projects.pluginjar.pluginjarCore)

    implementation(libs.spring.core)
}
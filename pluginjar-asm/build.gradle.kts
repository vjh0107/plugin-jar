plugins {
    id("pluginjar.auto-service")
    id("pluginjar.publish")
}

dependencies {
    implementation(projects.pluginjar.pluginjarAnnotations)
    implementation(projects.pluginjar.pluginjarCore)

    implementation(libs.spring.core)
}
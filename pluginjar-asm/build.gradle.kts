plugins {
    id("pluginjar.auto-service")
    id("pluginjar.publish")
}

dependencies {
    implementation(projects.pluginjar.pluginjarAnnotation)
    implementation(projects.pluginjar.pluginjarCore)

    implementation(libs.spring.core)
}
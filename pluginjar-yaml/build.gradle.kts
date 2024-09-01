plugins {
    id("pluginjar.auto-service")
    id("pluginjar.publish")
}

dependencies {
    implementation(projects.pluginjarCore)

    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
}
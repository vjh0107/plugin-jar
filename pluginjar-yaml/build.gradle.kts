plugins {
    id("pluginjar.auto-service")
}

dependencies {
    implementation(projects.pluginjarCore)

    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
}
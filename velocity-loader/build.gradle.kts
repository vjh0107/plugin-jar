plugins {
    id("pluginjar.java-library")
}

dependencies {
    compileOnly(libs.velocity.api)

    implementation(project(":core"))
}

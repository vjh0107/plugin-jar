plugins {
    id("pluginjar.java-library")
}

dependencies {
    implementation(project(":plugin-jar-core"))
    compileOnly(libs.velocity.api)
}

plugins {
    id("pluginjar.java-library")
}

dependencies {
    implementation(project(":plugin-jar-core"))
    compileOnly(libs.paper.api)
}

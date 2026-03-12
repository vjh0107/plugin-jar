plugins {
    `java-library`
    id("pluginjar.publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

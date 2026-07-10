plugins {
    java
    id("kr.junhyung.plugin-jar.paper")
}

group = "com.example"
version = "1.0.0"
description = "Test Paper Plugin"

repositories {
    maven(uri(providers.gradleProperty("functionalTestRepositoryPath").get()))
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

paperPlugin {
    author.set("TestAuthor")
}

pluginImage {
    targetImage.set("test-paper-plugin:latest")
}

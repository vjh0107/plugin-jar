plugins {
    java
    id("kr.junhyung.plugin-jar")
}

group = "com.example"
version = "1.0.0"
description = "Test Velocity Plugin"

repositories {
    maven(uri(providers.gradleProperty("functionalTestRepositoryPath").get()))
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}

velocityPlugin {
    name.set("TestVelocityPlugin")
    authors.add("TestAuthor")
}

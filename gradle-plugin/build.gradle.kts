plugins {
    `kotlin-dsl`
    id("pluginjar.publish")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

gradlePlugin {
    website.set(providers.gradleProperty("project.url"))
    vcsUrl.set(providers.gradleProperty("project.scm.url"))

    plugins {
        register("plugin-jar") {
            id = "kr.junhyung.plugin-jar"
            implementationClass = "kr.junhyung.pluginjar.gradle.PluginJarPlugin"
            displayName = "Plugin Jar"
            description = "Streamline your Gradle plugin JAR builds for Minecraft plugins."
            tags = listOf("paper", "minecraft")
        }
        register("plugin-jar-nested") {
            id = "kr.junhyung.plugin-jar.nested"
            implementationClass = "kr.junhyung.pluginjar.gradle.nested.NestedPluginJarPlugin"
            displayName = "plugin-jar (nested)"
            description = "Paper nested plugin jar."
            tags = listOf("paper", "nested-jar")
        }
        register("plugin-jar-image") {
            id = "kr.junhyung.plugin-jar.image"
            implementationClass = "kr.junhyung.pluginjar.gradle.image.PluginImageBuildPlugin"
            displayName = "plugin-jar (image)"
            description = "Paper plugin OCI image via jib."
            tags = listOf("paper", "oci")
        }
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation(project(":plugin-jar-paper-plugin-loader"))
                implementation(libs.paper.api) {
                    isTransitive = false
                }
            }
        }
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])
}

@Suppress("UnstableApiUsage")
tasks.named("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

val functionalTestRepositoryDir = rootProject.layout.buildDirectory.dir("functional-test-repository")

val publishToFunctionalTestRepository by tasks.registering {
    group = "publishing"
    description = "Publishes all required modules to functional test repository"
    dependsOn(
        ":plugin-jar-annotations:publishMavenPublicationToFunctionalTestRepository",
        ":plugin-jar-paper-plugin-loader:publishMavenPublicationToFunctionalTestRepository"
    )
}

tasks.named<Test>("functionalTest") {
    dependsOn(publishToFunctionalTestRepository)
    systemProperty("functionalTestRepositoryPath", functionalTestRepositoryDir.get().asFile.absolutePath)
    systemProperty("pluginjar.version", project.version.toString())
    systemProperty("pluginjar.group", project.group.toString())
}

dependencies {
    implementation(project(":plugin-jar-annotations"))
    compileOnly(project(":plugin-jar-paper-plugin-loader"))
    implementation(libs.asm)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jib.core)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to rootProject.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.group,
        )
    }
}

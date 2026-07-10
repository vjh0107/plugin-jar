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
        register("plugin-jar-nested") {
            id = "kr.junhyung.plugin-jar.nested"
            implementationClass = "kr.junhyung.pluginjar.gradle.nested.NestedPluginJarPlugin"
            displayName = "plugin-jar (nested)"
            description = "Nested plugin jar bundling runtime libraries."
            tags = listOf("minecraft", "nested-jar")
        }
        register("plugin-jar-image") {
            id = "kr.junhyung.plugin-jar.image"
            implementationClass = "kr.junhyung.pluginjar.gradle.image.PluginImageBuildPlugin"
            displayName = "plugin-jar (image)"
            description = "Plugin OCI image via jib."
            tags = listOf("minecraft", "oci")
        }
        register("plugin-jar-paper") {
            id = "kr.junhyung.plugin-jar.paper"
            implementationClass = "kr.junhyung.pluginjar.gradle.paper.PaperPluginJarPlugin"
            displayName = "plugin-jar (paper)"
            description = "Streamline your Gradle plugin JAR builds for Paper plugins."
            tags = listOf("paper", "minecraft")
        }
        register("plugin-jar-manifest") {
            id = "kr.junhyung.plugin-jar.manifest"
            implementationClass = "kr.junhyung.pluginjar.gradle.paper.PaperPluginManifestPlugin"
            displayName = "plugin-jar (manifest)"
            description = "Paper plugin manifest (paper-plugin.yml) generation."
            tags = listOf("paper", "manifest")
        }
        register("plugin-jar-velocity") {
            id = "kr.junhyung.plugin-jar.velocity"
            implementationClass = "kr.junhyung.pluginjar.gradle.velocity.VelocityPluginJarPlugin"
            displayName = "plugin-jar (velocity)"
            description = "Streamline your Gradle plugin JAR builds for Velocity plugins."
            tags = listOf("velocity", "minecraft")
        }
        register("plugin-jar-velocity-manifest") {
            id = "kr.junhyung.plugin-jar.velocity.manifest"
            implementationClass = "kr.junhyung.pluginjar.gradle.velocity.VelocityPluginManifestPlugin"
            displayName = "plugin-jar (velocity manifest)"
            description = "Velocity plugin manifest (velocity-plugin.json) generation."
            tags = listOf("velocity", "manifest")
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
                implementation(project(":paper-loader"))
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
        ":annotations:publishMavenPublicationToFunctionalTestRepository",
        ":core:publishMavenPublicationToFunctionalTestRepository",
        ":paper-loader:publishMavenPublicationToFunctionalTestRepository",
        ":velocity-loader:publishMavenPublicationToFunctionalTestRepository",
    )
}

tasks.named<Test>("functionalTest") {
    dependsOn(publishToFunctionalTestRepository)
    systemProperty("functionalTestRepositoryPath", functionalTestRepositoryDir.get().asFile.absolutePath)
    systemProperty("pluginjar.version", project.version.toString())
    systemProperty("pluginjar.group", project.group.toString())
}

dependencies {
    implementation(project(":annotations"))
    compileOnly(project(":core"))
    implementation(libs.asm)
    implementation(libs.jib.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
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

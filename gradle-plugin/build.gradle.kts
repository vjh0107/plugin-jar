plugins {
    `kotlin-dsl`
    id("pluginjar.publish")
}

gradlePlugin {
    plugins {
        register("plugin-jar") {
            id = "kr.junhyung.plugin-jar"
            implementationClass = "kr.junhyung.pluginjar.gradle.PluginJarPlugin"
        }
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
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
tasks.check {
    dependsOn(testing.suites.named("functionalTest"))
}

val functionalTestRepositoryDir = rootProject.layout.buildDirectory.dir("functional-test-repository")

val publishToFunctionalTestRepository by tasks.registering {
    group = "publishing"
    description = "Publishes all required modules to functional test repository"
    dependsOn(
        ":plugin-jar-annotations:publishMavenPublicationToFunctionalTestRepository",
        ":plugin-jar-core:publishMavenPublicationToFunctionalTestRepository",
        ":plugin-jar-paper:publishMavenPublicationToFunctionalTestRepository",
        ":plugin-jar-velocity:publishMavenPublicationToFunctionalTestRepository"
    )
}

tasks.named<Test>("functionalTest") {
    dependsOn(publishToFunctionalTestRepository)
    systemProperty("functionalTestRepositoryPath", functionalTestRepositoryDir.get().asFile.absolutePath)

    // Gradle Plugin 으로 동작할 때는, Manifest의 Implementation-Vendor에서 그룹을, Implementation-Version 에서 버전을 읽어오지만,
    // FunctionalTest 에서는 jar에 Manifest가 패키징 되지 않기 때문에 추가한다.
    systemProperty("pluginjar.version", project.version.toString())
    systemProperty("pluginjar.group", project.group.toString())
}

dependencies {
    implementation(project(":plugin-jar-annotations"))
    implementation(project(":plugin-jar-core"))
    implementation(libs.spring.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to rootProject.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.group
        )
    }
}

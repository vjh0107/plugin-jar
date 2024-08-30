import java.time.LocalDateTime

plugins {
    java
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    group = "kr.junhyung"
    version = "1.0.0"

    apply<JavaPlugin>()
    apply<MavenPublishPlugin>()
    pluginManager.apply(rootProject.libs.plugins.kotlin.jvm.get().pluginId)

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                group = project.group
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])
            }
        }

        repositories {
            maven("https://nexus.junhyung.kr/repository/maven-releases/") {
                credentials {
                    username = System.getenv("NEXUS_USERNAME")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
        }
    }
    tasks.withType<Jar> {
        manifest {
            attributes(
                "Implementation-Title" to rootProject.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Junhyung",
                "Implementation-Timestamp" to LocalDateTime.now()
            )
        }
    }
}
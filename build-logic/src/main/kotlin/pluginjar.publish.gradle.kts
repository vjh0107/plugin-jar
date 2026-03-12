plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "nexus"
            url = uri(
                if (version.toString().endsWith("-SNAPSHOT")) {
                    "https://nexus.junhyung.kr/repository/maven-snapshots/"
                } else {
                    "https://nexus.junhyung.kr/repository/maven-releases/"
                }
            )
            credentials {
                username = findProperty("nexus.username") as String? ?: System.getenv("NEXUS_USERNAME")
                password = findProperty("nexus.password") as String? ?: System.getenv("NEXUS_PASSWORD")
            }
        }
        maven {
            name = "functionalTest"
            url = uri(rootProject.layout.buildDirectory.dir("functional-test-repository"))
        }
    }

    publications.withType<MavenPublication> {
        pom {
            name.set(project.name)
            description.set(project.description)
            url.set(findProperty("project.url") as String?)

            licenses {
                license {
                    name.set(findProperty("project.license") as String?)
                    url.set(findProperty("project.license.url") as String?)
                }
            }

            developers {
                developer {
                    id.set(findProperty("project.developer.id") as String?)
                    name.set(findProperty("project.developer.name") as String?)
                    email.set(findProperty("project.developer.email") as String?)
                }
            }

            scm {
                url.set(findProperty("project.scm.url") as String?)
            }
        }
    }
}

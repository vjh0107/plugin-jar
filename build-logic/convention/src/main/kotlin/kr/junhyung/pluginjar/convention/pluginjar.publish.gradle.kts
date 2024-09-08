apply(plugin = "maven-publish")

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("java") {
            group = project.group
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            setupPom(pom)
        }
    }
    repositories {
        maven {
            if (!isSnapshotVersion(project.version.toString())) {
                name = "MavenCentral"
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }
}

configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

if (!isSnapshotVersion(project.version.toString())) {
    apply(plugin = "signing")
    configure<SigningExtension> {
        if (!extra.has("signing.keyId")) {
            extra["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
        }
        if (!extra.has("signing.password")) {
            extra["signing.password"] = System.getenv("SIGNING_PASSPHRASE")
        }
        if (!extra.has("signing.secretKeyRingFile")) {
            extra["signing.secretKeyRingFile"] =
                if (System.getenv("SIGNING_SECRET_KEY_RING_FILE_ABSOLUTE") != null) {
                    System.getenv("SIGNING_SECRET_KEY_RING_FILE_ABSOLUTE")
                } else {
                    System.getenv("HOME") + "/" + System.getenv("SIGNING_SECRET_KEY_RING_FILE")
                }
        }
        sign(extensions.getByType(PublishingExtension::class).publications)
    }
    tasks.withType(AbstractPublishToMaven::class.java) {
        dependsOn(tasks.withType<Sign>())
    }
}


tasks.withType(GenerateMavenPom::class.java) {
    doFirst {
        setupPom(pom)
    }
}

fun setupPom(pom: MavenPom) {
    with (pom) {
        val projectUrl = property("project.url").toString()
        val projectDescription = property("project.description").toString()
        val projectLicense = property("project.license").toString()
        val projectLicenseUrl = property("project.license.url").toString()
        val projectDeveloperId = property("project.developer.id").toString()
        val projectDeveloperName = property("project.developer.name").toString()
        val projectDeveloperEmail = property("project.developer.email").toString()
        val projectUrlScm = property("project.url.scm").toString()

        name.set(project.rootProject.name)
        url.set(projectUrl)
        description.set(projectDescription)

        licenses {
            license {
                name.set(projectLicense)
                url.set(projectLicenseUrl)
            }
        }
        developers {
            developer {
                id.set(projectDeveloperId)
                name.set(projectDeveloperName)
                email.set(projectDeveloperEmail)
            }
            scm {
                url.set(projectUrlScm)
            }
        }
    }
}

fun isSnapshotVersion(version: String): Boolean {
    return version.endsWith("-SNAPSHOT")
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime

plugins {
    java
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    group = property("project.group").toString()
    version = property("project.version").toString()

    with(pluginManager) {
        apply<JavaPlugin>()
        apply(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = JavaVersion.VERSION_17.majorVersion
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
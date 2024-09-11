import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            freeCompilerArgs.add("-Xcontext-receivers")
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val developerName = property("project.developer.name")
    tasks.withType<Jar> {
        manifest {
            attributes(
                "Implementation-Title" to rootProject.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to developerName,
                "Implementation-Timestamp" to LocalDateTime.now()
            )
        }
    }
}
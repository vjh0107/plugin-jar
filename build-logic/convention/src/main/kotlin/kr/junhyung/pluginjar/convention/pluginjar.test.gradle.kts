import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType

apply<JavaPlugin>()

dependencies {
    "testImplementation"(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
package kr.junhyung.pluginjar.gradle.velocity

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VelocityPluginJarPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    private val functionalTestRepositoryPath: String
        get() = System.getProperty("functionalTestRepositoryPath")
            ?: error("functionalTestRepositoryPath system property is not set")

    @BeforeEach
    fun setup() {
        val fixtureDir = File(javaClass.getResource("/fixtures/velocity-plugin")!!.toURI())
        fixtureDir.copyRecursively(testProjectDir)
        writeBuildScript(
            """
            velocityPlugin {
                name.set("TestVelocity")
                authors.add("TestAuthor")
                dependencies.create("luckperms") {
                    optional.set(true)
                }
            }
            """.trimIndent()
        )
    }

    private fun writeBuildScript(velocityPluginBlock: String) {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                java
                id("kr.junhyung.plugin-jar.velocity")
            }

            group = "com.example"
            version = "1.0.0"

            repositories {
                maven(uri(providers.gradleProperty("functionalTestRepositoryPath").get()))
                mavenCentral()
            }

            $velocityPluginBlock

            pluginImage {
                targetImage.set("test-velocity-plugin:latest")
            }
            """.trimIndent()
        )
    }

    private fun gradleRunner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments, "-PfunctionalTestRepositoryPath=$functionalTestRepositoryPath", "--stacktrace", "--configuration-cache")
            .withPluginClasspath()

    @Test
    @DisplayName("aggregator가 manifest/nested/image 태스크를 모두 등록한다")
    fun `aggregator registers manifest nested and image tasks`() {
        val result = gradleRunner("tasks").build()

        assertTrue(result.output.contains("resolveMainClass"))
        assertTrue(result.output.contains("generateVelocityPluginJson"))
        assertTrue(result.output.contains("pluginJar"))
        assertTrue(result.output.contains("pluginImageTar"))
    }

    @Test
    @DisplayName("velocity-plugin.json은 marker로 찾은 메인클래스와 확장 설정을 직렬화한다")
    fun `velocity plugin json serializes main class and extension`() {
        val result = gradleRunner("generateVelocityPluginJson").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateVelocityPluginJson")?.outcome)

        val json = File(testProjectDir, "build/generated/pluginjar/velocity-plugin.json")
        assertTrue(json.exists(), "velocity-plugin.json should exist")

        val compact = json.readText().replace(Regex("\\s"), "")
        assertTrue(compact.contains("\"main\":\"com.example.TestVelocityPlugin\""), "main from @PluginMarker")
        assertTrue(compact.contains("\"id\":\"testvelocityplugin\""), "id from @PluginMarker")
        assertTrue(compact.contains("\"name\":\"TestVelocity\""), "name from extension")
        assertTrue(compact.contains("\"version\":\"1.0.0\""), "version defaults to project version")
        assertTrue(compact.contains("\"authors\":[\"TestAuthor\"]"), "authors entry")
        assertTrue(compact.contains("\"id\":\"luckperms\",\"optional\":true"), "dependency id and optional flag")
        assertFalse(compact.contains("\"url\""), "empty url should be omitted")
    }

    @Test
    @DisplayName("pluginJar는 velocity-plugin.json과 로더 클래스를 포함한 nested jar를 만든다")
    fun `pluginJar bundles manifest and loader classes`() {
        val result = gradleRunner("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-velocity-plugin-1.0.0-plugin.jar")
        assertTrue(pluginJar.exists(), "plugin jar should exist")

        java.util.jar.JarFile(pluginJar).use { jar ->
            val entries = jar.entries().toList().map { it.name }
            assertTrue(entries.any { it == "velocity-plugin.json" }, "should contain velocity-plugin.json")
            assertTrue(entries.any { it == "com/example/TestVelocityPlugin.class" }, "should contain plugin class")
            assertTrue(
                entries.any { it.startsWith("kr/junhyung/pluginjar/velocity/") },
                "should bundle velocity-loader classes at root",
            )
        }
    }

    @Test
    @DisplayName("pluginImageTar가 scratch base로 tar 이미지를 생성한다")
    fun `pluginImageTar produces a tar image`() {
        val result = gradleRunner("pluginImageTar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageTar")?.outcome)

        val tar = File(testProjectDir, "build/pluginjar/test-velocity-plugin.tar")
        assertTrue(tar.exists(), "image tar should exist")
    }

    @Test
    @DisplayName("velocity id 패턴에 어긋나면 빌드가 실패한다")
    fun `invalid id fails the build`() {
        writeBuildScript(
            """
            velocityPlugin {
                id.set("Invalid_ID")
            }
            """.trimIndent()
        )

        val result = gradleRunner("generateVelocityPluginJson").buildAndFail()

        assertTrue(
            result.output.contains("is invalid"),
            "should report an invalid velocity plugin id",
        )
    }
}

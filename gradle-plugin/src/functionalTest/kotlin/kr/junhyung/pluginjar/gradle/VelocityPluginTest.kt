package kr.junhyung.pluginjar.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VelocityPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    private val functionalTestRepositoryPath: String
        get() = System.getProperty("functionalTestRepositoryPath")
            ?: error("functionalTestRepositoryPath system property is not set")

    @BeforeEach
    fun setup() {
        val fixtureDir = File(javaClass.getResource("/fixtures/velocity-plugin")!!.toURI())
        fixtureDir.copyRecursively(testProjectDir)
    }

    private fun gradleRunner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments, "-PfunctionalTestRepositoryPath=$functionalTestRepositoryPath", "--stacktrace")
            .withPluginClasspath()

    @Test
    @DisplayName("플러그인이 정상적으로 적용된다")
    fun `plugin applies successfully`() {
        val result = gradleRunner("tasks").build()

        assertTrue(result.output.contains("resolvePluginMarker"))
        assertTrue(result.output.contains("generateVelocityPluginJson"))
        assertTrue(result.output.contains("pluginJar"))
    }

    @Test
    @DisplayName("generateVelocityPluginJson 태스크가 유효한 JSON을 생성한다")
    fun `generateVelocityPluginJson creates valid json`() {
        val result = gradleRunner("generateVelocityPluginJson").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateVelocityPluginJson")?.outcome)

        val pluginJson = File(testProjectDir, "build/pluginjar/velocity-plugin.json")
        assertTrue(pluginJson.exists(), "velocity-plugin.json should exist")

        val content = pluginJson.readText()
        assertTrue(content.contains("\"name\""), "Should contain name")
        assertTrue(content.contains("testvelocityplugin"), "Should contain plugin name")
        assertTrue(content.contains("\"main\""), "Should contain main")
        assertTrue(content.contains("\"id\""), "Should contain id")
    }

    @Test
    @DisplayName("build 태스크가 성공한다")
    fun `build task succeeds`() {
        val result = gradleRunner("build").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

        val jarFile = File(testProjectDir, "build/libs/test-velocity-plugin-1.0.0.jar")
        assertTrue(jarFile.exists(), "jar file should exist")
    }

    @Test
    @DisplayName("pluginJar 태스크가 의존성을 포함한 JAR을 생성한다")
    fun `pluginJar task creates fat jar with dependencies`() {
        val result = gradleRunner("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-velocity-plugin-1.0.0-plugin.jar")
        assertTrue(pluginJar.exists(), "plugin jar should exist")

        val jarFile = java.util.jar.JarFile(pluginJar)
        val entries = jarFile.entries().toList().map { it.name }

        assertTrue(entries.any { it == "velocity-plugin.json" }, "Should contain velocity-plugin.json")
        assertTrue(entries.any { it.startsWith("com/example/") }, "Should contain plugin classes")

        jarFile.close()
    }

    @Test
    @DisplayName("pluginJar 라이브러리가 nested jar가 아닌 클래스로 포함된다")
    fun `pluginJar includes pluginjar libraries as classes not in BOOT-INF lib`() {
        val result = gradleRunner("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-velocity-plugin-1.0.0-plugin.jar")
        val jarFile = java.util.jar.JarFile(pluginJar)
        val entries = jarFile.entries().toList().map { it.name }

        assertTrue(
            entries.any { it.startsWith("kr/junhyung/pluginjar/core/") },
            "Should contain plugin-jar-core classes at root"
        )
        assertTrue(
            entries.any { it.startsWith("kr/junhyung/pluginjar/velocity/") },
            "Should contain plugin-jar-velocity classes at root"
        )

        assertTrue(
            entries.none { it.startsWith("BOOT-INF/lib/") && it.contains("plugin-jar-") },
            "plugin-jar libraries should NOT be in BOOT-INF/lib/"
        )

        jarFile.close()
    }

    @Test
    @DisplayName("Velocity 프로젝트의 runtimeClasspath에 plugin-jar-velocity만 포함된다")
    fun `velocity project should only have plugin-jar-velocity in runtimeClasspath`() {
        val result = gradleRunner("dependencies", "--configuration", "runtimeClasspath").build()

        val output = result.output
        assertTrue(output.contains("plugin-jar-velocity"), "Should contain plugin-jar-velocity")
        assertTrue(!output.contains("plugin-jar-paper"), "Should NOT contain plugin-jar-paper")
    }

    @Test
    @DisplayName("Velocity 프로젝트에는 generateVelocityPluginJson만 등록되고 generatePaperPluginYml은 등록되지 않는다")
    fun `velocity project registers only generateVelocityPluginJson task`() {
        val result = gradleRunner("tasks", "--all").build()

        assertTrue(result.output.contains("generateVelocityPluginJson"), "Should contain generateVelocityPluginJson")
        assertTrue(!result.output.contains("generatePaperPluginYml"), "Should NOT contain generatePaperPluginYml")
    }
}

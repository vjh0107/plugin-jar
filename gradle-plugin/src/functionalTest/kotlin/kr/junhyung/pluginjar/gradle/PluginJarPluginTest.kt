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

class PluginJarPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    private val functionalTestRepositoryPath: String
        get() = System.getProperty("functionalTestRepositoryPath")
            ?: error("functionalTestRepositoryPath system property is not set")

    @BeforeEach
    fun setup() {
        val fixtureDir = File(javaClass.getResource("/fixtures/paper-plugin")!!.toURI())
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
        assertTrue(result.output.contains("generatePaperPluginYml"))
        assertTrue(result.output.contains("pluginJar"))
    }

    @Test
    @DisplayName("generatePaperPluginYml 태스크가 유효한 YAML을 생성한다")
    fun `generatePaperPluginYml creates valid yaml`() {
        val result = gradleRunner("generatePaperPluginYml").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePaperPluginYml")?.outcome)

        val pluginYml = File(testProjectDir, "build/pluginjar/paper-plugin.yml")
        assertTrue(pluginYml.exists(), "paper-plugin.yml should exist")

        val content = pluginYml.readText()
        assertTrue(content.contains("name:"), "Should contain name")
        assertTrue(content.contains("TestPaperPlugin"), "Should contain plugin name")
        assertTrue(content.contains("main:"), "Should contain main")
        assertTrue(content.contains("api-version:"), "Should contain api-version")
        assertTrue(content.contains("loader:"), "Should contain loader")
    }

    @Test
    @DisplayName("build 태스크가 성공한다")
    fun `build task succeeds`() {
        val result = gradleRunner("build").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

        val jarFile = File(testProjectDir, "build/libs/test-paper-plugin-1.0.0.jar")
        assertTrue(jarFile.exists(), "jar file should exist")
    }

    @Test
    @DisplayName("pluginJar 태스크가 의존성을 포함한 JAR을 생성한다")
    fun `pluginJar task creates fat jar with dependencies`() {
        val result = gradleRunner("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-paper-plugin-1.0.0-plugin.jar")
        assertTrue(pluginJar.exists(), "plugin jar should exist")

        val jarFile = java.util.jar.JarFile(pluginJar)
        val entries = jarFile.entries().toList().map { it.name }

        assertTrue(entries.any { it == "paper-plugin.yml" }, "Should contain paper-plugin.yml")
        assertTrue(entries.any { it.startsWith("com/example/") }, "Should contain plugin classes")

        jarFile.close()
    }

    @Test
    @DisplayName("pluginJar 라이브러리가 nested jar가 아닌 클래스로 포함된다")
    fun `pluginJar includes pluginjar libraries as classes not in BOOT-INF lib`() {
        val result = gradleRunner("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-paper-plugin-1.0.0-plugin.jar")
        val jarFile = java.util.jar.JarFile(pluginJar)
        val entries = jarFile.entries().toList().map { it.name }

        assertTrue(
            entries.any { it.startsWith("kr/junhyung/pluginjar/core/") },
            "Should contain plugin-jar-core classes at root"
        )
        assertTrue(
            entries.any { it.startsWith("kr/junhyung/pluginjar/paper/") },
            "Should contain plugin-jar-paper classes at root"
        )

        assertTrue(
            entries.none { it.startsWith("BOOT-INF/lib/") && it.contains("plugin-jar-") },
            "plugin-jar libraries should NOT be in BOOT-INF/lib/"
        )

        jarFile.close()
    }

    @Test
    @DisplayName("Paper 프로젝트의 runtimeClasspath에 plugin-jar-paper만 포함된다")
    fun `paper project should only have plugin-jar-paper in runtimeClasspath`() {
        val result = gradleRunner("dependencies", "--configuration", "runtimeClasspath").build()

        val output = result.output
        assertTrue(output.contains("plugin-jar-paper"), "Should contain plugin-jar-paper")
        assertTrue(!output.contains("plugin-jar-velocity"), "Should NOT contain plugin-jar-velocity")
    }

    @Test
    @DisplayName("Paper 프로젝트에는 generatePaperPluginYml만 등록되고 generateVelocityPluginJson은 등록되지 않는다")
    fun `paper project registers only generatePaperPluginYml task`() {
        val result = gradleRunner("tasks", "--all").build()

        assertTrue(result.output.contains("generatePaperPluginYml"), "Should contain generatePaperPluginYml")
        assertTrue(!result.output.contains("generateVelocityPluginJson"), "Should NOT contain generateVelocityPluginJson")
    }
}

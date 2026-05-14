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
            .withArguments(*arguments, "-PfunctionalTestRepositoryPath=$functionalTestRepositoryPath", "--stacktrace", "--configuration-cache")
            .withPluginClasspath()

    private fun gradleRunnerWithoutConfigurationCache(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments, "-PfunctionalTestRepositoryPath=$functionalTestRepositoryPath", "--stacktrace")
            .withPluginClasspath()

    @Test
    @DisplayName("플러그인이 정상적으로 적용된다")
    fun `plugin applies successfully`() {
        val result = gradleRunner("tasks").build()

        assertTrue(result.output.contains("resolvePluginMarker"))
        assertTrue(result.output.contains("generateNestedJarPaperPluginYml"))
        assertTrue(result.output.contains("generateContainerImagePaperPluginYml"))
        assertTrue(result.output.contains("pluginJar"))
    }

    @Test
    @DisplayName("nested-jar paper-plugin.yml은 PluginJarPluginLoader를 가리킨다")
    fun `nested jar yaml pins loader to PluginJarPluginLoader`() {
        val result = gradleRunner("generateNestedJarPaperPluginYml").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateNestedJarPaperPluginYml")?.outcome)

        val pluginYml = File(testProjectDir, "build/pluginjar/nested-jar/paper-plugin.yml")
        assertTrue(pluginYml.exists(), "nested-jar paper-plugin.yml should exist")

        val content = pluginYml.readText()
        assertTrue(content.contains("TestPaperPlugin"), "should contain plugin name")
        assertTrue(content.contains("api-version:"), "should contain api-version")
        assertTrue(
            content.contains("kr.junhyung.pluginjar.paper.PluginJarPluginLoader"),
            "loader should be PluginJarPluginLoader",
        )
    }

    @Test
    @DisplayName("container-image paper-plugin.yml은 PluginJarPluginLoader를 가리킨다")
    fun `container image yaml pins loader to PluginJarPluginLoader`() {
        val result = gradleRunner("generateContainerImagePaperPluginYml").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateContainerImagePaperPluginYml")?.outcome)

        val pluginYml = File(testProjectDir, "build/pluginjar/container-image/paper-plugin.yml")
        assertTrue(pluginYml.exists(), "container-image paper-plugin.yml should exist")

        val content = pluginYml.readText()
        assertTrue(
            content.contains("kr.junhyung.pluginjar.paper.PluginJarPluginLoader"),
            "loader should be PluginJarPluginLoader",
        )
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
            entries.any { it.startsWith("kr/junhyung/pluginjar/paper/") },
            "Should contain plugin-jar-paper-plugin-loader classes at root"
        )

        assertTrue(
            entries.none { it.startsWith("BOOT-INF/lib/") && it.contains("plugin-jar-") },
            "plugin-jar libraries should NOT be in BOOT-INF/lib/"
        )

        jarFile.close()
    }

    @Test
    @DisplayName("runtimeClasspath에 plugin-jar-paper-plugin-loader가 포함된다")
    fun `runtimeClasspath contains plugin-jar-paper-plugin-loader`() {
        val result = gradleRunner("dependencies", "--configuration", "runtimeClasspath").build()

        assertTrue(
            result.output.contains("plugin-jar-paper-plugin-loader"),
            "Should contain plugin-jar-paper-plugin-loader"
        )
    }

    @Test
    @DisplayName("image bootstrap jar의 yml은 PluginJarPluginLoader를 가리키고 manifest는 image 타입이다")
    fun `bootstrap jar pins loader and tags manifest as image`() {
        val result = gradleRunner("pluginImageBootstrap").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageBootstrap")?.outcome)

        val bootstrapJar = File(testProjectDir, "build/docker-context/plugin/test-paper-plugin-1.0.0.jar")
        assertTrue(bootstrapJar.exists(), "bootstrap jar should exist")

        val jarFile = java.util.jar.JarFile(bootstrapJar)
        try {
            val ymlEntry = jarFile.getEntry("paper-plugin.yml")
            assertTrue(ymlEntry != null, "bootstrap jar should contain paper-plugin.yml")
            val ymlContent = jarFile.getInputStream(ymlEntry).bufferedReader().readText()
            assertTrue(
                ymlContent.contains("kr.junhyung.pluginjar.paper.PluginJarPluginLoader"),
                "bootstrap yml should pin loader to PluginJarPluginLoader",
            )
            assertEquals(
                "image",
                jarFile.manifest?.mainAttributes?.getValue("Plugin-Jar-Type"),
                "bootstrap manifest should declare Plugin-Jar-Type=image",
            )
        } finally {
            jarFile.close()
        }
    }

    @Test
    @DisplayName("pluginJar 결과물의 yml은 PluginJarPluginLoader를 가리키고 manifest는 nested 타입이다")
    fun `pluginJar archive pins loader and tags manifest as nested`() {
        val result = gradleRunner("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-paper-plugin-1.0.0-plugin.jar")
        val jarFile = java.util.jar.JarFile(pluginJar)
        try {
            val ymlEntry = jarFile.getEntry("paper-plugin.yml")
            assertTrue(ymlEntry != null, "nested jar should contain paper-plugin.yml")
            val ymlContent = jarFile.getInputStream(ymlEntry).bufferedReader().readText()
            assertTrue(
                ymlContent.contains("kr.junhyung.pluginjar.paper.PluginJarPluginLoader"),
                "nested jar yml should pin loader to PluginJarPluginLoader",
            )
            assertEquals(
                "nested",
                jarFile.manifest?.mainAttributes?.getValue("Plugin-Jar-Type"),
                "nested jar manifest should declare Plugin-Jar-Type=nested",
            )
        } finally {
            jarFile.close()
        }
    }

    @Test
    @DisplayName("image 모드의 main jar에는 paper-plugin.yml이 들어가지 않는다")
    fun `module main jar does not contain paper-plugin yml`() {
        val result = gradleRunner("pluginImageCollectModules").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageCollectModules")?.outcome)

        val mainJar = File(testProjectDir, "build/docker-context/modules/test-paper-plugin-1.0.0.jar")
        assertTrue(mainJar.exists(), "module jar should exist")

        val jarFile = java.util.jar.JarFile(mainJar)
        try {
            val entries = jarFile.entries().toList().map { it.name }
            assertTrue(
                entries.none { it == "paper-plugin.yml" },
                "module main jar should not contain paper-plugin.yml (it belongs to bootstrap)",
            )
        } finally {
            jarFile.close()
        }
    }

    @Test
    @DisplayName("pluginImageTar 태스크가 scratch base로 tar 이미지를 생성한다")
    fun `pluginImageTar produces a tar image from scratch base`() {
        val result = gradleRunner("pluginImageTar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageTar")?.outcome)

        val tar = File(testProjectDir, "build/pluginjar/test-paper-plugin.tar")
        assertTrue(tar.exists(), "image tar should exist")
    }

    @Test
    @DisplayName("연속 pluginImageTar 호출은 jib layer cache를 재사용한다")
    fun `consecutive pluginImageTar builds reuse jib layer cache`() {
        val first = gradleRunner("pluginImageTar").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":pluginImageTar")?.outcome)

        val applicationCache = File(testProjectDir, "build/pluginjar/jib-cache/application")
        assertTrue(applicationCache.isDirectory, "jib application cache directory should exist")

        val firstSnapshot = applicationCache.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(applicationCache).path to it.length() }
            .toSet()
        assertTrue(firstSnapshot.isNotEmpty(), "first build should populate the application cache")

        val second = gradleRunner("pluginImageTar").build()
        val secondOutcome = second.task(":pluginImageTar")?.outcome
        assertTrue(
            secondOutcome == TaskOutcome.SUCCESS || secondOutcome == TaskOutcome.UP_TO_DATE,
            "second invocation should either re-run hitting jib cache or skip as up-to-date (was $secondOutcome)",
        )

        val secondSnapshot = applicationCache.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(applicationCache).path to it.length() }
            .toSet()

        assertEquals(
            firstSnapshot,
            secondSnapshot,
            "no new layer files should be created on an identical second invocation",
        )
    }

    @Test
    @DisplayName("Configuration Cache 없이도 pluginJar 빌드가 성공한다")
    fun `pluginJar succeeds without configuration cache`() {
        val result = gradleRunnerWithoutConfigurationCache("pluginJar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginJar")?.outcome)

        val pluginJar = File(testProjectDir, "build/libs/test-paper-plugin-1.0.0-plugin.jar")
        assertTrue(pluginJar.exists(), "plugin jar should be produced even when CC is disabled")
    }

    @Test
    @DisplayName("Configuration Cache 없이도 pluginImageTar 빌드가 성공한다")
    fun `pluginImageTar succeeds without configuration cache`() {
        val result = gradleRunnerWithoutConfigurationCache("pluginImageTar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageTar")?.outcome)

        val tar = File(testProjectDir, "build/pluginjar/test-paper-plugin.tar")
        assertTrue(tar.exists(), "image tar should be produced even when CC is disabled")
    }

    @Test
    @DisplayName("kr.junhyung.plugin-jar.image 단독 적용만으로 pluginImageTar가 동작한다")
    fun `image sub-plugin works stand-alone without aggregator`() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                java
                id("kr.junhyung.plugin-jar.image")
            }

            group = "com.example"
            version = "1.0.0"
            description = "Test Paper Plugin"

            repositories {
                maven(uri(providers.gradleProperty("functionalTestRepositoryPath").get()))
                mavenCentral()
                maven("https://repo.papermc.io/repository/maven-public/")
            }

            dependencies {
                compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
            }

            paperPlugin {
                author.set("TestAuthor")
            }

            paperPluginImage {
                targetImage.set("test-paper-plugin:latest")
            }
            """.trimIndent()
        )

        val result = gradleRunner("pluginImageTar", "tasks").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageTar")?.outcome)
        assertTrue(
            result.output.contains("pluginImageTar"),
            "image task should be registered when applying the image sub-plugin alone",
        )
        assertTrue(
            !result.output.contains("pluginJar -"),
            "nested pluginJar task must not be registered when only the image sub-plugin is applied",
        )
    }
}

package kr.junhyung.pluginjar.gradle.paper

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

    private val pluginJarVersion: String
        get() = System.getProperty("pluginjar.version")
            ?: error("pluginjar.version system property is not set")

    private val pluginJarGroup: String
        get() = System.getProperty("pluginjar.group")
            ?: error("pluginjar.group system property is not set")

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

        assertTrue(result.output.contains("resolveMainClass"))
        assertTrue(result.output.contains("generatePaperPluginYml"))
        assertTrue(result.output.contains("pluginJar"))
    }

    @Test
    @DisplayName("paper-plugin.yml은 PluginJarPluginLoader를 가리킨다")
    fun `paper plugin yml pins loader to PluginJarPluginLoader`() {
        val result = gradleRunner("generatePaperPluginYml").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePaperPluginYml")?.outcome)

        val pluginYml = File(testProjectDir, "build/generated/pluginjar/paper-plugin.yml")
        assertTrue(pluginYml.exists(), "paper-plugin.yml should exist")

        val content = pluginYml.readText()
        assertTrue(content.contains("TestPaperPlugin"), "should contain plugin name")
        assertTrue(content.contains("api-version:"), "should contain api-version")
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
            "Should contain paper-loader classes at root"
        )

        assertTrue(
            entries.none { it.startsWith("BOOT-INF/lib/") && it.contains("paper-loader") },
            "plugin-jar libraries should NOT be in BOOT-INF/lib/"
        )

        jarFile.close()
    }

    @Test
    @DisplayName("runtimeClasspath에 paper-loader가 포함된다")
    fun `runtimeClasspath contains paper-loader`() {
        val result = gradleRunner("dependencies", "--configuration", "runtimeClasspath").build()

        assertTrue(
            result.output.contains("paper-loader"),
            "Should contain paper-loader"
        )
    }

    @Test
    @DisplayName("image bootstrap jar의 yml은 PluginJarPluginLoader를 가리킨다")
    fun `bootstrap jar pins loader`() {
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
        } finally {
            jarFile.close()
        }
    }

    @Test
    @DisplayName("pluginJar 결과물의 yml은 PluginJarPluginLoader를 가리킨다")
    fun `pluginJar archive pins loader`() {
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
        } finally {
            jarFile.close()
        }
    }

    @Test
    @DisplayName("image bootstrap jar에 main class가 포함된다")
    fun `bootstrap jar bundles main class`() {
        val result = gradleRunner("pluginImageBootstrap").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":pluginImageBootstrap")?.outcome)

        val bootstrapJar = File(testProjectDir, "build/docker-context/plugin/test-paper-plugin-1.0.0.jar")
        val jarFile = java.util.jar.JarFile(bootstrapJar)
        try {
            val entries = jarFile.entries().toList().map { it.name }
            assertTrue(
                entries.contains("com/example/TestPaperPlugin.class"),
                "bootstrap jar should contain main class",
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
    @DisplayName("image 서브플러그인은 nested aggregator 없이 image 태스크만 등록한다")
    fun `image sub-plugin registers image tasks without nested aggregator`() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                java
                id("kr.junhyung.plugin-jar.manifest")
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
                compileOnly("$pluginJarGroup:annotations:$pluginJarVersion")
                "pluginBootstrap"("$pluginJarGroup:paper-loader:$pluginJarVersion")
            }

            paperPlugin {
                author.set("TestAuthor")
            }

            pluginImage {
                targetImage.set("test-paper-plugin:latest")
            }
            """.trimIndent()
        )

        val result = gradleRunner("tasks").build()

        assertTrue(
            result.output.contains("pluginImageTar"),
            "image task should be registered when applying the image sub-plugin alone",
        )
        assertTrue(
            !result.output.contains("pluginJar -"),
            "nested pluginJar task must not be registered when only the image sub-plugin is applied",
        )
    }

    @Test
    @DisplayName("marker 없이 paperPlugin 수동 입력만으로 yml을 생성한다")
    fun `manifest generates yml from manual extension input without marker`() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                java
                id("kr.junhyung.plugin-jar.manifest")
            }

            group = "com.example"
            version = "1.0.0"

            repositories {
                maven(uri(providers.gradleProperty("functionalTestRepositoryPath").get()))
                mavenCentral()
                maven("https://repo.papermc.io/repository/maven-public/")
            }

            paperPlugin {
                main.set("com.example.ManualPaperPlugin")
                name.set("ManualPaperPlugin")
                apiVersion.set("1.21")
            }
            """.trimIndent()
        )

        val result = gradleRunner("generatePaperPluginYml").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePaperPluginYml")?.outcome)

        val pluginYml = File(testProjectDir, "build/generated/pluginjar/paper-plugin.yml")
        assertTrue(pluginYml.exists(), "paper-plugin.yml should exist")

        val content = pluginYml.readText()
        assertTrue(content.contains("com.example.ManualPaperPlugin"), "main should come from paperPlugin.main")
        assertTrue(content.contains("api-version:"), "api-version should be written")
    }
}

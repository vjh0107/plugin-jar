package kr.junhyung.pluginjar.gradle.manifest

import kr.junhyung.pluginjar.paper.PluginJarPluginLoader
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeneratePaperPluginYmlTest {

    @Test
    @DisplayName("PLUGIN_LOADER 상수가 PluginJarPluginLoader 실제 FQCN과 일치한다")
    fun `PLUGIN_LOADER matches actual loader class FQCN`() {
        assertEquals(PluginJarPluginLoader::class.java.name, GeneratePaperPluginYml.PLUGIN_LOADER)
    }
}

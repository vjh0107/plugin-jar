package kr.junhyung.pluginjar.gradle.tasks

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import tools.jackson.dataformat.yaml.YAMLMapper
import kr.junhyung.pluginjar.gradle.extensions.PaperPluginExtension
import kr.junhyung.pluginjar.gradle.jackson.GradlePropertyModule
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePaperPluginYml : DefaultTask() {

    @get:Nested
    abstract val extension: Property<PaperPluginExtension>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Generates paper-plugin.yml"
        group = "plugin"
    }

    @TaskAction
    fun generate() {
        val mapper = YAMLMapper.builder()
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .addModule(GradlePropertyModule())
            .changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null) }
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build()

        mapper.writeValue(outputFile.get().asFile, extension.get())
    }
}

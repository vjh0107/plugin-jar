package kr.junhyung.pluginjar.gradle.tasks

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import kr.junhyung.pluginjar.gradle.extensions.VelocityPluginExtension
import kr.junhyung.pluginjar.gradle.jackson.GradlePropertyModule
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateVelocityPluginJson : DefaultTask() {

    @get:Nested
    abstract val extension: Property<VelocityPluginExtension>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Generates velocity-plugin.json"
        group = "plugin"
    }

    @TaskAction
    fun generate() {
        val mapper = JsonMapper.builder()
            .addModule(GradlePropertyModule())
            .changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null) }
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build()

        mapper.writeValue(outputFile.get().asFile, extension.get())
    }
}

package kr.junhyung.pluginjar.gradle.manifest

import kr.junhyung.pluginjar.annotations.PluginMarker
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.Properties

@DisableCachingByDefault(because = "Cheap to recompute; output is small properties file")
abstract class ResolvePluginMarker : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "plugin"
    }

    companion object {
        const val TASK_NAME = "resolvePluginMarker"
        private const val OUTPUT_PATH = "pluginjar/plugin-meta.properties"

        internal fun register(project: Project): TaskProvider<ResolvePluginMarker> {
            val mainSourceSet = project.extensions.getByType<SourceSetContainer>()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            return project.tasks.register<ResolvePluginMarker>(TASK_NAME) {
                classesDirs.from(mainSourceSet.output.classesDirs)
                outputFile.convention(project.layout.buildDirectory.file(OUTPUT_PATH))
            }
        }
    }

    @TaskAction
    fun resolve() {
        val meta = Asm.scanForMarker(classesDirs.files)
            ?: throw IllegalStateException(
                "Cannot find plugin main class. Annotate your main class with @PluginMarker."
            )
        meta.writeTo(outputFile.get().asFile)
    }

    data class PluginMeta(
        val mainClass: String,
        val name: String?,
        val apiVersion: String?,
    ) {

        fun writeTo(file: File) {
            val properties = Properties().apply {
                setProperty(KEY_MAIN_CLASS, mainClass)
                name?.let { setProperty(KEY_NAME, it) }
                apiVersion?.let { setProperty(KEY_API_VERSION, it) }
            }
            file.outputStream().use { properties.store(it, null) }
        }

        companion object {
            private const val KEY_MAIN_CLASS = "mainClass"
            private const val KEY_NAME = "name"
            private const val KEY_API_VERSION = "apiVersion"

            fun readFrom(file: File): PluginMeta {
                val properties = Properties().apply { file.inputStream().use { load(it) } }
                val mainClass = properties.getProperty(KEY_MAIN_CLASS)
                    ?: error("Missing '$KEY_MAIN_CLASS' in ${file.absolutePath}")
                return PluginMeta(
                    mainClass,
                    properties.getProperty(KEY_NAME),
                    properties.getProperty(KEY_API_VERSION),
                )
            }
        }
    }

    private object Asm {

        private val MARKER_DESCRIPTOR = PluginMarker::class.java.descriptorString()

        fun scanForMarker(classesDirs: Iterable<File>): PluginMeta? = classesDirs
            .asSequence()
            .filter { it.isDirectory }
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "class" }
            .firstNotNullOfOrNull(::resolveFromFile)

        private fun resolveFromFile(classFile: File): PluginMeta? {
            val visitor = PluginMarkerClassVisitor()
            classFile.inputStream().use {
                ClassReader(it).accept(
                    visitor,
                    ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
                )
            }
            return visitor.pluginMeta
        }

        private class PluginMarkerClassVisitor : ClassVisitor(Opcodes.ASM9) {

            var pluginMeta: PluginMeta? = null
                private set

            private var currentClassName: String = ""

            override fun visit(
                version: Int, access: Int, name: String,
                signature: String?, superName: String?, interfaces: Array<out String>?,
            ) {
                currentClassName = name.replace('/', '.')
            }

            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                if (descriptor != MARKER_DESCRIPTOR) return null
                return PluginMarkerAnnotationVisitor { declaredName, declaredApiVersion ->
                    pluginMeta = PluginMeta(
                        currentClassName,
                        declaredName.takeIf { it.isNotEmpty() },
                        declaredApiVersion.takeIf { it.isNotEmpty() },
                    )
                }
            }
        }

        private class PluginMarkerAnnotationVisitor(
            private val onEnd: (declaredName: String, declaredApiVersion: String) -> Unit,
        ) : AnnotationVisitor(Opcodes.ASM9) {

            private var declaredName: String = ""
            private var declaredApiVersion: String = ""

            override fun visit(name: String?, value: Any?) {
                when (name) {
                    "name" -> if (value is String) declaredName = value
                    "apiVersion" -> if (value is String) declaredApiVersion = value
                }
            }

            override fun visitEnd() = onEnd(declaredName, declaredApiVersion)
        }
    }
}

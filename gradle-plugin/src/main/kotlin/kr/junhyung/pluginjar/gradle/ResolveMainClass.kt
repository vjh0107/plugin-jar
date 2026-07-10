package kr.junhyung.pluginjar.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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
abstract class ResolveMainClass : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    @get:Input
    abstract val markerAnnotation: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "plugin"
    }

    companion object {
        const val TASK_NAME = "resolveMainClass"
        private const val OUTPUT_PATH = "pluginjar/resolvedMainClassName"

        fun register(
            project: Project,
            markerAnnotation: String,
        ): TaskProvider<ResolveMainClass> {
            val mainSourceSet = project.extensions.getByType<SourceSetContainer>()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            return project.tasks.register<ResolveMainClass>(TASK_NAME) {
                classesDirs.from(mainSourceSet.output.classesDirs)
                this.markerAnnotation.convention(markerAnnotation)
                outputFile.convention(project.layout.buildDirectory.file(OUTPUT_PATH))
            }
        }
    }

    @TaskAction
    fun resolve() {
        val annotation = markerAnnotation.get()
        val meta = Asm.scanForMarker(classesDirs.files, annotation)
            ?: throw IllegalStateException(
                "Cannot find plugin main class. Annotate your main class with @$annotation."
            )
        meta.writeTo(outputFile.get().asFile)
    }

    data class PluginMeta(
        val mainClass: String,
        val id: String?,
    ) {

        fun writeTo(file: File) {
            val properties = Properties().apply {
                setProperty(KEY_MAIN_CLASS, mainClass)
                id?.let { setProperty(KEY_ID, it) }
            }
            file.outputStream().use { properties.store(it, null) }
        }

        companion object {
            private const val KEY_MAIN_CLASS = "mainClass"
            private const val KEY_ID = "id"

            fun readFrom(file: File): PluginMeta {
                val properties = Properties().apply { file.inputStream().use { load(it) } }
                val mainClass = properties.getProperty(KEY_MAIN_CLASS)
                    ?: error("Missing '$KEY_MAIN_CLASS' in ${file.absolutePath}")
                return PluginMeta(mainClass, properties.getProperty(KEY_ID))
            }
        }
    }

    private object Asm {

        fun scanForMarker(classesDirs: Iterable<File>, annotationClassName: String): PluginMeta? {
            val descriptor = "L" + annotationClassName.replace('.', '/') + ";"
            return classesDirs
                .asSequence()
                .filter { it.isDirectory }
                .flatMap { it.walkTopDown() }
                .filter { it.isFile && it.extension == "class" }
                .firstNotNullOfOrNull { resolveFromFile(it, descriptor) }
        }

        private fun resolveFromFile(classFile: File, markerDescriptor: String): PluginMeta? {
            val visitor = MarkerClassVisitor(markerDescriptor)
            classFile.inputStream().use {
                ClassReader(it).accept(
                    visitor,
                    ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
                )
            }
            return visitor.pluginMeta
        }

        private class MarkerClassVisitor(
            private val markerDescriptor: String,
        ) : ClassVisitor(Opcodes.ASM9) {

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
                if (descriptor != markerDescriptor) return null
                return MarkerAnnotationVisitor { declaredId ->
                    pluginMeta = PluginMeta(currentClassName, declaredId.takeIf { it.isNotEmpty() })
                }
            }
        }

        private class MarkerAnnotationVisitor(
            private val onEnd: (declaredId: String) -> Unit,
        ) : AnnotationVisitor(Opcodes.ASM9) {

            private var declaredId: String = ""

            override fun visit(name: String?, value: Any?) {
                if (name == "id" && value is String) declaredId = value
            }

            override fun visitEnd() = onEnd(declaredId)
        }
    }
}

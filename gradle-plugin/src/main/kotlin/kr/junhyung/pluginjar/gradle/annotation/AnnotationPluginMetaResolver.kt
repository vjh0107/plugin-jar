package kr.junhyung.pluginjar.gradle.annotation

import kr.junhyung.pluginjar.annotations.PluginMarker
import kr.junhyung.pluginjar.gradle.PluginMeta
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File

object AnnotationPluginMetaResolver {

    fun resolve(classesDirs: Iterable<File>): PluginMeta? {
        return classesDirs
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "class" }
            .mapNotNull { resolveFromFile(it) }
            .firstOrNull()
    }

    private fun resolveFromFile(classFile: File): PluginMeta? {
        return classFile.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val visitor = PluginMarkerClassVisitor()
            classReader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            visitor.pluginMeta
        }
    }

    private class PluginMarkerClassVisitor : ClassVisitor(Opcodes.ASM9) {

        var pluginMeta: PluginMeta? = null
            private set

        private var currentClassName: String? = null

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            currentClassName = name.replace('/', '.')
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor == PluginMarker::class.java.descriptorString()) {
                return PluginMarkerAnnotationVisitor { name ->
                    pluginMeta = PluginMeta(
                        mainClass = currentClassName!!,
                        name = name.takeIf { it.isNotEmpty() }
                    )
                }
            }
            return null
        }
    }

    private class PluginMarkerAnnotationVisitor(
        private val onEnd: (name: String) -> Unit
    ) : AnnotationVisitor(Opcodes.ASM9) {

        private var name: String = ""

        override fun visit(name: String?, value: Any?) {
            if (name == "name" && value is String) {
                this.name = value
            }
        }

        override fun visitEnd() {
            onEnd(name)
        }
    }
}

package kr.junhyung.pluginjar.asm

import com.google.auto.service.AutoService
import kr.junhyung.pluginjar.core.PluginMainClass
import kr.junhyung.pluginjar.core.PluginMainClassResolver
import org.springframework.asm.ClassReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.NotDirectoryException

@Suppress("unused")
@AutoService(PluginMainClassResolver::class)
class AsmPluginMainClassResolver : PluginMainClassResolver {
    override fun resolve(rootDirectory: File): PluginMainClass? {
        if (!rootDirectory.exists()) {
            throw FileNotFoundException("Directory not found: $rootDirectory")
        }
        if (!rootDirectory.isDirectory) {
            throw NotDirectoryException("Not a directory: $rootDirectory")
        }
        val classes = rootDirectory.walk()
            .filter { isClassFile(it) }
            .toList()
        for (clazz in classes) {
            val inputStream = clazz.inputStream()
            val descriptor = createDescriptor(inputStream)
            if (descriptor.isMainClass()) {
                return createPluginMainClass(descriptor.getName())
            }
        }
        return null
    }

    private fun createPluginMainClass(descriptorName: String): PluginMainClass {
        val qualifiedClassName = transformDescriptorNameToQualifiedClassName(descriptorName)
        return PluginMainClass(qualifiedClassName)
    }

    private fun transformDescriptorNameToQualifiedClassName(descriptorName: String): String {
        return descriptorName
            .removePrefix("L")
            .replace('/', '.')
            .removeSuffix(";")
    }

    private fun createDescriptor(inputStream: InputStream): PluginMainClassDescriptor {
        val classReader = ClassReader(inputStream)
        val visitor = PluginMainClassVisitor()
        classReader.accept(visitor, ClassReader.SKIP_DEBUG)
        return visitor
    }

    private fun isClassFile(file: File): Boolean {
        return file.isFile && file.extension == "class"
    }
}
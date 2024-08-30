package kr.junhyung.pluginjar.asm

import kr.junhyung.pluginjar.annotations.Plugin
import org.springframework.asm.AnnotationVisitor
import org.springframework.asm.ClassVisitor
import org.springframework.asm.Opcodes
import org.springframework.asm.SpringAsmInfo

class PluginMainClassVisitor : ClassVisitor(SpringAsmInfo.ASM_VERSION), PluginMainClassDescriptor {
    private var name: String? = null
    private var mainClass: Boolean = false

    override fun isMainClass(): Boolean {
        return mainClass
    }

    override fun getName(): String {
        return name ?: throw IllegalStateException("visit() must be called before getName()")
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (name != null && isAccess(access, Opcodes.ACC_PUBLIC)) {
            this.name = name
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor == Plugin::class.java.descriptorString()) {
            mainClass = true
        }
        return super.visitAnnotation(descriptor, visible)
    }

    @Suppress("SameParameterValue")
    private fun isAccess(access: Int, vararg requiredOpsCodes: Int): Boolean {
        for (requiredOpsCode in requiredOpsCodes) {
            if (access and requiredOpsCode == 0) {
                return false
            }
        }
        return true
    }
}
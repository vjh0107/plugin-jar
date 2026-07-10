package kr.junhyung.pluginjar.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

abstract class PluginJarBasePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val bootstrap = project.configurations.dependencyScope(BOOTSTRAP_CONFIGURATION_NAME) {
            description = "Libraries unpacked into the plugin jar root and bundled into the image bootstrap jar."
        }
        project.plugins.withType<JavaPlugin> {
            val objects = project.objects
            val runtimeClasspath = project.configurations
                .named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).get()
            runtimeClasspath.extendsFrom(bootstrap.get())

            val bootstrapClasspath = project.configurations.resolvable(BOOTSTRAP_CLASSPATH_CONFIGURATION_NAME).get()
            bootstrapClasspath.extendsFrom(bootstrap.get())
            // Standard JVM runtime attributes only; deliberately omit TargetJvmVersion so resolving
            // this view never forces the consumer's Java toolchain to be finalized prematurely.
            bootstrapClasspath.attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named<Bundling>(Bundling.EXTERNAL))
            }

            RuntimeClasspathView.register(project, runtimeClasspath, bootstrapClasspath)
        }
    }

    companion object {
        const val BOOTSTRAP_CONFIGURATION_NAME = "pluginBootstrap"
        private const val BOOTSTRAP_CLASSPATH_CONFIGURATION_NAME = "pluginBootstrapClasspath"
    }
}

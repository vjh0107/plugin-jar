package kr.junhyung.pluginjar.gradle.base

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class PluginExtension @Inject constructor(objects: ObjectFactory) {

    abstract val main: Property<String>
    abstract val name: Property<String>
    abstract val version: Property<String>
    abstract val apiVersion: Property<String>
    abstract val description: Property<String>
    abstract val author: Property<String>
    abstract val authors: ListProperty<String>
    abstract val website: Property<String>
    abstract val prefix: Property<String>
    abstract val defaultPermission: Property<String>
    abstract val bootstrapper: Property<String>
    abstract val hasOpenClassloader: Property<Boolean>

    val serverDependencies: NamedDomainObjectContainer<DependencyDefinition> =
        objects.domainObjectContainer(DependencyDefinition::class.java)

    val bootstrapDependencies: NamedDomainObjectContainer<DependencyDefinition> =
        objects.domainObjectContainer(DependencyDefinition::class.java)

    abstract class DependencyDefinition @Inject constructor(
        private val dependencyName: String,
    ) : Named {

        override fun getName(): String = dependencyName

        abstract val required: Property<Boolean>

        abstract val load: Property<LoadOrder>

        abstract val joinClasspath: Property<Boolean>
    }

    enum class LoadOrder {
        BEFORE, AFTER, OMIT
    }
}

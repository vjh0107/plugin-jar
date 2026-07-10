package kr.junhyung.pluginjar.gradle.velocity

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class VelocityPluginExtension @Inject constructor(objects: ObjectFactory) {

    abstract val main: Property<String>
    abstract val id: Property<String>
    abstract val name: Property<String>
    abstract val version: Property<String>
    abstract val description: Property<String>
    abstract val url: Property<String>
    abstract val authors: ListProperty<String>

    val dependencies: NamedDomainObjectContainer<DependencyDefinition> =
        objects.domainObjectContainer(DependencyDefinition::class.java)

    abstract class DependencyDefinition @Inject constructor(
        private val dependencyId: String,
    ) : Named {

        override fun getName(): String = dependencyId

        abstract val optional: Property<Boolean>
    }
}

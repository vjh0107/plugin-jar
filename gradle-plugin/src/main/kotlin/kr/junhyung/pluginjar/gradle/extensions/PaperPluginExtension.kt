package kr.junhyung.pluginjar.gradle.extensions

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
abstract class PaperPluginExtension @Inject constructor(objects: ObjectFactory) {

    @get:Input
    @get:JsonProperty("main")
    abstract val main: Property<String>

    @get:Input
    @get:JsonProperty("name")
    abstract val name: Property<String>

    @get:Input
    @get:JsonProperty("version")
    abstract val version: Property<String>

    @get:Input
    @get:JsonProperty("api-version")
    abstract val apiVersion: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("description")
    abstract val description: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("author")
    abstract val author: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("authors")
    abstract val authors: ListProperty<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("website")
    abstract val website: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("prefix")
    abstract val prefix: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("default-permission")
    abstract val defaultPermission: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("bootstrapper")
    abstract val bootstrapper: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("loader")
    abstract val loader: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("has-open-classloader")
    abstract val hasOpenClassloader: Property<Boolean>

    @get:Nested
    @get:JsonIgnore
    val serverDependencies: NamedDomainObjectContainer<DependencyDefinition> =
        objects.domainObjectContainer(DependencyDefinition::class.java)

    @get:Nested
    @get:JsonIgnore
    val bootstrapDependencies: NamedDomainObjectContainer<DependencyDefinition> =
        objects.domainObjectContainer(DependencyDefinition::class.java)

    @get:Internal
    @get:JsonProperty("dependencies")
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val dependencies: Map<String, Map<String, DependencyDefinition>>
        get() = buildMap {
            serverDependencies.takeIf { it.isNotEmpty() }?.let {
                put("server", it.associateBy { dep -> dep.name })
            }
            bootstrapDependencies.takeIf { it.isNotEmpty() }?.let {
                put("bootstrap", it.associateBy { dep -> dep.name })
            }
        }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    abstract class DependencyDefinition @Inject constructor(
        @get:JsonIgnore
        private val dependencyName: String
    ) : Named {

        @Internal
        @JsonIgnore
        override fun getName(): String = dependencyName

        @get:Input
        @get:Optional
        @get:JsonProperty("required")
        abstract val required: Property<Boolean>

        @get:Input
        @get:Optional
        @get:JsonProperty("load")
        abstract val load: Property<LoadOrder>

        @get:Input
        @get:Optional
        @get:JsonProperty("join-classpath")
        abstract val joinClasspath: Property<Boolean>
    }

    enum class LoadOrder {
        BEFORE, AFTER, OMIT
    }
}

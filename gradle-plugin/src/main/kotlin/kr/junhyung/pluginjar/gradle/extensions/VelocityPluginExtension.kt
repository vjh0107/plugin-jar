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
abstract class VelocityPluginExtension @Inject constructor(objects: ObjectFactory) {

    @get:Input
    @get:JsonProperty("id")
    abstract val id: Property<String>

    @get:Input
    @get:JsonProperty("name")
    abstract val name: Property<String>

    @get:Input
    @get:JsonProperty("version")
    abstract val version: Property<String>

    @get:Input
    @get:JsonProperty("main")
    abstract val main: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("description")
    abstract val description: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("url")
    abstract val url: Property<String>

    @get:Input
    @get:Optional
    @get:JsonProperty("authors")
    abstract val authors: ListProperty<String>

    @get:Nested
    @get:JsonIgnore
    val dependencyDefinitions: NamedDomainObjectContainer<DependencyDefinition> =
        objects.domainObjectContainer(DependencyDefinition::class.java)

    @get:Internal
    @get:JsonProperty("dependencies")
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val dependencies: List<DependencyDefinition>
        get() = dependencyDefinitions.toList()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    abstract class DependencyDefinition @Inject constructor(
        private val dependencyId: String
    ) : Named {

        @Internal
        @JsonIgnore
        override fun getName(): String = dependencyId

        @get:Input
        @get:JsonProperty("id")
        val id: String get() = dependencyId

        @get:Input
        @get:Optional
        @get:JsonProperty("optional")
        abstract val optional: Property<Boolean>
    }
}

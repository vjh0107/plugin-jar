package kr.junhyung.pluginjar.gradle.paper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_EMPTY)
internal data class PaperPluginManifest(
    @JsonProperty("main") val main: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("version") val version: String,
    @JsonProperty("api-version") val apiVersion: String,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("author") val author: String? = null,
    @JsonProperty("authors") val authors: List<String>? = null,
    @JsonProperty("website") val website: String? = null,
    @JsonProperty("prefix") val prefix: String? = null,
    @JsonProperty("default-permission") val defaultPermission: String? = null,
    @JsonProperty("bootstrapper") val bootstrapper: String? = null,
    @JsonProperty("loader") val loader: String? = null,
    @JsonProperty("has-open-classloader") val hasOpenClassloader: Boolean? = null,
    @JsonProperty("dependencies") val dependencies: Map<String, Map<String, DependencyDescriptor>>? = null,
) : Serializable {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class DependencyDescriptor(
        @JsonProperty("required") val required: Boolean? = null,
        @JsonProperty("load") val load: String? = null,
        @JsonProperty("join-classpath") val joinClasspath: Boolean? = null,
    ) : Serializable
}

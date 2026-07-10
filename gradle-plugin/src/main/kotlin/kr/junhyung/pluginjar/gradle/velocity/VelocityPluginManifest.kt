package kr.junhyung.pluginjar.gradle.velocity

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_EMPTY)
internal data class VelocityPluginManifest(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("version") val version: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("authors") val authors: List<String>? = null,
    @JsonProperty("dependencies") val dependencies: List<Dependency>? = null,
    @JsonProperty("main") val main: String,
) : Serializable {

    data class Dependency(
        @JsonProperty("id") val id: String,
        @JsonProperty("optional") val optional: Boolean,
    ) : Serializable
}

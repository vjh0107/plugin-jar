val versionCatalogs = extensions.getByName("versionCatalogs") as VersionCatalogsExtension
val libs = versionCatalogs.named("libs")

with(pluginManager) {
    apply(libs.findPlugin("kotlin-kapt").get().get().pluginId)
}

dependencies {
    "kapt"(libs.findLibrary("auto-service").get())
    "compileOnly"(libs.findLibrary("auto-service-annotations").get())
}
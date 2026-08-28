package kr.junhyung.pluginjar.gradle.image

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.Credential
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import kr.junhyung.pluginjar.core.ContainerImageLayout
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.time.Instant
import java.util.Optional

@DisableCachingByDefault(because = "Builds and ships an image; output is not a local artifact suitable for caching")
abstract class AbstractPluginImageBuild : DefaultTask() {

    @get:Input
    abstract val targetImage: Property<String>

    @get:Input
    @get:Option(option = "tag", description = "Tags to apply when pushing the image. Repeat to specify multiple.")
    abstract val tags: SetProperty<String>

    @get:Input
    abstract val pluginName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val libsFiles: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bootstrapJar: RegularFileProperty

    @get:Internal
    abstract val applicationLayersCache: DirectoryProperty

    @get:Internal
    abstract val baseImageLayersCache: DirectoryProperty

    init {
        group = "plugin"
    }

    @TaskAction
    fun build() {
        val name = pluginName.get()
        val imageRoot = AbsoluteUnixPath.get("/")
        val payloadRoot = imageRoot.resolve(name + ContainerImageLayout.PAYLOAD_DIRECTORY_SUFFIX)

        val containerBuilder = Jib.fromScratch().setCreationTime(Instant.EPOCH)
        addLibsLayer(containerBuilder, payloadRoot)
        addBootstrapLayer(containerBuilder, imageRoot, name)

        val containerizer = createContainerizer().apply {
            tags.get().forEach(::withAdditionalTag)
            applicationLayersCache.orNull?.asFile?.toPath()?.let { setApplicationLayersCache(it) }
            baseImageLayersCache.orNull?.asFile?.toPath()?.let { setBaseImageLayersCache(it) }
            addEventHandler(LogEvent::class.java, ::dispatchLog)
        }

        val container = containerBuilder.containerize(containerizer)
        logger.lifecycle("Built image {} (digest {})", container.targetImage, container.digest)
    }

    protected abstract fun createContainerizer(): Containerizer

    protected fun parseTargetImage(): ImageReference = ImageReference.parse(targetImage.get())

    protected fun configureFrom(
        imageExtension: PluginImageExtension,
        bootstrap: TaskProvider<PluginImageBootstrap>,
        libs: TaskProvider<Sync>,
    ) {
        targetImage.set(imageExtension.targetImage)
        tags.convention(imageExtension.tags)
        pluginName.set(bootstrap.flatMap { it.archiveBaseName })
        libsFiles.from(libs)
        bootstrapJar.set(bootstrap.flatMap { it.archiveFile })
        applicationLayersCache.set(imageExtension.applicationLayersCache)
        baseImageLayersCache.set(imageExtension.baseImageLayersCache)
    }

    protected fun registryImageWithCredentials(
        ref: ImageReference,
        username: String?,
        password: String?,
    ): RegistryImage {
        val image = RegistryImage.named(ref)
        val factory = CredentialRetrieverFactory.forImage(ref, ::dispatchLog)

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            image.addCredentialRetriever(
                factory.known(Credential.from(username, password), "user-supplied")
            )
            return image
        }

        val dockerConfig = factory.dockerConfig()
        image.addCredentialRetriever {
            try {
                dockerConfig.retrieve()
            } catch (e: Exception) {
                logger.warn("Skipping docker config credentials: {}", e.message)
                Optional.empty()
            }
        }
        return image
    }

    private fun addLibsLayer(builder: JibContainerBuilder, payloadRoot: AbsoluteUnixPath) {
        val libs = libsFiles.asFileTree.files
            .filter { it.isFile && it.name.endsWith(ContainerImageLayout.JAR_EXTENSION) }
            .sortedBy { it.name }
        if (libs.isEmpty()) return
        val layer = FileEntriesLayer.builder().setName(ContainerImageLayout.LIBS_DIRECTORY)
        for (jar in libs) {
            layer.addEntry(jar.toPath(), payloadRoot.resolve("${ContainerImageLayout.LIBS_DIRECTORY}/${jar.name}"))
        }
        builder.addFileEntriesLayer(layer.build())
    }

    private fun addBootstrapLayer(
        builder: JibContainerBuilder,
        imageRoot: AbsoluteUnixPath,
        pluginName: String,
    ) {
        val layer = FileEntriesLayer.builder()
            .setName("bootstrap")
            .addEntry(bootstrapJar.get().asFile.toPath(), imageRoot.resolve("$pluginName.jar"))
            .build()
        builder.addFileEntriesLayer(layer)
    }


    private fun dispatchLog(event: LogEvent) {
        when (event.level) {
            LogEvent.Level.ERROR -> logger.error(event.message)
            LogEvent.Level.WARN -> logger.warn(event.message)
            LogEvent.Level.LIFECYCLE, LogEvent.Level.PROGRESS -> logger.lifecycle(event.message)
            LogEvent.Level.INFO -> logger.info(event.message)
            LogEvent.Level.DEBUG -> logger.debug(event.message)
        }
    }
}

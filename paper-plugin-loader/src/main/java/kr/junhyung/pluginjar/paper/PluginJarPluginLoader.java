package kr.junhyung.pluginjar.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class PluginJarPluginLoader implements PluginLoader {

    private static final String PLUGIN_JAR_TYPE_HEADER = "Plugin-Jar-Type";
    private static final String TYPE_NESTED = "nested";
    private static final String TYPE_IMAGE = "image";

    private static final Logger logger = LoggerFactory.getLogger(PluginJarPluginLoader.class);

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        Path pluginJar = classpathBuilder.getContext().getPluginSource();
        String type = readPluginJarType(pluginJar);
        ClasspathResolver resolver = createResolver(type, pluginJar);

        try (Stream<Path> jars = resolver.resolve()) {
            jars.forEach(jar -> {
                classpathBuilder.addLibrary(new JarLibrary(jar));
                logger.trace("Registered library: {}", jar.getFileName());
            });
        }
        logger.info("Loaded plugin libraries via {} resolver", type);
    }

    private static String readPluginJarType(Path jar) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                throw new IllegalStateException(
                        "Plugin jar is missing META-INF/MANIFEST.MF: " + jar
                );
            }
            String value = manifest.getMainAttributes().getValue(PLUGIN_JAR_TYPE_HEADER);
            if (value == null || value.isEmpty()) {
                throw new IllegalStateException(
                        "Plugin jar manifest is missing '" + PLUGIN_JAR_TYPE_HEADER + "' attribute: " + jar
                );
            }
            return value;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read manifest from " + jar, e);
        }
    }

    private static ClasspathResolver createResolver(String type, Path pluginJar) {
        return switch (type) {
            case TYPE_NESTED -> new NestedJarClasspathResolver(pluginJar);
            case TYPE_IMAGE -> new ContainerImageClasspathResolver(pluginJar);
            default -> throw new IllegalStateException(
                    "Unsupported '" + PLUGIN_JAR_TYPE_HEADER + "': '" + type + "'. " +
                            "Expected '" + TYPE_NESTED + "' or '" + TYPE_IMAGE + "'."
            );
        };
    }
}

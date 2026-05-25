package kr.junhyung.pluginjar.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PluginJarPluginLoader implements PluginLoader {

    private static final String JAR_EXTENSION = ".jar";
    private static final String PAYLOAD_DIR_SUFFIX = ".d";

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        Path pluginJar = classpathBuilder.getContext().getPluginSource();
        ClasspathResolver resolver = detectResolver(pluginJar);

        try {
            List<Path> jars = resolver.resolve();
            jars.forEach(jar -> classpathBuilder.addLibrary(new JarLibrary(jar)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve plugin classpath", e);
        }
    }

    private static ClasspathResolver detectResolver(Path pluginJar) {
        if (hasPayloadDirectory(pluginJar)) {
            return new ContainerImageClasspathResolver(pluginJar);
        }
        return new NestedJarClasspathResolver(pluginJar);
    }

    private static boolean hasPayloadDirectory(Path pluginJar) {
        Path absoluteJar = pluginJar.toAbsolutePath();
        String fileName = absoluteJar.getFileName().toString();
        if (!fileName.endsWith(JAR_EXTENSION)) {
            return false;
        }
        String baseName = fileName.substring(0, fileName.length() - JAR_EXTENSION.length());
        Path payloadDir = absoluteJar.getParent().resolve(baseName + PAYLOAD_DIR_SUFFIX);
        return Files.isDirectory(payloadDir);
    }
}

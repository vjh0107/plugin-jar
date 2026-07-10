package kr.junhyung.pluginjar.core;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ClasspathResolvers {

    private ClasspathResolvers() {
    }

    public static ClasspathResolver detect(Path pluginJar) {
        if (hasPayloadDirectory(pluginJar)) {
            return new ContainerImageClasspathResolver(pluginJar);
        }
        return new NestedJarClasspathResolver(pluginJar);
    }

    private static boolean hasPayloadDirectory(Path pluginJar) {
        String fileName = pluginJar.toAbsolutePath().getFileName().toString();
        if (!fileName.endsWith(ContainerImageLayout.JAR_EXTENSION)) {
            return false;
        }
        return Files.isDirectory(ContainerImageLayout.payloadDirectory(pluginJar));
    }
}

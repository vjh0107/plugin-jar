package kr.junhyung.pluginjar.core;

import java.nio.file.Path;

public final class ContainerImageLayout {

    public static final String JAR_EXTENSION = ".jar";
    public static final String PAYLOAD_DIRECTORY_SUFFIX = ".d";
    public static final String LIBS_DIRECTORY = "libs";

    private ContainerImageLayout() {
    }

    public static Path payloadDirectory(Path pluginJar) {
        Path absoluteJar = pluginJar.toAbsolutePath();
        String fileName = absoluteJar.getFileName().toString();
        if (!fileName.endsWith(JAR_EXTENSION)) {
            throw new IllegalArgumentException("Plugin source is not a jar: " + fileName);
        }
        String baseName = fileName.substring(0, fileName.length() - JAR_EXTENSION.length());
        return absoluteJar.getParent().resolve(baseName + PAYLOAD_DIRECTORY_SUFFIX);
    }
}

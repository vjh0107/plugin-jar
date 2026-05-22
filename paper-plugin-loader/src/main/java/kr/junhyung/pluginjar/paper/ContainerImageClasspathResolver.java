package kr.junhyung.pluginjar.paper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class ContainerImageClasspathResolver implements ClasspathResolver {

    private static final String JAR_EXTENSION = ".jar";
    private static final String PAYLOAD_DIR_SUFFIX = ".d";
    private static final String LIBS_DIR = "libs";
    private static final String MODULES_DIR = "modules";

    private final Path pluginJar;

    public ContainerImageClasspathResolver(Path pluginJar) {
        this.pluginJar = pluginJar;
    }

    @Override
    public Stream<Path> resolve() {
        Path absoluteJar = pluginJar.toAbsolutePath();
        String baseName = stripJarExtension(absoluteJar.getFileName().toString());
        Path payloadRoot = absoluteJar.getParent().resolve(baseName + PAYLOAD_DIR_SUFFIX);
        Stream<Path> libs = streamJars(payloadRoot.resolve(LIBS_DIR), false);
        Stream<Path> modules = streamJars(payloadRoot.resolve(MODULES_DIR), true);
        return Stream.concat(libs, modules);
    }

    private static String stripJarExtension(String fileName) {
        if (!fileName.endsWith(JAR_EXTENSION)) {
            throw new IllegalStateException("Plugin source is not a jar: " + fileName);
        }
        return fileName.substring(0, fileName.length() - JAR_EXTENSION.length());
    }

    private static Stream<Path> streamJars(Path directory, boolean required) {
        if (!Files.isDirectory(directory)) {
            if (required) {
                throw new IllegalStateException("Required directory is missing: " + directory);
            }
            return Stream.empty();
        }
        try {
            return Files.walk(directory, 1)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(JAR_EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to enumerate " + directory, e);
        }
    }
}

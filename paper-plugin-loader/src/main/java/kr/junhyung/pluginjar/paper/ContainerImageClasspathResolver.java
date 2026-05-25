package kr.junhyung.pluginjar.paper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    public List<Path> resolve() throws IOException {
        Path absoluteJar = pluginJar.toAbsolutePath();
        String baseName = stripJarExtension(absoluteJar.getFileName().toString());
        Path payloadRoot = absoluteJar.getParent().resolve(baseName + PAYLOAD_DIR_SUFFIX);
        List<Path> result = new ArrayList<>();
        result.addAll(listJars(payloadRoot.resolve(LIBS_DIR)));
        result.addAll(listJars(payloadRoot.resolve(MODULES_DIR)));
        return result;
    }

    private static String stripJarExtension(String fileName) {
        if (!fileName.endsWith(JAR_EXTENSION)) {
            throw new IllegalStateException("Plugin source is not a jar: " + fileName);
        }
        return fileName.substring(0, fileName.length() - JAR_EXTENSION.length());
    }

    private static List<Path> listJars(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.walk(directory, 1)) {
            return entries.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(JAR_EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }
}

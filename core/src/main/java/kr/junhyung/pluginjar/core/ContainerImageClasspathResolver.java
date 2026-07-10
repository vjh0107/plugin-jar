package kr.junhyung.pluginjar.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ContainerImageClasspathResolver implements ClasspathResolver {

    private final Path pluginJar;

    public ContainerImageClasspathResolver(Path pluginJar) {
        this.pluginJar = pluginJar;
    }

    @Override
    public List<Path> resolve() throws IOException {
        Path payloadRoot = ContainerImageLayout.payloadDirectory(pluginJar);
        List<Path> result = new ArrayList<>();
        result.addAll(listJars(payloadRoot.resolve(ContainerImageLayout.LIBS_DIRECTORY)));
        result.addAll(listJars(payloadRoot.resolve(ContainerImageLayout.MODULES_DIRECTORY)));
        return result;
    }

    private static List<Path> listJars(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.walk(directory, 1)) {
            return entries.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(ContainerImageLayout.JAR_EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }
}

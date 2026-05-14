package kr.junhyung.pluginjar.paper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class ContainerImageClasspathResolver implements ClasspathResolver {

    private static final String PAYLOAD_DIR_SUFFIX = ".d";
    private static final String LIBS_DIR = "libs";
    private static final String MODULES_DIR = "modules";

    private final String pluginName;

    public ContainerImageClasspathResolver(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public Stream<Path> resolve() {
        Path payloadRoot = Paths.get(pluginName + PAYLOAD_DIR_SUFFIX).toAbsolutePath();
        Stream<Path> libs = streamJars(payloadRoot.resolve(LIBS_DIR), false);
        Stream<Path> modules = streamJars(payloadRoot.resolve(MODULES_DIR), true);
        return Stream.concat(libs, modules);
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
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to enumerate " + directory, e);
        }
    }
}

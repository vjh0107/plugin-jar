package kr.junhyung.pluginjar.paper;

import java.nio.file.Path;
import java.util.stream.Stream;

public class NestedJarClasspathResolver implements ClasspathResolver {

    private final Path pluginJar;

    public NestedJarClasspathResolver(Path pluginJar) {
        this.pluginJar = pluginJar;
    }

    @Override
    public Stream<Path> resolve() {
        return LibraryExtractor.extractToTempDirectory(pluginJar);
    }
}

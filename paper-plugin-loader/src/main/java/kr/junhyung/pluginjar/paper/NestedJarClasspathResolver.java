package kr.junhyung.pluginjar.paper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NestedJarClasspathResolver implements ClasspathResolver {

    private final Path pluginJar;

    public NestedJarClasspathResolver(Path pluginJar) {
        this.pluginJar = pluginJar;
    }

    @Override
    public List<Path> resolve() throws IOException {
        return kr.junhyung.pluginjar.core.LibraryExtractor.extractToTempDirectory(pluginJar);
    }
}

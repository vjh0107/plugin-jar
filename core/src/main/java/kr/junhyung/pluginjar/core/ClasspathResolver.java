package kr.junhyung.pluginjar.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ClasspathResolver {

    List<Path> resolve() throws IOException;

}

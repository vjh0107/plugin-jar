package kr.junhyung.pluginjar.paper;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface ClasspathResolver {

    Stream<Path> resolve();

}
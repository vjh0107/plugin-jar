package kr.junhyung.pluginjar.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import kr.junhyung.pluginjar.core.LibraryExtractor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Paper plugin loader implementation.
 *
 * <p>Extracts libraries from the {@code BOOT-INF/lib/} directory inside the plugin JAR
 * and adds them to the classpath.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class PluginJarPluginLoader implements PluginLoader {

    private static final Logger logger = LoggerFactory.getLogger(PluginJarPluginLoader.class);

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        Path pluginPath = classpathBuilder.getContext().getPluginSource();
        logger.debug("Loading plugin libraries from: {}", pluginPath);

        List<Path> extractedLibs = LibraryExtractor.extractToTempDirectory(pluginPath, tempJar -> {
            classpathBuilder.addLibrary(new JarLibrary(tempJar));
            logger.trace("Loaded library: {}", tempJar.getFileName());
        });

        if (extractedLibs.isEmpty()) {
            logger.debug("No libraries found in plugin jar");
            return;
        }

        logger.info("Loaded {} libraries to classpath", extractedLibs.size());
    }

}

package kr.junhyung.pluginjar.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import kr.junhyung.pluginjar.core.ClasspathResolvers;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PluginJarPluginLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        Path pluginJar = classpathBuilder.getContext().getPluginSource();
        try {
            List<Path> jars = ClasspathResolvers.detect(pluginJar).resolve();
            jars.forEach(jar -> classpathBuilder.addLibrary(new JarLibrary(jar)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve plugin classpath", e);
        }
    }
}

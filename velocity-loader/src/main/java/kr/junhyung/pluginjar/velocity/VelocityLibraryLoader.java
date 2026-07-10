package kr.junhyung.pluginjar.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import kr.junhyung.pluginjar.core.ClasspathResolvers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class VelocityLibraryLoader {

    private VelocityLibraryLoader() {
    }

    public static void load(PluginManager pluginManager, PluginContainer pluginContainer) throws IOException {
        Path source = pluginContainer.getDescription().getSource()
            .orElseThrow(() -> new IllegalStateException("Plugin source path is not available"));
        List<Path> jars = ClasspathResolvers.detect(source).resolve();
        for (Path jar : jars) {
            pluginManager.addToClasspath(pluginContainer, jar);
        }
    }

}

package kr.junhyung.pluginjar.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import kr.junhyung.pluginjar.core.LibraryExtractor;

import java.nio.file.Path;

public final class PluginJarLibraryLoader {

    private PluginJarLibraryLoader() {
    }

    public static void loadLibraries(PluginManager pluginManager, PluginContainer pluginContainer) {
        Path pluginPath = pluginContainer.getDescription().getSource()
            .orElseThrow(() -> new IllegalStateException("Plugin source path not found"));

        LibraryExtractor.extractToTempDirectory(pluginPath, tempJar ->
            pluginManager.addToClasspath(pluginContainer, tempJar)
        );
    }
}

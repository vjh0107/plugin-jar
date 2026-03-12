package com.example;

import kr.junhyung.pluginjar.annotations.PluginMarker;
import org.bukkit.plugin.java.JavaPlugin;

@PluginMarker(name = "TestPaperPlugin")
public class TestPaperPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("TestPaperPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TestPaperPlugin disabled!");
    }
}

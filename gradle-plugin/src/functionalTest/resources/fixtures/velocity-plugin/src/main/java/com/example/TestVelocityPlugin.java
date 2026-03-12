package com.example;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import kr.junhyung.pluginjar.annotations.PluginMarker;
import org.slf4j.Logger;

@PluginMarker(name = "testvelocityplugin")
public class TestVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public TestVelocityPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("TestVelocityPlugin initialized!");
    }
}

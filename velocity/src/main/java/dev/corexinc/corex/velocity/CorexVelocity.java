package dev.corexinc.corex.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.logging.Logger;

@Plugin(
        id = "corex",
        name = "Corex",
        version = "1.0"
)

public class CorexVelocity {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public CorexVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }
    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        logger.info("Hey Corex! Say hello to Velocity :)");
    }
}

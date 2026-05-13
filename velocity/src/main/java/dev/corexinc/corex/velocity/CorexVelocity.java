package dev.corexinc.corex.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.corexinc.corex.engine.utils.CorexLogger;

@Plugin(
        id = "corex",
        name = "Corex",
        version = "1.0",
        description = "Modern, Paper-exclusive compiled scripting engine",
        authors = { "tizis0", "Nybik_YT" }
)

public class CorexVelocity {

    private final ProxyServer server;

    @Inject
    public CorexVelocity(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        CorexLogger.setConsole(server.getConsoleCommandSource());

        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");
    }
}
package dev.corexinc.corex.velocity.environment.addons;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.corexinc.corex.api.addons.AbstractCorexAddon;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves addon owners from Velocity plugins.
 *
 * <p>Velocity has no lookup from a class back to the plugin that loaded it, so a class is matched
 * against the classloader of every loaded plugin instance. The answer is cached by the caller, so
 * the sweep happens once per class that ever registers anything.</p>
 *
 * @since 1.0.0
 */
public final class VelocityAddonResolver extends AddonResolver {

    private final ProxyServer server;
    private final Object corex;

    /**
     * @param server the proxy, used to enumerate plugins.
     * @param corex  the Corex plugin instance, which owns everything the engine registers itself.
     */
    public VelocityAddonResolver(@NotNull ProxyServer server, @NotNull Object corex) {
        this.server = server;
        this.corex = corex;
    }

    @Override
    @Nullable
    public AddonOwner describe(@NotNull AbstractCorexAddon addon) {
        return server.getPluginManager().fromInstance(addon)
                .map(container -> toOwner(container, AddonOwner.Origin.ADDON))
                .orElse(null);
    }

    @Override
    @NotNull
    public String registrationHint() {
        return "your ProxyInitializeEvent handler";
    }

    @Override
    @Nullable
    public AddonOwner ownerOfClass(@NotNull Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            return null;
        }

        for (PluginContainer container : server.getPluginManager().getPlugins()) {
            Object instance = container.getInstance().orElse(null);
            if (instance == null || instance.getClass().getClassLoader() != loader) {
                continue;
            }
            if (instance == corex) {
                return AddonOwner.CORE;
            }
            return toOwner(container, instance instanceof AbstractCorexAddon
                    ? AddonOwner.Origin.ADDON
                    : AddonOwner.Origin.FOREIGN);
        }
        return null;
    }

    private static AddonOwner toOwner(PluginContainer container, AddonOwner.Origin origin) {
        PluginDescription description = container.getDescription();
        return new AddonOwner(
                description.getName().orElse(description.getId()),
                description.getVersion().orElse(""),
                origin);
    }
}

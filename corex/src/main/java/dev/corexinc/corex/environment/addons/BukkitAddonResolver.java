package dev.corexinc.corex.environment.addons;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.addons.AbstractCorexAddon;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonResolver;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves addon owners from Bukkit plugins.
 *
 * <p>Paper gives every plugin its own classloader, so the plugin a class came from is an exact
 * answer rather than a guess, which is what makes it safe to refuse a registration on the basis
 * of who made the call.</p>
 *
 * @since 1.0.0
 */
public final class BukkitAddonResolver extends AddonResolver {

    @Override
    @Nullable
    public AddonOwner describe(@NotNull AbstractCorexAddon addon) {
        if (!(addon instanceof Plugin plugin)) {
            return null;
        }
        return new AddonOwner(plugin.getName(), versionOf(plugin), AddonOwner.Origin.ADDON);
    }

    @Override
    @NotNull
    public String registrationHint() {
        return "the plugin onLoad()";
    }

    @Override
    @Nullable
    public AddonOwner ownerOfClass(@NotNull Class<?> clazz) {
        Plugin plugin;
        try {
            plugin = JavaPlugin.getProvidingPlugin(clazz);
        } catch (Throwable notAPluginClass) {
            return null;
        }

        if (plugin instanceof Corex) {
            return AddonOwner.CORE;
        }
        AddonOwner.Origin origin = plugin instanceof AbstractCorexAddon
                ? AddonOwner.Origin.ADDON
                : AddonOwner.Origin.FOREIGN;
        return new AddonOwner(plugin.getName(), versionOf(plugin), origin);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static String versionOf(Plugin plugin) {
        try {
            return plugin.getPluginMeta().getVersion();
        } catch (Throwable unsupported) {
            return "";
        }
    }
}

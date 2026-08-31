package dev.corexinc.corex.environment.network;

import dev.corexinc.corex.engine.network.NetworkSecret;
import dev.corexinc.corex.engine.network.SecretSources;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.utils.scripts.EnvManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Works out which shared secret this backend should sign with.
 *
 * <h2>Why the forwarding secret is borrowed</h2>
 * <p>A shared secret cannot be generated locally: generating one per server produces a different
 * value on every machine, which is the one thing a shared secret must not be. Something already
 * synchronised across the proxy and every backend has to be the source, and on a modern Velocity
 * network exactly one such thing exists: the forwarding secret. It is already on every box, it is
 * already the same everywhere, and it is already the credential the whole network's identity rests
 * on, so borrowing it adds no new thing to leak.</p>
 *
 * <p>It is never used raw. {@link dev.corexinc.corex.engine.network.PacketCodec#toKey} runs it
 * through a fixed label first, so the bytes signing a Corex packet are not the bytes Velocity
 * checks on a handshake.</p>
 *
 * <h2>Order</h2>
 * <p>{@link SecretSources} owns the chain, so this side and the proxy cannot drift apart on it.
 * The only step that belongs here is the last one: {@code proxies.velocity.secret} from
 * {@code config/paper-global.yml}, read when Velocity forwarding is switched on. Finding nothing
 * at all leaves plain messages working and privileged packets refused.</p>
 *
 * @since 1.0.0
 */
public final class BackendSecretResolver {

    private static final String PAPER_GLOBAL_PATH = "config/paper-global.yml";
    private static final String VELOCITY_ENABLED_KEY = "proxies.velocity.enabled";
    private static final String VELOCITY_SECRET_KEY = "proxies.velocity.secret";

    private BackendSecretResolver() {}

    public static @Nullable NetworkSecret resolve(@NotNull Plugin plugin, boolean allowProxySecret) {
        return SecretSources.resolve(
                EnvManager.getSecret(SecretSources.COREX_KEY),
                allowProxySecret,
                () -> fromPaperGlobal(plugin));
    }

    private static @Nullable NetworkSecret fromPaperGlobal(Plugin plugin) {
        File root = serverRoot(plugin);
        if (root == null) {
            return null;
        }

        File paperGlobal = new File(root, PAPER_GLOBAL_PATH);
        if (!paperGlobal.isFile()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(paperGlobal);
            if (!config.getBoolean(VELOCITY_ENABLED_KEY, false)) {
                return null;
            }

            String secret = config.getString(VELOCITY_SECRET_KEY, "");
            if (secret == null || secret.isBlank()) {
                return null;
            }
            return new NetworkSecret(secret, "the Velocity forwarding secret in " + PAPER_GLOBAL_PATH);
        }
        catch (Exception e) {
            CorexLogger.warn("Corex could not read " + PAPER_GLOBAL_PATH + " for the forwarding secret: "
                    + e.getMessage());
            return null;
        }
    }

    private static @Nullable File serverRoot(Plugin plugin) {
        File dataFolder = plugin.getDataFolder().getAbsoluteFile();
        File pluginsFolder = dataFolder.getParentFile();
        return pluginsFolder != null ? pluginsFolder.getParentFile() : null;
    }
}

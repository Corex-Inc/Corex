package dev.corexinc.corex.velocity.environment.network;

import dev.corexinc.corex.engine.network.NetworkSecret;
import dev.corexinc.corex.engine.network.SecretSources;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.utils.scripts.EnvManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Works out which shared secret this proxy should sign with.
 *
 * <p>The proxy side of {@link dev.corexinc.corex.environment.network.BackendSecretResolver}, and it
 * has to look in different places for the same value: the Velocity API deliberately does not expose
 * the forwarding secret, so it comes off disk or out of the environment the way Velocity itself
 * reads it.</p>
 *
 * <h2>Order</h2>
 * <p>{@link SecretSources} owns the chain, so this side and the backends cannot drift apart on it.
 * The only step that belongs here is the last one, the secret file. Finding nothing at all leaves
 * plain messages working and privileged packets refused.</p>
 *
 * <h2>The secret file</h2>
 * <p>Two hops, because the file does not have a fixed name. Corex opens velocity.toml next to the
 * proxy jar, reads {@code forwarding-secret-file = "..."} out of it, and only then goes to the file
 * that line names for the key itself. No such line, or no velocity.toml, means
 * {@code forwarding.secret}, which is what Velocity ships with.</p>
 *
 * <p>The name has to be quoted to be seen, the way Velocity writes it. A relative name is resolved
 * against the proxy root, an absolute one is taken as it stands.</p>
 *
 * <p>velocity.toml is scanned line by line rather than parsed: one string is wanted out of a file
 * Corex has no other reason to understand, and a TOML parser would be a dependency bought for a
 * single key.</p>
 *
 * @since 1.0.0
 */
public final class ProxySecretResolver {

    private static final String DEFAULT_SECRET_FILE = "forwarding.secret";
    private static final String CONFIG_FILE = "velocity.toml";

    private static final Pattern SECRET_FILE_LINE =
            Pattern.compile("^\\s*forwarding-secret-file\\s*=\\s*[\"'](.+)[\"']\\s*$");

    private ProxySecretResolver() {}

    public static @Nullable NetworkSecret resolve(@NotNull Path dataDirectory, boolean allowProxySecret) {
        return SecretSources.resolve(
                EnvManager.getSecret(SecretSources.COREX_KEY),
                allowProxySecret,
                () -> fromSecretFile(proxyRoot(dataDirectory)));
    }

    private static @Nullable NetworkSecret fromSecretFile(@Nullable Path root) {
        if (root == null) {
            return null;
        }

        Path secretFile = root.resolve(resolveSecretFileName(root));
        if (!Files.isRegularFile(secretFile)) {
            return null;
        }

        try {
            String secret = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (secret.isBlank()) {
                return null;
            }
            return new NetworkSecret(secret, "the Velocity forwarding secret in " + secretFile.getFileName());
        }
        catch (IOException e) {
            CorexLogger.warn("Corex could not read " + secretFile + " for the forwarding secret: "
                    + e.getMessage());
            return null;
        }
    }

    private static String resolveSecretFileName(Path root) {
        Path config = root.resolve(CONFIG_FILE);
        if (!Files.isRegularFile(config)) {
            return DEFAULT_SECRET_FILE;
        }

        try {
            List<String> lines = Files.readAllLines(config, StandardCharsets.UTF_8);
            for (String line : lines) {
                Matcher matcher = SECRET_FILE_LINE.matcher(line);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        }
        catch (IOException e) {
            CorexLogger.warn("Corex could not read " + CONFIG_FILE + ": " + e.getMessage());
        }
        return DEFAULT_SECRET_FILE;
    }

    private static @Nullable Path proxyRoot(Path dataDirectory) {
        Path pluginsFolder = dataDirectory.toAbsolutePath().getParent();
        return pluginsFolder != null ? pluginsFolder.getParent() : null;
    }
}

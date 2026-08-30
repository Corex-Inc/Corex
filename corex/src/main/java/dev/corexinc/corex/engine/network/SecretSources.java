package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The order in which a shared secret is looked for, shared by the proxy and the backends.
 *
 * <p>The two sides read different files at the end of the chain, but everything before that has to
 * be identical. If one side preferred the environment and the other preferred a file, an admin who
 * set both would get two different keys and a network where nothing verifies and nothing says why.
 * Keeping the order in one place is what stops that.</p>
 *
 * <h2>Order</h2>
 * <ol>
 *   <li>{@code CX_NETWORK_SECRET} in secrets.env, the explicit answer.</li>
 *   <li>{@code CX_NETWORK_SECRET} in the environment, for containers where a file is awkward.</li>
 *   <li>{@code VELOCITY_FORWARDING_SECRET} in the environment, which Velocity itself prefers over
 *       its secret file.</li>
 *   <li>Whatever the platform can read off disk: {@code proxies.velocity.secret} on a backend,
 *       the forwarding secret file on the proxy.</li>
 * </ol>
 *
 * <p>Corex only needs every server to land on the <em>same</em> value, not on the value Paper or
 * Velocity happen to use for forwarding. So an environment variable set across the whole network
 * works even on a platform that ignores it for forwarding itself.</p>
 *
 * @since 1.0.0
 */
public final class SecretSources {

    /**
     * The variable that names a secret Corex should use, in secrets.env or in the environment.
     */
    public static final String COREX_KEY = "CX_NETWORK_SECRET";

    /**
     * Velocity's own variable, read so a network that already sets it needs no Corex config.
     */
    public static final String VELOCITY_KEY = "VELOCITY_FORWARDING_SECRET";

    private SecretSources() {}

    /**
     * Walks the chain and returns the first secret found.
     *
     * @param fromSecretsFile  the value of {@link #COREX_KEY} in secrets.env, or null.
     * @param allowProxySecret whether borrowing the proxy's forwarding secret is permitted.
     * @param platformSecret   reads the platform's own secret file, called only if nothing earlier
     *                         matched and borrowing is permitted.
     * @return the secret and where it came from, or null when there is none.
     */
    public static @Nullable NetworkSecret resolve(@Nullable String fromSecretsFile,
                                                  boolean allowProxySecret,
                                                  @NotNull Supplier<@Nullable NetworkSecret> platformSecret) {
        if (isSet(fromSecretsFile)) {
            return new NetworkSecret(fromSecretsFile, COREX_KEY + " in secrets.env");
        }

        String corexEnvironment = System.getenv(COREX_KEY);
        if (isSet(corexEnvironment)) {
            return new NetworkSecret(corexEnvironment, "the " + COREX_KEY + " environment variable");
        }

        if (!allowProxySecret) {
            return null;
        }

        String velocityEnvironment = System.getenv(VELOCITY_KEY);
        if (isSet(velocityEnvironment)) {
            return new NetworkSecret(velocityEnvironment, "the " + VELOCITY_KEY + " environment variable");
        }

        return platformSecret.get();
    }

    private static boolean isSet(@Nullable String value) {
        return value != null && !value.isBlank();
    }
}

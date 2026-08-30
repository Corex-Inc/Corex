package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The outcome of handing a packet to a {@link ProxyTransport}.
 *
 * <p>A transport reports why it could not deliver rather than returning a bare {@code false}, so
 * the calling command can put the actual reason in front of the script author. Plugin messaging
 * fails for mundane reasons a scripter needs to know about, a server with nobody on it being the
 * common one, and silently dropping those is how a network ends up with events that only fire
 * sometimes.</p>
 *
 * @param delivered whether the transport handed the frame off successfully.
 * @param reason    why it did not, or null when it did.
 * @since 1.0.0
 */
public record SendResult(boolean delivered, @Nullable String reason) {

    private static final SendResult SUCCESS = new SendResult(true, null);

    public static @NotNull SendResult success() {
        return SUCCESS;
    }

    public static @NotNull SendResult failed(@NotNull String reason) {
        return new SendResult(false, reason);
    }
}

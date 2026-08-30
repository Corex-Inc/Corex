package dev.corexinc.corex.engine.network;

import dev.corexinc.corex.api.tags.AbstractTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Receives {@link dev.corexinc.corex.engine.network.packets.NetworkMessagePacket} payloads that
 * passed the checks in {@link NetworkManager}.
 *
 * <p>Implemented by the {@code network message} script event, which is a Bukkit listener and so
 * cannot be referenced from the engine directly.</p>
 *
 * @since 1.0.0
 */
public interface NetworkMessageHandler {

    /**
     * Called on the server thread when a message arrives for this server.
     *
     * @param channel the channel name the sender chose.
     * @param data    the payload tag, or null when the sender attached none.
     * @param source  the name of the server the message came from, as stamped by the proxy.
     */
    void onNetworkMessage(@NotNull String channel, @Nullable AbstractTag data, @NotNull String source);
}

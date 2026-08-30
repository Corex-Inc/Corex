package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;

/**
 * A way of getting a Corex packet from this server to the rest of the network.
 *
 * <p>The engine talks to this interface and never to a concrete transport, so the plugin
 * messaging implementation can be swapped for a socket based one without a script or a command
 * changing. The two differ in exactly the way that matters here: plugin messaging rides a player's
 * connection and therefore cannot deliver anything while a server is empty, whereas a socket
 * holds its own connection and can.</p>
 *
 * @since 1.0.0
 */
public interface ProxyTransport {

    @NotNull String name();

    boolean isConnected();

    /**
     * Attempts to deliver one packet.
     *
     * @param packet the packet to send.
     * @return whether it went out, and why not when it did not.
     */
    @NotNull SendResult send(@NotNull CorexPacket packet);
}

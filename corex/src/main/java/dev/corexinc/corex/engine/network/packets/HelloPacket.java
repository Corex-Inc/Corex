package dev.corexinc.corex.engine.network.packets;

import dev.corexinc.corex.engine.network.CorexPacket;
import dev.corexinc.corex.engine.network.PacketBuffer;
import dev.corexinc.corex.engine.network.PacketType;
import org.jetbrains.annotations.NotNull;

/**
 * The first thing a backend sends after opening a socket to the proxy, naming which server it is.
 *
 * <p>Plugin messaging never needs this: a message arrives on a connection the proxy already
 * associates with a backend, so it can read the name off {@code ServerConnection}. A socket is just
 * a socket, and the proxy has no idea who dialled in until it is told.</p>
 *
 * <p>Privileged, which is what makes the claim worth anything. The name decides which server the
 * proxy will route to it and what {@code context.from} reads on every message it relays onward, so
 * an unsigned hello would let anyone who can reach the port impersonate a backend.</p>
 *
 * <p>Not a {@link RoutedPacket}: it is about the connection rather than about a destination, and
 * it never travels any further than the proxy.</p>
 *
 * @since 1.0.0
 */
public class HelloPacket implements CorexPacket {

    private String serverName = "";

    public HelloPacket() {}

    public HelloPacket(@NotNull String serverName) {
        this.serverName = serverName;
    }

    @Override
    public @NotNull PacketType type() {
        return PacketType.HELLO;
    }

    public @NotNull String getServerName() {
        return serverName;
    }

    @Override
    public void write(@NotNull PacketBuffer buffer) {
        buffer.writeString(serverName);
    }

    @Override
    public void read(@NotNull PacketBuffer buffer) {
        serverName = buffer.readString();
    }
}

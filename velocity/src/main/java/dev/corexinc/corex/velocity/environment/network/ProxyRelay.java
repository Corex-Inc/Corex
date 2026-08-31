package dev.corexinc.corex.velocity.environment.network;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.corexinc.corex.engine.network.CorexPacket;
import dev.corexinc.corex.engine.network.ForeignChannelPayload;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.network.ProxyTransport;
import dev.corexinc.corex.engine.network.SendResult;
import dev.corexinc.corex.engine.network.packets.NetworkMessagePacket;
import dev.corexinc.corex.engine.network.packets.RoutedPacket;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The proxy half of the Corex network layer: it routes packets between backends, runs the ones
 * addressed to the proxy itself, and acts as the transport for scripts running on the proxy.
 *
 * <h2>Client sent frames are dropped here</h2>
 * <p>A plugin message on this channel can come from a backend or from a player's own client, and
 * Velocity forwards a client sent one to the backend by default. That would let any modded client
 * post a packet straight into a backend's listener, so a frame whose source is not a
 * {@link ServerConnection} is marked handled and goes no further. Marking it handled is the part
 * that matters: simply returning leaves the default forward result in place.</p>
 *
 * <h2>Two pipes, one router</h2>
 * <p>A backend reaches the proxy either by plugin messaging or by a socket, and the proxy answers
 * on whichever one that backend has. A socket wins when there is one, since it works while the
 * server is empty; otherwise the packet rides a player connection as before. That lets a network
 * move one server at a time rather than all at once.</p>
 *
 * <p>The exception is a foreign plugin channel. That payload is meant for a plugin listening on a
 * Minecraft channel, which the Corex socket is not, so it always goes out as a plugin message.</p>
 *
 * <h2>Why the frame is rebuilt rather than passed through</h2>
 * <p>The proxy overwrites the source field with the name of the server the packet actually arrived
 * on, which is the only reason a backend can trust {@code context.from}. That changes the signed
 * body, so the frame is re-encoded and re-signed on the way out.</p>
 *
 * @since 1.0.0
 */
public final class ProxyRelay implements ProxyTransport {

    /**
     * The plugin messaging channel Corex talks on, matching the backend side.
     */
    public static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("corex:network");

    /**
     * Source name stamped on packets the proxy sends itself, as opposed to ones it relays.
     */
    public static final String PROXY_SOURCE = "proxy";

    private static final int MAX_PLUGIN_MESSAGE_BYTES = 30_000;

    private final ProxyServer server;
    private final Set<String> heardFrom = ConcurrentHashMap.newKeySet();
    private final Set<String> warnedSilent = ConcurrentHashMap.newKeySet();

    @Nullable
    private WebSocketRelayServer webSocket;

    public ProxyRelay(@NotNull ProxyServer server) {
        this.server = server;
    }

    public void init() {
        server.getChannelRegistrar().register(CHANNEL);
        NetworkManager.setTransport(this);
    }

    /**
     * Starts listening for backend sockets.
     *
     * <p>Refuses without a shared secret rather than falling back to an unauthenticated port: a
     * plugin message at least arrives on a connection the proxy already trusts, while anyone who
     * can reach this port could otherwise claim to be a backend.</p>
     *
     * @param address where to listen.
     * @return true when the listener started.
     */
    public boolean startWebSocket(@NotNull InetSocketAddress address) {
        if (!NetworkManager.hasSecret()) {
            CorexLogger.error("Corex will not open the network websocket without a shared secret. "
                    + "Set CX_NETWORK_SECRET in secrets.env on the proxy and on every backend.");
            return false;
        }

        WebSocketRelayServer listener = new WebSocketRelayServer(address, this);
        listener.setDaemon(true);
        listener.start();
        webSocket = listener;
        return true;
    }

    public void shutdown() {
        WebSocketRelayServer listener = webSocket;
        webSocket = null;
        if (listener == null) return;

        try {
            listener.stop(1000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection source)) {
            return;
        }

        CorexPacket packet = NetworkManager.verifyAndDecode(event.getData());
        if (packet == null) return;

        routeFrom(packet, source.getServerInfo().getName());
    }

    /**
     * Routes a packet that arrived from {@code senderName}, over either pipe.
     *
     * @param packet     a packet that came back from {@link NetworkManager#verifyAndDecode}.
     * @param senderName the backend it arrived from.
     */
    public void routeFrom(@NotNull CorexPacket packet, @NotNull String senderName) {
        if (!(packet instanceof RoutedPacket routed)) {
            CorexLogger.warn("Corex packet " + packet.type().name() + " from " + senderName
                    + " carries no routing information, dropped.");
            return;
        }

        routed.setSource(senderName);
        if (heardFrom.add(senderName)) warnedSilent.remove(senderName);

        if (routed.isProxyTarget()) {
            handleHere(routed);
            return;
        }

        SendResult result = dispatch(routed, senderName);
        if (!result.delivered()) {
            CorexLogger.warn("Corex packet from " + senderName + " was not relayed: " + result.reason() + ".");
        }
    }

    @Override
    public @NotNull String name() {
        return "proxy-relay";
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public @NotNull SendResult send(@NotNull CorexPacket packet) {
        if (!(packet instanceof RoutedPacket routed)) {
            return SendResult.failed("packet type " + packet.type().name() + " cannot be routed");
        }

        routed.setSource(PROXY_SOURCE);

        if (routed.isProxyTarget()) {
            handleHere(routed);
            return SendResult.success();
        }
        return dispatch(routed, null);
    }

    private void handleHere(RoutedPacket packet) {
        SchedulerAdapter.get().run(() -> NetworkManager.handleDecoded(packet));
    }

    private SendResult dispatch(RoutedPacket packet, @Nullable String excludedServer) {
        boolean foreignChannel = packet instanceof NetworkMessagePacket message && message.isPluginChannel();
        ChannelIdentifier channel = CHANNEL;
        byte[] frame;

        if (foreignChannel) {
            NetworkMessagePacket message = (NetworkMessagePacket) packet;
            ChannelIdentifier resolved = resolveForeignChannel(message.getChannel());
            if (resolved == null) {
                return SendResult.failed("\"" + message.getChannel()
                        + "\" is not a valid plugin messaging channel, it must read namespace:path");
            }
            channel = resolved;
            frame = ForeignChannelPayload.encode(message.getRawPayload());
        }
        else {
            try {
                frame = NetworkManager.encode(packet);
            }
            catch (RuntimeException e) {
                return SendResult.failed("the packet could not be encoded: " + e.getMessage());
            }
        }

        if (!packet.isBroadcast()) {
            return deliverTo(packet.getTarget(), frame, channel, foreignChannel);
        }

        int delivered = 0;
        for (String target : knownServers()) {
            if (target.equals(excludedServer)) {
                continue;
            }
            if (deliverTo(target, frame, channel, foreignChannel).delivered()) {
                delivered++;
            }
        }

        if (delivered == 0) {
            return SendResult.failed("no other server on the proxy could be reached");
        }
        return SendResult.success();
    }

    private SendResult deliverTo(String target, byte[] frame, ChannelIdentifier channel, boolean foreignChannel) {
        WebSocketRelayServer listener = webSocket;
        if (!foreignChannel && listener != null && listener.sendTo(target, frame)) {
            return SendResult.success();
        }

        if (frame.length > MAX_PLUGIN_MESSAGE_BYTES) {
            return SendResult.failed("the packet is " + frame.length + " bytes, over the "
                    + MAX_PLUGIN_MESSAGE_BYTES + " byte limit for a plugin message, and \"" + target
                    + "\" has no websocket to take it instead");
        }

        Optional<RegisteredServer> destination = server.getServer(target);
        if (destination.isEmpty()) {
            return SendResult.failed("there is no server named \"" + target + "\" on this proxy");
        }
        if (!destination.get().sendPluginMessage(channel, frame)) {
            return SendResult.failed("nobody is connected to \"" + target
                    + "\" and it has no websocket, so there is no connection to deliver on");
        }

        if (!foreignChannel && !heardFrom.contains(target) && warnedSilent.add(target)) {
            CorexLogger.warn("Corex has never heard from \"" + target + "\". The packet went out on a "
                    + "player connection, but if nothing there listens on " + CHANNEL.getId()
                    + " it is dropped without a word. Check that Corex is installed on that server.");
        }
        return SendResult.success();
    }

    private Set<String> knownServers() {
        Set<String> names = new LinkedHashSet<>();
        for (RegisteredServer registered : server.getAllServers()) {
            names.add(registered.getServerInfo().getName());
        }
        WebSocketRelayServer listener = webSocket;
        if (listener != null) {
            names.addAll(listener.connectedServers());
        }
        return names;
    }

    private static @Nullable ChannelIdentifier resolveForeignChannel(String channel) {
        try {
            return MinecraftChannelIdentifier.from(channel);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}

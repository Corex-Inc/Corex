package dev.corexinc.corex.environment.network;

import dev.corexinc.corex.engine.network.CorexPacket;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.network.ProxyTransport;
import dev.corexinc.corex.engine.network.SendResult;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries Corex packets over Minecraft plugin messaging.
 *
 * <h2>What this transport cannot do</h2>
 * <p>A plugin message travels on a player connection, so an empty server can neither send nor
 * receive one. That is not a bug to be worked around here, it is the shape of the transport, and
 * it is why {@link #send} reports the reason back to the script instead of logging and moving on.
 * A setup that needs delivery while a server is empty wants a socket transport behind the same
 * {@link ProxyTransport} interface.</p>
 *
 * @since 1.0.0
 */
public final class PluginMessageTransport implements ProxyTransport, PluginMessageListener {

    /**
     * The plugin messaging channel Corex talks on, registered on both the backend and the proxy.
     */
    public static final String CHANNEL = "corex:network";

    /**
     * Largest frame handed to the connection. The limiting leg is the proxy sending on to a
     * backend, where the payload rides a serverbound packet with a far smaller cap than the one
     * Bukkit enforces locally, so this sits well below both.
     */
    public static final int MAX_FRAME_BYTES = 30_000;

    private final Plugin plugin;

    public PluginMessageTransport(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        Messenger messenger = Bukkit.getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
        NetworkManager.setTransport(this);
    }

    public void shutdown() {
        Messenger messenger = Bukkit.getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    public @NotNull String name() {
        return "plugin-messaging";
    }

    @Override
    public boolean isConnected() {
        return !Bukkit.getOnlinePlayers().isEmpty();
    }

    @Override
    public @NotNull SendResult send(@NotNull CorexPacket packet) {
        byte[] frame;
        try {
            frame = NetworkManager.encode(packet);
        }
        catch (RuntimeException e) {
            return SendResult.failed("the packet could not be encoded: " + e.getMessage());
        }

        if (frame.length > MAX_FRAME_BYTES) {
            return SendResult.failed("the packet is " + frame.length + " bytes, over the "
                    + MAX_FRAME_BYTES + " byte limit for a plugin message");
        }
        if (!isConnected()) {
            return SendResult.failed("no player is online to carry the message off this server");
        }

        SchedulerAdapter.get().run(() -> deliver(frame));
        return SendResult.success();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel,
                                        @NotNull Player player,
                                        byte @NotNull [] message) {
        if (!CHANNEL.equals(channel)) return;
        NetworkManager.handleFrame(message);
    }

    private void deliver(byte[] frame) {
        Player carrier = findCarrier();
        if (carrier == null) {
            CorexLogger.warn("Corex packet dropped: the last player left before it could be sent.");
            return;
        }
        try {
            carrier.sendPluginMessage(plugin, CHANNEL, frame);
        }
        catch (Exception e) {
            CorexLogger.warn("Failed to send a Corex packet: " + e.getMessage());
        }
    }

    private @Nullable Player findCarrier() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            return player;
        }
        return null;
    }
}

package dev.corexinc.corex.environment.network;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.events.implementation.core.ProxyMessageEvent;
import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.protocol.CorexPackets;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundExecuteCommandPacket;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundExecuteScriptPacket;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundMessagePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class ProxyMessagingManager implements PluginMessageListener {

    public static final String CHANNEL = "corex:network";

    private static ProxyMessagingManager instance;

    public void init(Plugin plugin) {
        instance = this;
        CorexPackets.bootstrap();

        var messenger = Bukkit.getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);

        CorexLogger.info("ProxyMessagingManager initialised on channel \"" + CHANNEL + "\".");
    }

    public void shutdown(Plugin plugin) {
        var messenger = Bukkit.getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
        instance = null;
    }

    public static ProxyMessagingManager get() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel,
                                        @NotNull Player player,
                                        byte @NotNull [] message) {
        if (!CHANNEL.equals(channel)) return;

        ByteBuf buf = Unpooled.wrappedBuffer(message);
        try {
            CorexPacket packet = CorexPacket.Codec.decode(buf);
            handleIncoming(packet);
        } catch (Exception e) {
            CorexLogger.warn("Failed to decode CoreX proxy packet: " + e.getMessage());
        } finally {
            buf.release();
        }
    }

    private void handleIncoming(CorexPacket packet) {
        switch (packet) {
            case ClientBoundMessagePacket msg       -> ProxyMessageEvent.fire(msg);
            case ClientBoundExecuteScriptPacket scr -> handleExecuteScript(scr);
            case ClientBoundExecuteCommandPacket cmd -> handleExecuteCommand(cmd);
            default -> CorexLogger.warn("Unhandled incoming CoreX packet: " + packet.type());
        }
    }

    private void handleExecuteScript(ClientBoundExecuteScriptPacket packet) {
        CorexLogger.info("[ProxyMessaging] CB_EXECUTE_SCRIPT received ("
                + packet.getLines().size() + " lines).");
    }

    private void handleExecuteCommand(ClientBoundExecuteCommandPacket packet) {
        CorexLogger.info("[ProxyMessaging] CB_EXECUTE_COMMAND received: " + packet.getCommand());
    }

    public void send(CorexPacket.ServerBound packet) {
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            CorexLogger.warn("Cannot send CoreX proxy packet ("
                    + packet.type().name() + "): no players online.");
            return;
        }

        ByteBuf buf = CorexPacket.Codec.encode(packet, ByteBufAllocator.DEFAULT);
        try {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            carrier.sendPluginMessage(Corex.getInstance(), CHANNEL, bytes);
        } finally {
            buf.release();
        }
    }
}
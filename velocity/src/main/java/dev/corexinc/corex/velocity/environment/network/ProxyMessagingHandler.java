package dev.corexinc.corex.velocity.environment.network;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.protocol.CorexPackets;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundMessagePacket;
import dev.corexinc.corex.shared.network.protocol.serverbound.ServerBoundMessagePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class ProxyMessagingHandler {

    public static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("corex:network");

    private final ProxyServer server;
    private final Logger logger;

    public ProxyMessagingHandler(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public void init() {
        CorexPackets.bootstrap();
        server.getChannelRegistrar().register(CHANNEL);
        logger.info("Corex ProxyMessagingHandler registered on channel \"corex:network\".");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;

        if (!(event.getSource() instanceof ServerConnection source)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String senderName = source.getServerInfo().getName();
        byte[] data       = event.getData();

        ByteBuf buf = Unpooled.wrappedBuffer(data);
        try {
            CorexPacket packet = CorexPacket.Codec.decode(buf);
            handleIncoming(packet, senderName);
        } catch (Exception e) {
            logger.warn("Failed to decode Corex packet from {}: {}", senderName, e.getMessage());
        } finally {
            buf.release();
        }
    }

    private void handleIncoming(CorexPacket packet, String senderName) {
        if (Objects.requireNonNull(packet) instanceof ServerBoundMessagePacket msg) {
            handleMessage(msg, senderName);
        } else {
            logger.warn("Unhandled Corex packet type from {}: {}",
                    senderName, packet.type().name());
        }
    }


    private void handleMessage(ServerBoundMessagePacket incoming, String senderName) {
        ClientBoundMessagePacket outgoing = new ClientBoundMessagePacket(
                incoming.getChannel(),
                incoming.getData(),
                senderName
        );

        byte[] encoded = encode(outgoing);
        if (encoded == null) return;

        String target = incoming.getTargetServer();

        if ("*".equals(target)) {
            for (RegisteredServer rs : server.getAllServers()) {
                if (!rs.getServerInfo().getName().equals(senderName)) {
                    sendToServer(rs, encoded, senderName);
                }
            }
        } else {
            Optional<RegisteredServer> dest = server.getServer(target);
            if (dest.isPresent()) {
                sendToServer(dest.get(), encoded, senderName);
            } else {
                logger.warn("Corex message from {} targets unknown server \"{}\".",
                        senderName, target);
            }
        }
    }

    private void sendToServer(RegisteredServer dest, byte[] data, String from) {
        Collection<com.velocitypowered.api.proxy.Player> players = dest.getPlayersConnected();
        if (players.isEmpty()) {
            logger.debug("Corex: cannot relay message from {} to {} — no players connected.",
                    from, dest.getServerInfo().getName());
            return;
        }
        players.iterator().next()
                .getCurrentServer()
                .ifPresent(conn -> conn.sendPluginMessage(CHANNEL, data));
    }

    private byte[] encode(CorexPacket packet) {
        ByteBuf buf = CorexPacket.Codec.encode(packet, ByteBufAllocator.DEFAULT);
        try {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } catch (Exception e) {
            logger.error("Failed to encode Corex packet {}: {}", packet.type().name(), e.getMessage());
            return null;
        } finally {
            buf.release();
        }
    }
}
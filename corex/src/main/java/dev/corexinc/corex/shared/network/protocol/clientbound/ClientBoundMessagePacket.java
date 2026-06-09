package dev.corexinc.corex.shared.network.protocol.clientbound;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.PacketBufUtils;
import io.netty.buffer.ByteBuf;

import java.util.Map;

public class ClientBoundMessagePacket implements CorexPacket.ClientBound {

    public static final CorexPacketType<ClientBoundMessagePacket> TYPE = CorexPacketType.of(0x11, "CB_MESSAGE", ClientBoundMessagePacket::new);

    private String channel;
    private Map<String, String> data;
    private String sourceServer;

    public ClientBoundMessagePacket() {}

    public ClientBoundMessagePacket(String channel, Map<String, String> data, String sourceServer) {
        this.channel = channel;
        this.data = data;
        this.sourceServer = sourceServer;
    }

    @Override
    public CorexPacketType<ClientBoundMessagePacket> type() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufUtils.writeString(buf, channel);
        PacketBufUtils.writeStringMap(buf, data);
        PacketBufUtils.writeString(buf, sourceServer);
    }

    @Override
    public void read(ByteBuf buf) {
        channel = PacketBufUtils.readString(buf);
        data = PacketBufUtils.readStringMap(buf);
        sourceServer = PacketBufUtils.readString(buf);
    }

    public String getChannel() {
        return channel;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getSourceServer() {
        return sourceServer;
    }
}

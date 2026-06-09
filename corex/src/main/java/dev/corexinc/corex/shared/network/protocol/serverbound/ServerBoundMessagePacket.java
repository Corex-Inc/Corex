package dev.corexinc.corex.shared.network.protocol.serverbound;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.PacketBufUtils;
import io.netty.buffer.ByteBuf;

import java.util.Map;

public class ServerBoundMessagePacket implements CorexPacket.ServerBound {

    public static final CorexPacket.CorexPacketType<ServerBoundMessagePacket> TYPE = CorexPacket.CorexPacketType.of(0x01, "MESSAGE", ServerBoundMessagePacket::new);

    private String targetServer;

    private String channel;

    private Map<String, String> data;

    private String sourceServer;

    public ServerBoundMessagePacket() {}

    public ServerBoundMessagePacket(String targetServer, String channel, Map<String, String> data, String sourceServer) {
        this.targetServer = targetServer;
        this.channel = channel;
        this.data = data;
        this.sourceServer = sourceServer;
    }

    @Override
    public CorexPacket.CorexPacketType<ServerBoundMessagePacket> type() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufUtils.writeString(buf, targetServer);
        PacketBufUtils.writeString(buf, channel);
        PacketBufUtils.writeStringMap(buf, data);
        PacketBufUtils.writeString(buf, sourceServer);
    }

    @Override
    public void read(ByteBuf buf) {
        targetServer = PacketBufUtils.readString(buf);
        channel = PacketBufUtils.readString(buf);
        data = PacketBufUtils.readStringMap(buf);
        sourceServer = PacketBufUtils.readString(buf);
    }

    public String getTargetServer() {
        return targetServer;
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

    public void setSourceServer(String sourceServer) {
        this.sourceServer = sourceServer;
    }
}
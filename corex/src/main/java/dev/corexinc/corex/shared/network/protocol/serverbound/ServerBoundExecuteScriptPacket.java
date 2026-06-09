package dev.corexinc.corex.shared.network.protocol.serverbound;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.PacketBufUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ServerBoundExecuteScriptPacket implements CorexPacket.ServerBound {

    public static final CorexPacket.CorexPacketType<ServerBoundExecuteScriptPacket> TYPE = CorexPacket.CorexPacketType.of(0x00, "EXECUTE_SCRIPT", ServerBoundExecuteScriptPacket::new);

    private String targetServer;

    private List<String> lines;

    @Nullable
    private UUID playerUuid;

    public ServerBoundExecuteScriptPacket() {}

    public ServerBoundExecuteScriptPacket(String targetServer, List<String> lines, @Nullable UUID playerUuid) {
        this.targetServer = targetServer;
        this.lines = lines;
        this.playerUuid = playerUuid;
    }

    @Override
    public CorexPacket.CorexPacketType<ServerBoundExecuteScriptPacket> type() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufUtils.writeString(buf, targetServer);
        PacketBufUtils.writeStringList(buf, lines);
        PacketBufUtils.writeNullableUUID(buf, playerUuid);
    }

    @Override
    public void read(ByteBuf buf) {
        targetServer = PacketBufUtils.readString(buf);
        lines = PacketBufUtils.readStringList(buf);
        playerUuid = PacketBufUtils.readNullableUUID(buf);
    }

    public String getTargetServer() {
        return targetServer;
    }

    public List<String> getLines() {
        return lines;
    }

    @Nullable
    public UUID getPlayerUuid() {
        return playerUuid;
    }
}
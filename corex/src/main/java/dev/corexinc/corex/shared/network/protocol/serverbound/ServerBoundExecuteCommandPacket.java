package dev.corexinc.corex.shared.network.protocol.serverbound;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.PacketBufUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ServerBoundExecuteCommandPacket implements CorexPacket.ServerBound {

    public static final CorexPacket.CorexPacketType<ServerBoundExecuteCommandPacket> TYPE = CorexPacket.CorexPacketType.of(0x02, "EXECUTE_COMMAND", ServerBoundExecuteCommandPacket::new);

    private String targetServer;
    private String command;

    @Nullable
    private UUID playerUuid;

    public ServerBoundExecuteCommandPacket() {}

    public ServerBoundExecuteCommandPacket(String targetServer, String command) {
        this(targetServer, command, null);
    }

    public ServerBoundExecuteCommandPacket(String targetServer, String command, @Nullable UUID playerUuid) {
        this.targetServer = targetServer;
        this.command = command;
        this.playerUuid = playerUuid;
    }

    @Override
    public CorexPacket.CorexPacketType<ServerBoundExecuteCommandPacket> type() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufUtils.writeString(buf, targetServer);
        PacketBufUtils.writeString(buf, command);
        PacketBufUtils.writeNullableUUID(buf, playerUuid);
    }

    @Override
    public void read(ByteBuf buf) {
        targetServer = PacketBufUtils.readString(buf);
        command = PacketBufUtils.readString(buf);
        playerUuid = PacketBufUtils.readNullableUUID(buf);
    }

    public String getTargetServer() {
        return targetServer;
    }

    public String getCommand() {
        return command;
    }

    @Nullable
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isConsoleSender() {
        return playerUuid == null;
    }
}
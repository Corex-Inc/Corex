package dev.corexinc.corex.shared.network.protocol.clientbound;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.PacketBufUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Velocity → Paper: execute a vanilla command on this backend.
 * Mirror of {@link dev.corexinc.corex.shared.network.protocol.serverbound.ServerBoundExecuteCommandPacket}.
 */
public class ClientBoundExecuteCommandPacket implements CorexPacket.ClientBound {

    public static final CorexPacketType<ClientBoundExecuteCommandPacket> TYPE = CorexPacketType.of(0x12, "CB_EXECUTE_COMMAND", ClientBoundExecuteCommandPacket::new);

    private String command;

    @Nullable
    private UUID playerUuid;

    public ClientBoundExecuteCommandPacket() {}

    public ClientBoundExecuteCommandPacket(String command, @Nullable UUID playerUuid) {
        this.command = command;
        this.playerUuid = playerUuid;
    }

    @Override
    public CorexPacketType<ClientBoundExecuteCommandPacket> type() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufUtils.writeString(buf, command);
        PacketBufUtils.writeNullableUUID(buf, playerUuid);
    }

    @Override
    public void read(ByteBuf buf) {
        command = PacketBufUtils.readString(buf);
        playerUuid = PacketBufUtils.readNullableUUID(buf);
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

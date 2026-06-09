package dev.corexinc.corex.shared.network.protocol.clientbound;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.PacketBufUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ClientBoundExecuteScriptPacket implements CorexPacket.ClientBound {

    public static final CorexPacketType<ClientBoundExecuteScriptPacket> TYPE = CorexPacketType.of(0x10, "CB_EXECUTE_SCRIPT", ClientBoundExecuteScriptPacket::new);

    private List<String> lines;

    @Nullable
    private UUID playerUuid;

    public ClientBoundExecuteScriptPacket() {}

    public ClientBoundExecuteScriptPacket(List<String> lines, @Nullable UUID playerUuid) {
        this.lines = lines;
        this.playerUuid = playerUuid;
    }

    @Override
    public CorexPacketType<ClientBoundExecuteScriptPacket> type() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufUtils.writeStringList(buf, lines);
        PacketBufUtils.writeNullableUUID(buf, playerUuid);
    }

    @Override
    public void read(ByteBuf buf) {
        lines = PacketBufUtils.readStringList(buf);
        playerUuid = PacketBufUtils.readNullableUUID(buf);
    }

    public List<String> getLines() {
        return lines;
    }

    @Nullable
    public UUID getPlayerUuid() {
        return playerUuid;
    }
}

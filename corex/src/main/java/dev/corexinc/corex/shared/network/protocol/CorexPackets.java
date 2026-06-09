package dev.corexinc.corex.shared.network.protocol;

import dev.corexinc.corex.shared.network.CorexPacket;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundExecuteCommandPacket;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundExecuteScriptPacket;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundMessagePacket;
import dev.corexinc.corex.shared.network.protocol.serverbound.ServerBoundExecuteCommandPacket;
import dev.corexinc.corex.shared.network.protocol.serverbound.ServerBoundExecuteScriptPacket;
import dev.corexinc.corex.shared.network.protocol.serverbound.ServerBoundMessagePacket;

public final class CorexPackets {

    private CorexPackets() {}

    public static final CorexPacket.CorexPacketType<ServerBoundExecuteScriptPacket> EXECUTE_SCRIPT = ServerBoundExecuteScriptPacket.TYPE;      // 0x00
    public static final CorexPacket.CorexPacketType<ServerBoundMessagePacket> MESSAGE = ServerBoundMessagePacket.TYPE;                         // 0x01
    public static final CorexPacket.CorexPacketType<ServerBoundExecuteCommandPacket> EXECUTE_COMMAND = ServerBoundExecuteCommandPacket.TYPE;   // 0x02

    public static final CorexPacket.CorexPacketType<ClientBoundExecuteScriptPacket> CB_EXECUTE_SCRIPT = ClientBoundExecuteScriptPacket.TYPE;    // 0x10
    public static final CorexPacket.CorexPacketType<ClientBoundMessagePacket> CB_MESSAGE = ClientBoundMessagePacket.TYPE;                       // 0x11
    public static final CorexPacket.CorexPacketType<ClientBoundExecuteCommandPacket> CB_EXECUTE_COMMAND = ClientBoundExecuteCommandPacket.TYPE; // 0x12

    public static void bootstrap() {}
}
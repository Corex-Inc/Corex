package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.network.ProxyMessagingManager;
import dev.corexinc.corex.shared.network.protocol.serverbound.ServerBoundMessagePacket;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class NetworkMessageCommand implements AbstractCommand {

    private static final String DEFAULT_TARGET = "*";

    @Override
    public @NonNull String getName() {
        return "message";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<channel>] (to:<server>|*) (data:<map>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public boolean setCanBeWaitable() {
        return false;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String channel = instruction.getLinear(0, queue);
        String target  = instruction.getPrefix("to", queue);
        if (target == null) target = DEFAULT_TARGET;

        Map<String, String> data = resolveData(instruction, queue);

        Debugger.report(queue, instruction,
                "channel", channel,
                "to", target,
                "data", data.toString());

        if (channel == null || channel.isBlank()) {
            Debugger.echoError(queue, "message command requires a channel name as the first argument.");
            return;
        }

        if (!ProxyMessagingManager.isAvailable()) {
            Debugger.echoError(queue, "Cannot send proxy message: ProxyMessagingManager is not initialised. "
                    + "Is this server connected to a Velocity proxy?");
            return;
        }

        ServerBoundMessagePacket packet =
                new ServerBoundMessagePacket(target, channel, data, "");

        ProxyMessagingManager.get().send(packet);
    }

    private Map<String, String> resolveData(Instruction instruction, ScriptQueue queue) {
        var rawTag = instruction.getPrefixObject("data", queue);
        if (rawTag == null) return Map.of();

        String raw = rawTag.identify();
        if (raw.isBlank()) return Map.of();

        Map<String, String> result = new HashMap<>();
        for (String pair : raw.split("\\|")) {
            int eq = pair.indexOf('=');
            if (eq < 1) continue;
            result.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        return result;
    }
}
package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class RepeatCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "repeat"; }
    @Override public @NonNull List<String> getAlias() { return List.of(); }
    @Override public @NonNull String getSyntax() { return "- repeat [<count>|stop|next] (from:<number>) (as:<var>)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 3; }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String action = instruction.getLinear(0, queue);

        if (action != null && action.equalsIgnoreCase("stop")) { queue.skipFrame(true); return; }
        if (action != null && action.equalsIgnoreCase("next")) { queue.skipFrame(false); return; }
        if (instruction.innerBlock == null || instruction.innerBlock.length == 0) return;

        int times = new ElementTag(action).asInt();
        if (times <= 0) return;

        String fromStr = instruction.getPrefix("from", queue);
        int from = fromStr != null ? new ElementTag(fromStr).asInt() : 1;
        String asVar = instruction.getPrefix("as", queue) != null ? instruction.getPrefix("as", queue) : "loopIndex";

        int max = from + times - 1;
        int[] current = { from };

        queue.define(asVar, new ElementTag(current[0]));

        queue.pushFrame("repeat_loop", instruction.innerBlock,
                () -> {
                    queue.setBroken(false);
                    queue.define(asVar, null);
                },
                () -> {
                    if (queue.isBroken()) return false;
                    current[0]++;
                    if (current[0] > max) return false;
                    queue.define(asVar, new ElementTag(current[0]));
                    return true;
                }
        );
    }
}
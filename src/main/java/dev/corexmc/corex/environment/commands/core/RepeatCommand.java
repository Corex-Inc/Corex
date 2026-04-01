package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

public class RepeatCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "repeat"; }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String action = instruction.getLinear(0, queue);

        if (action != null && action.equalsIgnoreCase("stop")) {
            queue.skipFrame(true);
            return;
        }
        if (action != null && action.equalsIgnoreCase("next")) {
            queue.skipFrame(false);
            return;
        }

        if (instruction.innerBlock == null || instruction.innerBlock.length == 0) return;

        int times = new ElementTag(action).asInt();
        int from = instruction.getPrefix("from", queue) != null ? new ElementTag(instruction.getPrefix("from", queue)).asInt() : 1;
        String asVar = instruction.getPrefix("as", queue) != null ? instruction.getPrefix("as", queue) : "value";

        int max = from + times - 1;

        runIteration(queue, instruction.innerBlock, from, max, asVar);
    }

    private void runIteration(ScriptQueue queue, Instruction[] block, int current, int max, String asVar) {
        if (current > max) {
            queue.define(asVar, null);
            return;
        }

        queue.define(asVar, new ElementTag(current));

        queue.pushFrame(block, () -> {
            if (queue.isBroken()) {
                queue.setBroken(false);
                queue.define(asVar, null);
                return;
            }
            runIteration(queue, block, current + 1, max, asVar);
        });
    }

    @Override public @NonNull String getSyntax() { return "- repeat [<amount>|stop|next] (from:<#>) (as:<name>)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 3; }
}
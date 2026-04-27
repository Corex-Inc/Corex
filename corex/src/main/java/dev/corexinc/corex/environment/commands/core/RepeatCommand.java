package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import java.util.List;

public class RepeatCommand implements AbstractCommand {

    @Override public @NonNull String getName() {
        return "repeat";
    }

    @Override public @NonNull List<String> getAlias() {
        return List.of();
    }

    @Override public @NonNull String getSyntax() {
        return "[<count>|break|continue] (from:<number>) (as:<var>)";
    }

    @Override public int getMinArgs() {
        return 1;
    }

    @Override public int getMaxArgs() {
        return 3;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String action = instruction.getLinear(0, queue);

        if (action != null && action.equals("break")) {
            queue.skipFrame(true);
            return;
        }

        if (action != null && action.equals("continue")) {
            queue.skipFrame(false);
            return;
        }

        if (instruction.innerBlock == null || instruction.innerBlock.length == 0) return;

        ElementTag countTag = new ElementTag(action);
        if (!countTag.isInt()) return;
        int times = countTag.asInt();
        if (times <= 0) return;

        String fromPrefix = instruction.getPrefix("from", queue);
        final int startFrom = (fromPrefix != null) ? new ElementTag(fromPrefix).asInt() : 1;

        String rawAs = instruction.getPrefix("as", queue);
        final String asVar = (rawAs != null) ? rawAs.replace(":", "").trim() : "loopIndex";

        final int max = startFrom + times - 1;
        final String stateKey = "rep_idx_" + queue.getDepth();

        queue.define(asVar, new ElementTag(startFrom));
        queue.setTempData(stateKey, startFrom + 1);

        Debugger.report(queue, instruction,
                "Times", times,
                "From", startFrom,
                "AsDefinition", asVar
        );

        queue.pushFrame("repeat_loop", instruction.innerBlock,
                () -> {
                    queue.setBroken(false);
                    queue.define(asVar, null);
                    queue.setTempData(stateKey, null);
                },
                () -> {
                    if (queue.isBroken()) return false;

                    Object nextValObj = queue.getTempData(stateKey);
                    int nextVal = (nextValObj instanceof Integer) ? (int) nextValObj : startFrom + 1;

                    if (nextVal > max) {
                        return false;
                    }

                    queue.define(asVar, new ElementTag(nextVal));

                    queue.setTempData(stateKey, nextVal + 1);

                    return true;
                }
        );
    }
}
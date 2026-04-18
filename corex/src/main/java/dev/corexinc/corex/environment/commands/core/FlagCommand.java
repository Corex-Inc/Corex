package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class FlagCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "flag"; }
    @Override public @NonNull List<String> getAlias() { return List.of(); }
    @Override public @NonNull String getSyntax() { return "[<target>] [<flagName>(:action)] [<value>] (expire:<duration>)"; }
    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 3; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        AbstractTag targetObj = instruction.getLinearObject(0, queue);
        if (!(targetObj instanceof Flaggable flaggable)) {
            Debugger.error(queue, "'<yellow>" + (targetObj != null ? targetObj.identify() : "null") + "</yellow>' does not support flags!", queue.getDepth());
            return;
        }

        AbstractFlagTracker tracker = flaggable.getFlagTracker();
        if (tracker == null) {
            Debugger.error(queue, "Flag tracker is not available for this object!", queue.getDepth());
            return;
        }

        String rawArg = instruction.getLinear(1, queue);
        if (rawArg == null) return;

        int colonIndex = findColonOutsideBrackets(rawArg);
        String keyPath;
        AbstractDataAction action = null;
        String param = null;

        if (colonIndex < 0) {
            keyPath = rawArg;
        } else {
            keyPath = rawArg.substring(0, colonIndex);
            String actionStr = rawArg.substring(colonIndex + 1);

            action = Corex.getInstance().getRegistry().findAction(actionStr);
            if (action != null) {
                param = action.extractParam(actionStr);
            } else {
                Debugger.error(queue, "Unknown DataAction in line: " + actionStr, queue.getDepth());
                return;
            }
        }

        AbstractTag valueObj = instruction.getLinearObject(2, queue);

        long durationMs = 0;
        AbstractTag expireTag = instruction.getPrefixObject("expire", queue);
        if (expireTag instanceof DurationTag dt) {
            durationMs = dt.getMilliseconds();
        }

        AbstractTag finalValue;

        if (action == null) {
            finalValue = valueObj;
        } else {
            AbstractTag currentFlag = tracker.getFlag(keyPath);
            finalValue = action.apply(currentFlag, param, valueObj, queue);
        }

        tracker.setFlag(keyPath, finalValue, durationMs);

        Debugger.report(queue, instruction,
                "Target", targetObj.identify(),
                "Flag", keyPath,
                "Value", finalValue != null ? finalValue.identify() : "null",
                "Expire", durationMs > 0 ? durationMs + "ms" : "never"
        );
    }

    private static int findColonOutsideBrackets(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == ':' && depth == 0) return i;
        }
        return -1;
    }
}
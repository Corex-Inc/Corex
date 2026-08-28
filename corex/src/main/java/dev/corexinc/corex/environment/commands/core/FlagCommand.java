package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Flag
 * @Syntax flag [<object>] [<name>(:action)] [<value>] (expire:<duration>)
 * @RequiredArgs 2
 * @MaxArgs 4
 * @ShortDescription Sets a persistent flag on an object.
 *
 * @Description
 * Stores a named value on the given object (player, server, entity, etc).
 * Flags persist across script queues and, depending on the object type, across server restarts.
 *
 * Append a data action after a colon to modify the existing value instead of replacing it
 * (e.g. "flag <player> score:++").
 *
 * Use dot-notation to store nested data inside a map flag (e.g. "flag <player> stats.kills 5").
 *
 * Providing only a flag name with no value sets the flag to true, so "flag <player> isVip"
 * is the same as "flag <player> isVip true".
 *
 * Optionally specify "expire:<duration>" to automatically remove the flag after the given time.
 * When a flag expires, the FlagExpireEvent fires and scripts listening to it can intercept removal.
 *
 * To remove a flag, provide no value and use the "!" action or set it to null.
 *
 * @Tags
 * <object.flag[name]>          - returns the flag value.
 * <object.hasFlag[name]>       - returns an ElementTag(Boolean).
 * <object.flagExpiry[name]>    - returns a DurationTag of time remaining.
 *
 * @Usage
 * // Set a flag on the player.
 * - flag <player> isVip true
 *
 * @Usage
 * // Same thing - a bare flag name defaults to true.
 * - flag <player> isVip
 *
 * @Usage
 * // Increment a numeric flag.
 * - flag <player> kills:++
 *
 * @Usage
 * // Set a flag that expires after 1 hour.
 * - flag <player> boost true expire:1h
 *
 * @Usage
 * // Remove a flag.
 * - flag <player> isVip:!
 */
public class FlagCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "flag";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of();
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<target>] [<flagName>(:action)] (<value>) (expire:<duration>)";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ListTag.class)
            .requireLinear(1, ElementTag.class)
            .optionalLinear(2, ElementTag.class, null)
            .optionalPrefix("expire", DurationTag.class)
            .build();

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        ArgumentSet args = SCHEMA.bind(instruction, queue);

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

            action = ScriptManager.getRegistry().findAction(actionStr);
            if (action != null) {
                param = action.extractParam(actionStr);
            } else {
                Debugger.error(queue, "Unknown DataAction in line: " + actionStr, queue.getDepth());
                return;
            }
        }

        AbstractTag valueObj = instruction.getLinearObject(2, queue);

        long durationMs = 0;
        DurationTag expire = args.prefix("expire");
        if (expire != null) {
            durationMs = expire.getMilliseconds();
        }

        AbstractTag finalValue;

        if (action == null) {
            finalValue = valueObj != null ? valueObj : new ElementTag(true);
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
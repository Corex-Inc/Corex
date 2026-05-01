package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

/* @doc command
 *
 * @Name Foreach
 * @Syntax foreach [<object>|break|continue] (as:<name>) (key:<name>): [<commands>]
 * @RequiredArgs 1
 * @MaxArgs 3
 * @ShortDescription Loops through a ListTag or MapTag.
 *
 * @Description
 * Loops through all items in a ListTag or all pairs in a MapTag.
 * To get the number of loops so far, use <[loopIndex]>.
 *
 * When iterating a ListTag, the value is stored in <[value]>. Use "as:<name>" to change this.
 * When iterating a MapTag, the key is stored in <[key]>. Use "key:<name>" to change this.
 *
 * To break out of the loop early, use - foreach break
 * To skip to the next iteration, use - foreach continue
 */
public class ForeachCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "foreach";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of();
    }

    @Override
    public @NonNull String getSyntax() {
        return "- foreach [<list>|<map>|break|continue] (as:<var>) (key:<var>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        AbstractTag targetObj = instruction.getLinearObject(0, queue);

        if (targetObj == null) {
            Debugger.echoError(queue, "Foreach target (or break/continue) must be specified.");
            return;
        }

        if (targetObj instanceof ElementTag) {
            String action = targetObj.identify();
            if (action.equalsIgnoreCase("break")) { queue.skipFrame(true); return; }
            if (action.equalsIgnoreCase("continue")) { queue.skipFrame(false); return; }
        }

        if (instruction.innerBlock == null || instruction.innerBlock.length == 0) {
            Debugger.echoError(queue, "Foreach command requires an inner block of instructions!");
            return;
        }

        String rawAs = instruction.getPrefix("as", queue);
        final String asVar = (rawAs != null && !rawAs.isBlank()) ? rawAs : "value";

        String rawKey = instruction.getPrefix("key", queue);
        final String keyVar = (rawKey != null && !rawKey.isBlank()) ? rawKey : "key";

        final boolean isMap = targetObj instanceof MapTag;

        BooleanSupplier loopCondition;

        Runnable onFinish = () -> {
            queue.setBroken(false);
            queue.define("loopIndex", null);
            queue.define(asVar, null);
            if (isMap) queue.define(keyVar, null);
        };

        final int[] stateIdx = new int[] { 2 };

        if (isMap) {
            final MapTag mt = (MapTag) targetObj;

            if (mt.keySet().isEmpty()) return;

            final Iterator<String> iterator = mt.keySet().iterator();
            String firstKey = iterator.next();

            queue.define("loopIndex", new ElementTag(1));
            queue.define(keyVar, new ElementTag(firstKey));
            queue.define(asVar, mt.getObject(firstKey));

            loopCondition = () -> {
                if (queue.isBroken()) return false;
                if (!iterator.hasNext()) return false;

                queue.define("loopIndex", new ElementTag(stateIdx[0]++));

                String key = iterator.next();
                queue.define(keyVar, new ElementTag(key));
                queue.define(asVar, mt.getObject(key));
                return true;
            };

        } else {
            ListTag lt = (targetObj instanceof ListTag) ? (ListTag) targetObj : new ListTag(targetObj.identify());
            List<AbstractTag> items = lt.getList();

            if (items.isEmpty()) return;

            final Iterator<AbstractTag> iterator = items.iterator();

            queue.define("loopIndex", new ElementTag(1));
            queue.define(asVar, iterator.next());

            loopCondition = () -> {
                if (queue.isBroken()) return false;
                if (!iterator.hasNext()) return false;

                queue.define("loopIndex", new ElementTag(stateIdx[0]++));
                queue.define(asVar, iterator.next());
                return true;
            };
        }

        Debugger.report(queue, instruction,
                "Type", isMap ? "Map" : "List",
                "AsVar", asVar,
                isMap ? "KeyVar" : null, isMap ? keyVar : null
        );

        queue.pushFrame("foreach_loop", instruction.innerBlock, onFinish, loopCondition);
    }
}
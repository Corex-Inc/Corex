package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.SlotAware;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.SlotTable;
import dev.corexinc.corex.engine.compiler.args.StaticArg;
import dev.corexinc.corex.engine.queue.MutableDefinition;
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
public class ForeachCommand implements AbstractCommand, SlotAware {

    private record LoopSlots(String asVar, int valueSlot, String keyVar, int keySlot, int indexSlot) {}

    private static String prefixVar(Instruction instruction, String prefix, String fallback) {
        CompiledArgument raw = instruction.prefixArgs.get(prefix);
        if (raw == null) return fallback;
        if (!(raw instanceof StaticArg staticArg)) return null;
        String value = staticArg.evaluate(null).identify();
        return value.isBlank() ? fallback : value;
    }

    @Override
    public @NonNull List<String> writtenDefinitions(@NonNull Instruction instruction) {
        String asVar = prefixVar(instruction, "as", "value");
        String keyVar = prefixVar(instruction, "key", "key");
        if (asVar == null || keyVar == null) return List.of();
        return List.of(asVar, keyVar, "loopIndex");
    }

    @Override
    public void bindSlots(@NonNull Instruction instruction, @NonNull SlotTable table) {
        String asVar = prefixVar(instruction, "as", "value");
        String keyVar = prefixVar(instruction, "key", "key");
        if (asVar == null || keyVar == null) return;
        instruction.slotData = new LoopSlots(asVar, table.indexOf(asVar),
                keyVar, table.indexOf(keyVar), table.indexOf("loopIndex"));
    }

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
        return "[<list>|<map>|break|continue] (as:<var>) (key:<var>)";
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
    public boolean isAsyncSafe() {
        return true;
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

        final String asVar;
        final String keyVar;
        final int valueSlot;
        final int keySlot;
        final int indexSlot;

        if (instruction.slotData instanceof LoopSlots cached && instruction.slots == queue.slotTable()) {
            asVar = cached.asVar();
            keyVar = cached.keyVar();
            valueSlot = cached.valueSlot();
            keySlot = cached.keySlot();
            indexSlot = cached.indexSlot();
        } else {
            String rawAs = instruction.getPrefix("as", queue);
            asVar = (rawAs != null && !rawAs.isBlank()) ? rawAs : "value";
            String rawKey = instruction.getPrefix("key", queue);
            keyVar = (rawKey != null && !rawKey.isBlank()) ? rawKey : "key";
            valueSlot = SlotTable.NO_SLOT;
            keySlot = SlotTable.NO_SLOT;
            indexSlot = SlotTable.NO_SLOT;
        }

        final boolean isMap = targetObj instanceof MapTag;

        BooleanSupplier loopCondition;

        final AbstractTag shadowedIndex = queue.rawDefinition(indexSlot, "loopIndex");
        final AbstractTag shadowedValue = queue.rawDefinition(valueSlot, asVar);
        final AbstractTag shadowedKey = isMap ? queue.rawDefinition(keySlot, keyVar) : null;

        Runnable onFinish = () -> {
            queue.setBroken(false);
            queue.setDefinition(indexSlot, "loopIndex", shadowedIndex);
            queue.setDefinition(valueSlot, asVar, shadowedValue);
            if (isMap) queue.setDefinition(keySlot, keyVar, shadowedKey);
        };

        final MutableDefinition.OfInt index = new MutableDefinition.OfInt(1);

        if (isMap) {
            final MapTag mt = (MapTag) targetObj;

            if (mt.keySet().isEmpty()) return;

            final Iterator<String> iterator = mt.keySet().iterator();
            String firstKey = iterator.next();

            final MutableDefinition.OfTag key = new MutableDefinition.OfTag(new ElementTag(firstKey));
            final MutableDefinition.OfTag value = new MutableDefinition.OfTag(mt.getObject(firstKey));

            queue.setDefinition(indexSlot, "loopIndex", index);
            queue.setDefinition(keySlot, keyVar, key);
            queue.setDefinition(valueSlot, asVar, value);

            loopCondition = () -> {
                if (queue.isBroken()) return false;
                if (!iterator.hasNext()) return false;

                index.value++;

                String nextKey = iterator.next();
                key.current = new ElementTag(nextKey);
                value.current = mt.getObject(nextKey);

                if (queue.rawDefinition(indexSlot, "loopIndex") != index) queue.setDefinition(indexSlot, "loopIndex", index);
                if (queue.rawDefinition(keySlot, keyVar) != key) queue.setDefinition(keySlot, keyVar, key);
                if (queue.rawDefinition(valueSlot, asVar) != value) queue.setDefinition(valueSlot, asVar, value);
                return true;
            };

        } else {
            ListTag lt = (targetObj instanceof ListTag) ? (ListTag) targetObj : new ListTag(targetObj.identify());
            List<AbstractTag> items = lt.getList();

            if (items.isEmpty()) return;

            final Iterator<AbstractTag> iterator = items.iterator();

            final MutableDefinition.OfTag value = new MutableDefinition.OfTag(iterator.next());

            queue.setDefinition(indexSlot, "loopIndex", index);
            queue.setDefinition(valueSlot, asVar, value);

            loopCondition = () -> {
                if (queue.isBroken()) return false;
                if (!iterator.hasNext()) return false;

                index.value++;
                value.current = iterator.next();

                if (queue.rawDefinition(indexSlot, "loopIndex") != index) queue.setDefinition(indexSlot, "loopIndex", index);
                if (queue.rawDefinition(valueSlot, asVar) != value) queue.setDefinition(valueSlot, asVar, value);
                return true;
            };
        }

        if (Debugger.shouldReport(queue)) {
            Debugger.report(queue, instruction,
                    "Type", isMap ? "Map" : "List",
                    "AsVar", asVar,
                    isMap ? "KeyVar" : null, isMap ? keyVar : null
            );
        }

        queue.pushFrame("foreach_loop", instruction.innerBlock, onFinish, loopCondition);
    }
}
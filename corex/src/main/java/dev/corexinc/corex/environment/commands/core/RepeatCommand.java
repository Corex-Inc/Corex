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
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Repeat
 * @Syntax repeat [<amount>|break|continue] (from:<#>) (as:<name>): [<commands>]
 * @RequiredArgs 1
 * @MaxArgs 3
 * @ShortDescription Runs a series of braced commands several times.
 *
 * @Implements Repeat
 *
 * @Description
 * Loops through a series of braced commands a specified number of times.
 * To get the number of loops so far, you can use <[loopIndex]>.
 *
 * Optionally, specify "as:<name>" to change the definition name to something other than "loopIndex".
 *
 * Optionally, to specify a starting index, use "from:<#>". Note that the "amount" input is how many loops will happen, not an end index.
 * The default "from" index is "1". Note that the value you give to "from" will be the loopIndex of the first loop.
 *
 * To break a repeat loop, do - repeat break
 *
 * To jump immediately to the next number in the loop, do - repeat continue
 *
 * @Tags
 * <[loopIndex]> - Returns the number of loops so far
 *
 * @Usage
 * // Use to loop through a command five times.
 * - repeat 5:
 *     - narrate "Narrated Number <[loopIndex]>"
 *
 * @Usage
 * // Use to announce the numbers: 1, 2, 3, 4, 5.
 * - repeat 5 as:number:
 *     - narrate "I can count! <[number]>"
 *
 * @Usage
 * // Use to narrate the numbers: 21, 22, 23, 24, 25.
 * - repeat 5 from:21:
 *     - narrate "Narrated Number <[loopIndex]>"
 *
 * @Usage
 * // Use to stop loop if number equals 11.
 * - repeat 100:
 *     - if <[loopIndex]> == 11:
 *         - repeat break // Breaks a repeat loop
 *
 * @Usage
 * // Use to skip iteration.
 * - repeat 10:
 *     - if <[loopIndex]> == 4:
 *         - narrate "<&c>Skipped <[loopIndex]>!"
 *         - repeat continue // We jump to the next iteration
 *     - narrate "The number is <[loopIndex]>!"
 */
public class RepeatCommand implements AbstractCommand, SlotAware {

    private record LoopSlots(String asVar, int slot) {}

    static String resolveLoopVar(Instruction instruction, String fallback) {
        CompiledArgument raw = instruction.prefixArgs.get("as");
        if (raw == null) return fallback;
        if (!(raw instanceof StaticArg staticArg)) return null;
        String value = staticArg.evaluate(null).identify();
        return value.isBlank() ? fallback : value;
    }

    @Override
    public @NonNull List<String> writtenDefinitions(@NonNull Instruction instruction) {
        String asVar = resolveLoopVar(instruction, "loopIndex");
        return asVar != null ? List.of(asVar) : List.of();
    }

    @Override
    public void bindSlots(@NonNull Instruction instruction, @NonNull SlotTable table) {
        String asVar = resolveLoopVar(instruction, "loopIndex");
        if (asVar != null) instruction.slotData = new LoopSlots(asVar, table.indexOf(asVar));
    }

    @Override
    public @NonNull String getName() {
        return "repeat";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of();
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<count>|break|continue] (from:<number>) (as:<var>)";
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
        String actionRaw = instruction.getLinear(0, queue);

        if (actionRaw == null) {
            Debugger.echoError(queue, "Repeat amount (or break/continue) must be specified.");
            return;
        }

        if (actionRaw.equalsIgnoreCase("break")) {
            queue.skipFrame(true);
            return;
        }

        if (actionRaw.equalsIgnoreCase("continue")) {
            queue.skipFrame(false);
            return;
        }

        if (instruction.innerBlock == null || instruction.innerBlock.length == 0) {
            Debugger.echoError(queue, "Repeat command requires an inner block of instructions!");
            return;
        }

        ElementTag countTag = new ElementTag(actionRaw);
        if (!countTag.isInt() && !countTag.isDouble()) {
            Debugger.echoError(queue, "Repeat amount must be a valid number, got: " + actionRaw);
            return;
        }

        int times = countTag.asInt();
        if (times <= 0) return;

        int startFrom = 1;
        String fromRaw = instruction.getPrefix("from", queue);
        if (fromRaw != null) {
            ElementTag fromTag = new ElementTag(fromRaw);
            if (!fromTag.isInt() && !fromTag.isDouble()) {
                Debugger.echoError(queue, "Invalid 'from' value, expected a number, got: " + fromRaw);
                return;
            }
            startFrom = fromTag.asInt();
        }

        final String asVar;
        final int slot;

        if (instruction.slotData instanceof LoopSlots cached && instruction.slots == queue.slotTable()) {
            asVar = cached.asVar();
            slot = cached.slot();
        } else {
            String rawAs = instruction.getPrefix("as", queue);
            asVar = (rawAs != null && !rawAs.isBlank()) ? rawAs : "loopIndex";
            slot = SlotTable.NO_SLOT;
        }

        final int max = startFrom + times - 1;
        final MutableDefinition.OfInt index = new MutableDefinition.OfInt(startFrom);
        final AbstractTag shadowed = queue.rawDefinition(slot, asVar);

        queue.setDefinition(slot, asVar, index);

        if (Debugger.shouldReport(queue)) {
            Debugger.report(queue, instruction,
                    "Times", times,
                    "From", startFrom,
                    "AsDefinition", asVar
            );
        }

        queue.pushFrame("repeat_loop", instruction.innerBlock,
                () -> {
                    queue.setBroken(false);
                    queue.setDefinition(slot, asVar, shadowed);
                },
                () -> {
                    if (queue.isBroken()) return false;

                    int nextVal = index.value + 1;
                    if (nextVal > max) {
                        return false;
                    }

                    index.value = nextVal;
                    if (queue.rawDefinition(slot, asVar) != index) queue.setDefinition(slot, asVar, index);
                    return true;
                }
        );
    }
}
package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
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
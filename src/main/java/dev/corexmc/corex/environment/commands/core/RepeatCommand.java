package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

/* @[command]
 *
 * @Name Repeat
 * @Syntax repeat [<amount>|stop|next] (from:<#>) (as:<name>): [<commands>]
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
 * To stop a repeat loop, do - repeat stop
 *
 * To jump immediately to the next number in the loop, do - repeat next
 *
 * @Tags
 * <[loopIndex]> to get the number of loops so far
 *
 * @Usage
 * // Use to loop through a command five times.
 * - repeat 5:
 *     - announce "Announce Number <[loopIndex]>"
 *
 * @Usage
 * // Use to announce the numbers: 1, 2, 3, 4, 5.
 * - repeat 5 as:number:
 *     - announce "I can count! <[number]>"
 *
 * @Usage
 * // Use to announce the numbers: 21, 22, 23, 24, 25.
 * - repeat 5 from:21:
 *     - announce "Announce Number <[loopIndex]>"
 */
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
        String asVar = instruction.getPrefix("as", queue) != null ? instruction.getPrefix("as", queue) : "loopIndex";

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

    @Override public @NonNull String getSyntax() { return "[<amount>|stop|next] (from:<#>) (as:<name>)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 3; }
}
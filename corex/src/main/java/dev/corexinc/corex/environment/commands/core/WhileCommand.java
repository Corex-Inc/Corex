package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.utils.scripts.ConditionCompiler;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name While
 * @Syntax while [<condition>|break|continue] (as:<name>): [<commands>]
 * @RequiredArgs 1
 * @MaxArgs -1
 * @ShortDescription Runs a series of braced commands as long as a condition is true.
 *
 * @Implements While
 *
 * @Description
 * Loops through a series of braced commands repeatedly as long as the specified condition evaluates to true.
 * The condition uses the exact same syntax as the 'if' command.
 *
 * To get the number of loops so far, you can use <[loopIndex]>.
 * Optionally, specify "as:<name>" to change the definition name to something other than "loopIndex".
 *
 * The condition is evaluated before the first iteration, and then re-evaluated at the end of every loop.
 *
 * To break out of the loop early, use - while break
 * To skip to the next iteration, use - while continue
 *
 * Note: A hard limit of 100,000 iterations is enforced to prevent server freezes from infinite loops.
 *
 * @Tags
 * <[loopIndex]> - Returns the number of loops so far
 *
 * @Usage
 * // Loop until a player's health is full
 * - while <player.health> < <player.maxHealth>:
 *     - heal 1
 *     - wait 10t
 *
 * @Usage
 * // Loop using the loopIndex in the condition
 * - while <[loopIndex]> < 5:
 *     - narrate "This will run exactly 4 times! Currently on: <[loopIndex]>"
 */
public class WhileCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "while";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of();
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<condition>|break|continue] (as:<var>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return -1;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        if (instruction.innerBlock == null || instruction.innerBlock.length == 0) {
            String actionRaw = instruction.getLinear(0, queue);
            if (actionRaw != null) {
                if (actionRaw.equalsIgnoreCase("break")) {
                    queue.skipFrame(true);
                    return;
                } else if (actionRaw.equalsIgnoreCase("continue")) {
                    queue.skipFrame(false);
                    return;
                }
            }
            Debugger.echoError(queue, "While command requires an inner block of instructions (unless using 'while break' or 'while continue')!");
            return;
        }

        ConditionCompiler.Condition condition = (ConditionCompiler.Condition) instruction.customData;
        if (condition == null) {
            condition = ConditionCompiler.compile(instruction.linearArgs);
            instruction.customData = condition;
        }

        String rawAs = instruction.getPrefix("as", queue);
        final String asVar = (rawAs != null && !rawAs.isBlank()) ? rawAs : "loopIndex";

        queue.define(asVar, new ElementTag(1));

        boolean initialResult = condition.evaluate(queue);

        Debugger.report(queue, instruction,
                "Initial Condition", String.valueOf(initialResult),
                "AsDefinition", asVar
        );

        if (!initialResult) {
            queue.define(asVar, null);
            return;
        }

        final ConditionCompiler.Condition finalCondition = condition;

        final int[] iterationCounter = new int[] { 1 };

        queue.pushFrame("while_loop", instruction.innerBlock,
                () -> {
                    queue.setBroken(false);
                    queue.define(asVar, null);
                },
                () -> {
                    if (queue.isBroken()) return false;

                    int nextIndex = ++iterationCounter[0];

                    if (nextIndex > 100000) {
                        Debugger.echoError(queue, "StackOverflow: Too many iterations!");
                        Debugger.echoError(queue, "While loop exceeded 100,000 iterations! Force-breaking to prevent server crash. Check your condition logic.");
                        return false;
                    }

                    queue.define(asVar, new ElementTag(nextIndex));

                    return finalCondition.evaluate(queue);
                }
        );
    }
}
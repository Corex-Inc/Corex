package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.SlotAware;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.SlotTable;
import dev.corexinc.corex.engine.queue.MutableDefinition;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
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
public class WhileCommand implements AbstractCommand, SlotAware {

    private record LoopSlots(String asVar, int slot) {}

    public static final int DEFAULT_MAX_ITERATIONS = 100_000;

    private static volatile int maxIterations = DEFAULT_MAX_ITERATIONS;

    /**
     * Sets the iteration count at which a while loop is force-broken; {@code 0} keeps the default.
     */
    public static void setMaxIterations(int iterations) {
        maxIterations = iterations > 0 ? iterations : DEFAULT_MAX_ITERATIONS;
    }

    @Override
    public @NonNull List<String> writtenDefinitions(@NonNull Instruction instruction) {
        String asVar = RepeatCommand.resolveLoopVar(instruction, "loopIndex");
        return asVar != null ? List.of(asVar) : List.of();
    }

    @Override
    public void bindSlots(@NonNull Instruction instruction, @NonNull SlotTable table) {
        String asVar = RepeatCommand.resolveLoopVar(instruction, "loopIndex");
        if (asVar != null) instruction.slotData = new LoopSlots(asVar, table.indexOf(asVar));
    }

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
    public boolean isAsyncSafe() {
        return true;
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

        final MutableDefinition.OfInt index = new MutableDefinition.OfInt(1);
        final AbstractTag shadowed = queue.rawDefinition(slot, asVar);
        queue.setDefinition(slot, asVar, index);

        boolean initialResult = condition.evaluate(queue);

        if (Debugger.shouldReport(queue)) {
            Debugger.report(queue, instruction,
                    "Initial Condition", String.valueOf(initialResult),
                    "AsDefinition", asVar
            );
        }

        if (!initialResult) {
            queue.setDefinition(slot, asVar, shadowed);
            return;
        }

        final ConditionCompiler.Condition finalCondition = condition;

        queue.pushFrame("while_loop", instruction.innerBlock,
                () -> {
                    queue.setBroken(false);
                    queue.setDefinition(slot, asVar, shadowed);
                },
                () -> {
                    if (queue.isBroken()) return false;

                    int nextIndex = index.value + 1;

                    if (nextIndex > maxIterations) {
                        Debugger.echoError(queue, "While loop exceeded " + maxIterations
                                + " iterations (scripts.while-max-iterations in config.yml)! Force-breaking, check your condition logic.");
                        return false;
                    }

                    index.value = nextIndex;
                    if (queue.rawDefinition(slot, asVar) != index) queue.setDefinition(slot, asVar, index);

                    return finalCondition.evaluate(queue);
                }
        );
    }
}
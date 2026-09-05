package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.environment.utils.scripts.ConditionCompiler;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/* @doc command
 *
 * @Name Else
 * @Syntax else (if <condition>)
 * @RequiredArgs 0
 * @MaxArgs -1
 * @ShortDescription Runs its block when the preceding 'if' did not.
 *
 * @Implements Else
 *
 * @Description
 * Only valid directly after an 'if' block (or another 'else if'). Runs its block
 * when the check before it came out false.
 *
 * Write 'else if <condition>' to chain another check. The chain stops at the
 * first branch that runs, and a bare 'else' at the end catches everything that
 * fell through.
 *
 * Using 'else' with no 'if' above it logs an error and does nothing.
 *
 * @Usage
 * // Two-way branch.
 * - if <player.health> < 5:
 *   - narrate "Careful, you're almost dead!"
 * - else:
 *   - narrate "You're fine."
 *
 * @Usage
 * // Chained checks, top to bottom.
 * - if <[score]> >= 100:
 *   - narrate "Gold rank"
 * - else if <[score]> >= 50:
 *   - narrate "Silver rank"
 * - else:
 *   - narrate "Bronze rank"
 */
public class IfElseCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "else";
    }

    @Override
    public @NonNull String getSyntax() {
        return "(if) (<value>)";
    }

    @Override
    public int getMinArgs() {
        return 0;
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
        Object lastIfObj = queue.getTempData("corex_if_result");

        if (!(lastIfObj instanceof Boolean)) {
            Debugger.echoError(queue, "Command '" + getName() +"' cannot be called without a preceding '- if'!");
            return;
        }

        boolean lastIf = (Boolean) lastIfObj;

        if (lastIf) return;

        if (instruction.linearArgs.length > 0) {
            String firstArg = instruction.getLinear(0, queue);
            if (firstArg != null && firstArg.equals("if")) {

                ConditionCompiler.Condition condition = (ConditionCompiler.Condition) instruction.customData;
                if (condition == null) {
                    CompiledArgument[] conditionArgs =
                            Arrays.copyOfRange(instruction.linearArgs, 1, instruction.linearArgs.length);

                    condition = ConditionCompiler.compile(conditionArgs);
                    instruction.customData = condition;
                }

                boolean result = condition.evaluate(queue);
                queue.setTempData("corex_if_result", result);

                Debugger.report(queue, instruction,
                        "Result", String.valueOf(result)
                );

                if (result && instruction.innerBlock != null) {
                    queue.pushFrame(getName(), instruction.innerBlock, () -> queue.setTempData("corex_if_result", true));
                }
                return;
            }
        }

        queue.setTempData("corex_if_result", null);

        if (instruction.innerBlock != null) {
            queue.pushFrame(getName(), instruction.innerBlock, () -> queue.setTempData("corex_if_result", null));
        }
    }
}
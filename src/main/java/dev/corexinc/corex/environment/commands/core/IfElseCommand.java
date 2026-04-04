package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.environment.utils.ConditionCompiler;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

public class IfElseCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "else";
    }

    @Override
    public @NonNull String getSyntax() {
        return "(if) (<value>)";
    }

    @Override public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return -1;
    }

    @Override
    public void run(ScriptQueue queue, @NonNull Instruction instruction) {
        Object lastIfObj = queue.getTempData("corex_if_result");

        if (!(lastIfObj instanceof Boolean)) {
            Debugger.error(queue, "Command '" + getName() +"' cannot be called without a preceding '- if'!", 0);
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

                if (result && instruction.innerBlock != null) {
                    queue.pushFrame(getName(), instruction.innerBlock, null);
                }
                return;
            }
        }

        queue.setTempData("corex_if_result", null);

        if (instruction.innerBlock != null) {
            queue.pushFrame(getName(), instruction.innerBlock, null);
        }
    }
}
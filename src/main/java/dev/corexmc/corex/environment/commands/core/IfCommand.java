package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.utils.ConditionCompiler;
import org.jspecify.annotations.NonNull;

public class IfCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "if"; }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        ConditionCompiler.Condition condition = (ConditionCompiler.Condition) instruction.customData;
        if (condition == null) {
            condition = ConditionCompiler.compile(instruction.linearArgs);
            instruction.customData = condition;
        }

        boolean result = condition.evaluate(queue);

        queue.setTempData("corex_if_result", result);

        if (result && instruction.innerBlock != null) {
            queue.pushFrame(instruction.innerBlock, null);
        }
    }

    @Override public @NonNull String getSyntax() { return "[<value>]"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 99; }
}
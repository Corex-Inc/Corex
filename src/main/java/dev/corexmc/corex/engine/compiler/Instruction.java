package dev.corexmc.corex.engine.compiler;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.queue.ScriptQueue;

import javax.annotation.Nullable;
import java.util.Map;

public class Instruction {
    public final AbstractCommand command;
    public final CompiledArgument[] linearArgs;
    public final Map<String, CompiledArgument> prefixArgs;
    public final String[] flags;

    public Instruction(AbstractCommand command, CompiledArgument[] linearArgs, Map<String, CompiledArgument> prefixArgs, String[] flags) {
        this.command = command;
        this.linearArgs = linearArgs;
        this.prefixArgs = prefixArgs;
        this.flags = flags;
    }

    public String getLinear(int index, ScriptQueue queue) {
        if (index < 0 || index >= linearArgs.length) return null;
        return linearArgs[index].evaluate(queue);
    }

    @Nullable
    public String getPrefix(String prefix, ScriptQueue queue) {
        CompiledArgument arg = prefixArgs.get(prefix.toLowerCase());
        return arg != null ? arg.evaluate(queue) : null;
    }

    public boolean hasFlag(String flag) {
        for (String f : flags) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }
}
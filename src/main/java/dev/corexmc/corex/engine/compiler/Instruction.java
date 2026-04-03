package dev.corexmc.corex.engine.compiler;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.api.flags.AbstractGlobalFlag;
import dev.corexmc.corex.engine.queue.ScriptQueue;

import javax.annotation.Nullable;
import java.util.Map;

public class Instruction {
    public final AbstractCommand command;
    public final CompiledArgument[] linearArgs;
    public final Map<String, CompiledArgument> prefixArgs;
    public final String[] flags;
    public final Instruction[] innerBlock;
    public Object customData = null;
    public final boolean isWaitable;
    public final java.util.Map<dev.corexmc.corex.api.flags.AbstractGlobalFlag, CompiledArgument> globalFlags;

    public Instruction(
           AbstractCommand command,
           CompiledArgument[] linearArgs,
           Map<String, CompiledArgument> prefixArgs,
           String[] flags,
           Instruction[] innerBlock,
           boolean isWaitable,
           Map<AbstractGlobalFlag, CompiledArgument> globalFlags
    ) {
        this.command = command;
        this.linearArgs = linearArgs;
        this.prefixArgs = prefixArgs;
        this.flags = flags;
        this.innerBlock = innerBlock;
        this.isWaitable = isWaitable;
        this.globalFlags = globalFlags;
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
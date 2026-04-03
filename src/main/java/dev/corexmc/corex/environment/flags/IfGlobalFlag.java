package dev.corexmc.corex.environment.flags;

import dev.corexmc.corex.api.flags.AbstractGlobalFlag;
import dev.corexmc.corex.engine.compiler.CompiledArgument;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.core.ElementTag;

public class IfGlobalFlag implements AbstractGlobalFlag {
    @Override public String getName() { return "if"; }

    @Override
    public boolean execute(ScriptQueue queue, Instruction instruction, CompiledArgument value) {
        String result = value.evaluate(queue);
        return new ElementTag(result).asBoolean();
    }
}
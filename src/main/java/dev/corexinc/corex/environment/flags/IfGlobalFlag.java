package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;

public class IfGlobalFlag implements AbstractGlobalFlag {
    @Override public String getName() { return "if"; }

    @Override
    public boolean execute(ScriptQueue queue, Instruction instruction, CompiledArgument value) {
        String result = value.evaluate(queue);
        return new ElementTag(result).asBoolean();
    }
}
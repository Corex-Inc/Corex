package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;

public class SaveGlobalFlag implements AbstractGlobalFlag {
    @Override public String getName() { return "save"; }

    @Override
    public boolean execute(ScriptQueue queue, Instruction instruction, CompiledArgument value) {
        return true;
    }
}
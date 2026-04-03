package dev.corexmc.corex.api.flags;

import dev.corexmc.corex.engine.compiler.CompiledArgument;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;

public interface AbstractGlobalFlag {
    String getName();

    boolean execute(ScriptQueue queue, Instruction instruction, CompiledArgument value);
}
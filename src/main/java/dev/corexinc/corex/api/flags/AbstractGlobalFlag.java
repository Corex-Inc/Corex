package dev.corexinc.corex.api.flags;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;

public interface AbstractGlobalFlag {
    String getName();

    boolean execute(ScriptQueue queue, Instruction instruction, CompiledArgument value);
}
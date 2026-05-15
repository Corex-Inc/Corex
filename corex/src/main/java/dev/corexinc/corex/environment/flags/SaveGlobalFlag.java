package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jspecify.annotations.NonNull;

public class SaveGlobalFlag implements AbstractGlobalFlag {
    @Override public @NonNull String getName() { return "save"; }

    @Override
    public boolean execute(@NonNull ScriptQueue queue, @NonNull Instruction instruction, @NonNull CompiledArgument value) {
        return true;
    }
}
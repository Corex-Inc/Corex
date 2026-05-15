package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

public class IfGlobalFlag implements AbstractGlobalFlag {
    @Override public @NonNull String getName() { return "if"; }

    @Override
    public boolean execute(@NonNull ScriptQueue queue, @NonNull Instruction instruction, @NonNull CompiledArgument value) {
        String result = value.evaluate(queue).identify();
        return new ElementTag(result).asBoolean();
    }
}
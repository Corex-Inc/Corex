package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;

import java.util.function.Consumer;

public interface CompiledArgument {
    AbstractTag evaluate(ScriptQueue queue);
    String getRaw();

    default void visitChildren(Consumer<CompiledArgument> visitor) {}
}

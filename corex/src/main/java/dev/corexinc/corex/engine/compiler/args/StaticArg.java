package dev.corexinc.corex.engine.compiler.args;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;

public class StaticArg implements CompiledArgument {
    private final AbstractTag tag;

    public StaticArg(String text) {
        this.tag = new ElementTag(text);
    }

    public StaticArg(AbstractTag tag) {
        this.tag = tag;
    }

    @Override
    public AbstractTag evaluate(ScriptQueue queue) {
        return tag;
    }

    @Override
    public String getRaw() {
        return tag.identify();
    }
}

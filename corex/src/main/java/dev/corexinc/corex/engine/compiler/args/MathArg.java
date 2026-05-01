package dev.corexinc.corex.engine.compiler.args;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.math.MathNode;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.tags.core.ElementTag;

public class MathArg implements CompiledArgument {
    private final MathNode node;
    private final String raw;

    public MathArg(MathNode node, String raw) {
        this.node = node;
        this.raw = raw;
    }

    @Override
    public AbstractTag evaluate(ScriptQueue queue) {
        try {
            double result = node.eval(queue);
            return new ElementTag(result);
        } catch (Exception e) {
            CorexLogger.error("ERROR while calculating expression " + raw + ": " + e.getMessage());
            return new ElementTag(0);
        }
    }

    @Override
    public String getRaw() {
        return raw;
    }
}

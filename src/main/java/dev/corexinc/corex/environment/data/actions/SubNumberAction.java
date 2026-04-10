package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SubNumberAction implements AbstractDataAction {

    @Override public @NonNull String getSymbol() { return "-:"; }
    @Override public boolean isPrefix() { return true; }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        if (!(current instanceof ElementTag el) || !el.isDouble()) return current;
        ElementTag subtrahend = new ElementTag(param);
        if (!subtrahend.isDouble()) return current;
        return new ElementTag(el.asDouble() - subtrahend.asDouble());
    }
}



package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class IncrementAction implements AbstractDataAction {

    @Override public @NonNull String getSymbol() { return "++"; }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        double base;
        if (current == null) {
            base = 0;
        }
        else if (current instanceof ElementTag el && el.isDouble()) {
            base = el.asDouble();
        }
        else {
            Debugger.echoError(queue, "Cannot apply '<yellow>:++</yellow>' to non-decimal value '<red>" + current.identify() + "</red>'.");
            return null;
        }
        return new ElementTag(base + 1);
    }
}
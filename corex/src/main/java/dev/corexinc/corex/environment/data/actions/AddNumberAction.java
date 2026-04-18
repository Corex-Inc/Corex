package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class AddNumberAction implements AbstractDataAction {

    @Override public @NonNull String getSymbol() { return "+:"; }
    @Override public boolean isPrefix() { return true; }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        if (current == null) {
            Debugger.echoError(queue, "Cannot apply '<yellow>:+:</yellow>' to an uninitialized definition.");
            Debugger.echoError(queue, "Expected a decimal value, but got '<red>nothing</red>'.");
            return null;
        }

        if (!(current instanceof ElementTag el) || !el.isDouble()) {
            Debugger.echoError(queue, "Invalid value for data action '<yellow>:+:</yellow>'.");
            Debugger.echoError(queue, "Expected a decimal value, but got '<red>" + current.identify() + "</red>'.");
            return null;
        }
        ElementTag addend = new ElementTag(param);
        if (!addend.isDouble()) return current;
        return new ElementTag(el.asDouble() + addend.asDouble());
    }
}
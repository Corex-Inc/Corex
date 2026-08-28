package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class AssignAction implements AbstractDataAction {

    @Override
    public @NonNull String getSymbol() {
        return "";
    }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        if (secondArg == null) {
            return ObjectFetcher.pickObject(param);
        }

        if (secondArg instanceof ElementTag element) {
            return element.isKnownNumeric() || element.asString().indexOf('@') < 1
                    ? element
                    : ObjectFetcher.pickObject(element.asString());
        }

        return secondArg;
    }
}


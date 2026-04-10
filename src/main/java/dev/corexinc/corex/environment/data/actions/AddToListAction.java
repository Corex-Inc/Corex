package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ListTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class AddToListAction implements AbstractDataAction {

    @Override public @NonNull String getSymbol() { return "|+:"; }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        ListTag list = current instanceof ListTag lt ? lt : new ListTag();
        AbstractTag toAdd = secondArg != null ? secondArg : ObjectFetcher.pickObject(param);
        list.addObject(toAdd);
        return list;
    }

    @Override public boolean isPrefix() { return true; }
}
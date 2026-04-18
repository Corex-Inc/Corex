package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ListTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MergeListsAction implements AbstractDataAction {

    @Override public @NonNull String getSymbol() { return "|:"; }
    @Override public boolean isPrefix() { return true; }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        ListTag result = new ListTag();

        if (current instanceof ListTag existingList) {
            for (AbstractTag item : existingList.getList()) result.addObject(item);
        }

        AbstractTag resolved = ObjectFetcher.pickObject(param);
        ListTag toMerge = resolved instanceof ListTag lt ? lt : new ListTag(param);
        for (AbstractTag item : toMerge.getList()) result.addObject(item);

        return result;
    }
}
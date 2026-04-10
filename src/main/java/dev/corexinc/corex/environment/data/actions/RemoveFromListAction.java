package dev.corexinc.corex.environment.data.actions;

import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ListTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RemoveFromListAction implements AbstractDataAction {

    @Override public @NonNull String getSymbol() { return "|-:"; }

    @Override
    public @Nullable AbstractTag apply(@Nullable AbstractTag current, @NonNull String param,
                                       @Nullable AbstractTag secondArg, @NonNull ScriptQueue queue) {
        if (!(current instanceof ListTag existingList)) return current;

        String targetIdentity = secondArg != null
                ? secondArg.identify()
                : ObjectFetcher.pickObject(param).identify();

        List<AbstractTag> items = existingList.getList();
        ListTag result = new ListTag();
        boolean removed = false;
        for (AbstractTag item : items) {
            if (!removed && item.identify().equals(targetIdentity)) {
                removed = true;
                continue;
            }
            result.addObject(item);
        }
        return result;
    }

    @Override public boolean isPrefix() { return true; }
}
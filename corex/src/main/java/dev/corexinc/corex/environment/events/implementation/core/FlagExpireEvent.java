package dev.corexinc.corex.environment.events.implementation.core;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.flags.FlagExpirationHandler;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;

public class FlagExpireEvent implements AbstractEvent, FlagExpirationHandler {

    private static final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NonNull String getName() {
        return "FlagExpire";
    }

    @Override
    public @NonNull String getSyntax() {
        return "<flag> expires";
    }

    @Override
    public void initListener() {
        FlagManager.setExpirationHandler(this);
    }

    @Override
    public void addScript(@NonNull EventData data) {
        scripts.add(data);
    }

    @Override
    public void reset() {
        scripts.clear();
    }

    @Override
    public AbstractTag onExpired(String trackerId, String path, AbstractTag value) {
        AbstractTag finalDecision = null;
        ElementTag isCancelled = new ElementTag(false);

        for (EventData data : scripts) {
            if (!data.isGenericMatch("flag", 0, path)) {
                continue;
            }

            ContextTag context = new ContextTag();
            context.put("object", ObjectFetcher.pickObject(trackerId));
            context.put("name", new ElementTag(path));
            context.put("value", value);

            ScriptQueue queue = EventRegistry.fire(data, null, context);

            if (!queue.getReturns().isEmpty()) {
                isCancelled = new ElementTag(queue.isCancelled());
                finalDecision = queue.getReturns().getFirst();
            }
        }
        return isCancelled.asBoolean() ? isCancelled : finalDecision;
    }
}
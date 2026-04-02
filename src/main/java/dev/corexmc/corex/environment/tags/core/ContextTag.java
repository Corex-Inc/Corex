package dev.corexmc.corex.environment.tags.core;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class ContextTag implements AbstractTag {

    private final String prefix = "context";
    private final Map<String, AbstractTag> contextData = new HashMap<>();

    public static final TagProcessor<ContextTag> PROCESSOR = new TagProcessor<>();
    public ContextTag() {}

    public static void register() {
        BaseTagProcessor.registerBaseTag("context", attr -> {
            if (attr.getQueue() != null && attr.getQueue().getContext() != null) {
                return attr.getQueue().getContext();
            }
            return null;
        });
    }

    public ContextTag put(String key, AbstractTag value) {
        if (value != null) {
            contextData.put(key.toLowerCase(), value);
        }
        return this;
    }

    public AbstractTag get(String key) {
        return contextData.get(key.toLowerCase());
    }

    @Override public @NonNull String getPrefix() { return prefix; }
    @Override public @NonNull String identify() { return prefix + "@"; }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        AbstractTag result = PROCESSOR.process(this, attribute);
        if (result != null) return result;

        String key = attribute.getName().toLowerCase();
        if (contextData.containsKey(key)) {
            attribute.fulfill(1);
            return contextData.get(key);
        }

        return null;
    }

    @Override
    public @NonNull String getTestValue() {
        return null;
    }

    @Override
    public @NonNull TagProcessor<? extends AbstractTag> getProcessor() {
        return PROCESSOR;
    }
}
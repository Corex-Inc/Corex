package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class ContextTag implements AbstractTag {

    private final String prefix = "context";
    private final Map<String, AbstractTag> contextData = new HashMap<>();

    public static final TagProcessor<ContextTag> TAG_PROCESSOR = new TagProcessor<>();
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
            contextData.put(key, value);
        }
        return this;
    }

    public AbstractTag get(String key) {
        return contextData.get(key);
    }

    @Override public @NonNull String getPrefix() { return prefix; }
    @Override public @NonNull String identify() { return prefix + "@"; }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        AbstractTag result = TAG_PROCESSOR.process(this, attribute);
        if (result != null) return result;

        String key = attribute.getName();
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
        return TAG_PROCESSOR;
    }
}
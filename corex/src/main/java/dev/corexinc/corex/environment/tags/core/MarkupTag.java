package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MarkupTag implements AbstractTag {

    private final String markup;

    public MarkupTag(String markup) {
        this.markup = markup;
    }

    @Override
    public @NonNull String identify() {
        return markup;
    }

    @Override
    public @NonNull String getPrefix() {
        return "markup";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return null;
    }

    @Override
    public @Nullable String getTestValue() {
        return null;
    }

    @Override
    public @NonNull TagProcessor<? extends AbstractTag> getProcessor() {
        return null;
    }
}
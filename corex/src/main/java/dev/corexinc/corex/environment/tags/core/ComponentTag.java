package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class ComponentTag implements AbstractTag {

    private final Component component;

    public ComponentTag(Component component) {
        this.component = component;
    }

    @Override
    public @NonNull Component asComponent() {
        return this.component;
    }

    public @NonNull String identify() {
        return Corex.SERIALIZER.serialize(component);
    }

    @Override public @NonNull String getPrefix() { return "component"; }
    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return null; }

    @Override
    public @Nullable String getTestValue() {
        return "";
    }

    @Override public @NonNull TagProcessor<? extends AbstractTag> getProcessor() { return null; }
}
package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SpaceFormatter implements AbstractFormatter {
    private static final AbstractTag INSTANCE = new ElementTag(" ");

    @Override public @NonNull String getName() { return "sp"; }
    @Override public @NonNull List<String> getAlias() { return List.of("sp", "space"); }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        return INSTANCE;
    }
}
package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class NewLineFormatter implements AbstractFormatter {
    private static final AbstractTag INSTANCE = new ElementTag("\n");

    @Override public @NonNull String getName() { return "n"; }
    @Override public @NonNull List<String> getAlias() { return List.of("n", "newline", "nl", "&nl"); }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        return INSTANCE;
    }
}
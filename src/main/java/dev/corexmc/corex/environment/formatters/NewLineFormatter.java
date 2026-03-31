package dev.corexmc.corex.environment.formatters;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.environment.tags.core.ElementTag;

import java.util.List;

public class NewLineFormatter implements AbstractFormatter {
    private static final AbstractTag INSTANCE = new ElementTag("\n");

    @Override public String getName() { return "n"; }
    @Override public List<String> getAlias() { return List.of("n", "newline", "nl", "&nl"); }

    @Override
    public AbstractTag parse(Attribute attribute) {
        return INSTANCE;
    }
}
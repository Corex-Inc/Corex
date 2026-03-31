package dev.corexmc.corex.environment.formatters;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.environment.tags.core.ElementTag;

import java.util.List;

public class SpaceFormatter implements AbstractFormatter {
    private static final AbstractTag INSTANCE = new ElementTag(" ");
    @Override public String getName() { return "sp"; }
    @Override public List<String> getAlias() { return List.of("sp", "space"); }
    @Override public AbstractTag getObject() { return INSTANCE; }
}
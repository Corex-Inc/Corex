package dev.corexmc.corex.environment.formatters;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import java.util.List;

public class SpaceFormatter implements AbstractFormatter {
    @Override public String getName() { return "sp"; }
    @Override public List<String> getAlias() { return List.of("sp", "space"); }
    @Override public String getValue() { return " "; }
}
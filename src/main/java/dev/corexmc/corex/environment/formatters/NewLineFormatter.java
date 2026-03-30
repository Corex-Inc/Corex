package dev.corexmc.corex.environment.formatters;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import java.util.List;

public class NewLineFormatter implements AbstractFormatter {
    @Override public String getName() { return "n"; }
    @Override public List<String> getAlias() { return List.of("n", "newline", "nl", "&nl"); }
    @Override public String getValue() { return "\n"; }
}
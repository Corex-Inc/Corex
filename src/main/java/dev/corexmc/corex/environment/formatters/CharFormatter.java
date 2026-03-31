package dev.corexmc.corex.environment.formatters;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.environment.tags.core.ElementTag;

public class CharFormatter implements AbstractFormatter {
    @Override public String getName() { return "&char"; }

    @Override
    public AbstractTag parse(Attribute attribute) {
        if (!attribute.hasParam()) return new ElementTag("?");

        try {
            int code = Integer.parseInt(attribute.getParam());
            return new ElementTag(String.valueOf((char) code));
        } catch (Exception e) {
            return null;
        }
    }

    @Override public String getTestParam() { return "65"; }
}
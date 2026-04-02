package dev.corexmc.corex.environment.formatters;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

public class CharFormatter implements AbstractFormatter {
    @Override public @NonNull String getName() { return "&char"; }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
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
package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
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
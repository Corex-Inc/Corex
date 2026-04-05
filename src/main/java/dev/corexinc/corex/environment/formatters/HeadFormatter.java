package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

public class HeadFormatter implements AbstractFormatter {
    @Override public @NonNull String getName() { return "&head"; }

    @Override
    public @NonNull AbstractTag parse(Attribute attribute) {
        if (!attribute.hasParam()) return new ElementTag("");

        String mmTag = "<head:" + attribute.getParam() + ">";

        try {
            return new ComponentTag(MiniMessage.miniMessage().deserialize(mmTag));
        } catch (Exception e) {
            return new ElementTag("");
        }
    }
}
package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jspecify.annotations.NonNull;

public class HeadFormatter implements AbstractFormatter {
    @Override public @NonNull String getName() { return "&head"; }
    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull AbstractTag parse(Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;

        try {
            return new ComponentTag(MiniMessage.miniMessage().deserialize("<head:name>",
                    Placeholder.unparsed("name", attribute.getParam())));
        } catch (Exception e) {
            return INSTANCE;
        }
    }
}
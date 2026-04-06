package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ColorFormatter implements AbstractFormatter {

    @Override
    public @NonNull String getName() {
        return "&color";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("&0","&1","&2","&3",
                "&4","&5","&6","&7",
                "&8","&9","&a","&b",
                "&c","&d","&e","&f",
                "&l","&m","&n","&o","&r");
    }

    @Override
    public @NonNull AbstractTag parse(Attribute attribute) {
        String name = attribute.getName();

        if (name.length() == 2 && name.startsWith("&")) {
            char code = name.charAt(1);
            return new ElementTag("§" + code);
        }

        if (attribute.hasParam()) {
            String hex = attribute.getParam();
            if (hex.startsWith("#")) hex = hex.substring(1);

            if (hex.length() == 6) {
                StringBuilder hexResult = new StringBuilder("§x");
                for (char c : hex.toCharArray()) {
                    hexResult.append('§').append(c);
                }
                return new ElementTag(hexResult.toString());
            }
        }

        return new ElementTag("");
    }
}
package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SpriteFormatter implements AbstractFormatter {

    @Override public @NonNull String getName() { return "&sprite"; }
    @Override public @NonNull List<String> getAlias() { return List.of("&icon"); }

    @Override
    public @NonNull AbstractTag parse(Attribute attribute) {
        if (!attribute.hasParam()) return new ElementTag("");

        String param = attribute.getParam();
        String atlas = "minecraft:items";
        String sprite = "";

        if (param.contains("=") || param.contains(";")) {
            MapTag map = new MapTag(param);

            AbstractTag spaceTag = map.getObject("space");
            if (spaceTag != null) atlas = spaceTag.identify();

            AbstractTag nameTag = map.getObject("name");
            if (nameTag == null) nameTag = map.getObject("key");

            if (nameTag != null) sprite = nameTag.identify();
        }
        else if (param.contains("|")) {
            String[] parts = param.split("\\|");
            atlas = parts[0];
            sprite = parts[1];
        } else {
            sprite = param;
        }

        if (sprite.isEmpty()) return new ElementTag("");

        String mmTag = "<sprite:\"" + atlas + "\":\"" + sprite + "\">";

        return new ComponentTag(MiniMessage.miniMessage().deserialize(mmTag));
    }
}
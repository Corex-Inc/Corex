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
import java.util.Optional;

public class SpriteFormatter implements AbstractFormatter {

    private static final String DEFAULT_ATLAS = "minecraft:items";
    private static final ElementTag INSTANCE = new ElementTag("");

    @Override public @NonNull String getName() { return "&sprite"; }
    @Override public @NonNull List<String> getAlias() { return List.of("&icon"); }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;

        String param = attribute.getParam();
        String atlas = DEFAULT_ATLAS;
        String sprite;

        if (param.contains("=") || param.contains(";")) {
            MapTag map = new MapTag(param);
            atlas = getValue(map, "atlas").orElse(DEFAULT_ATLAS);
            sprite = getValue(map, "name").or(() -> getValue(map, "key")).orElse("");

        } else if (param.contains("|")) {
            String[] parts = param.split("\\|", 2);

            if (parts.length < 2) return INSTANCE;

            atlas = parts[0].strip();
            sprite = parts[1].strip();

        } else {
            sprite = param.strip();
        }

        if (sprite.isBlank()) return INSTANCE;

        String safeAtlas = atlas.replace("<", "").replace(">", "");
        String safeSprite = sprite.replace("<", "").replace(">", "");

        try {
            return new ComponentTag(MiniMessage.miniMessage().deserialize("<sprite:\"" + safeAtlas + "\":" + safeSprite + ">"));
        } catch (Exception e) {
            return INSTANCE;
        }
    }

    private Optional<String> getValue(MapTag map, String key) {
        return Optional.ofNullable(map.getObject(key))
                .map(AbstractTag::identify)
                .filter(value -> !value.isBlank());
    }
}
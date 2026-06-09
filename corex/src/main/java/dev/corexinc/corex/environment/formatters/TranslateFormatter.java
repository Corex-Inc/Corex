package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/* @doc formatter
 *
 * @Name &translate
 * @Syntax <&translate[key=<key>;(fallback=<fallback>);(with=<text>|...)]>
 * @ArgRequired
 * @Description
 * Returns a component that the client auto-translates using its language file.
 * "key" is the translation key (required).
 * "fallback" is displayed when the client cannot find a translation for the key (optional).
 * "with" is a pipe-separated list of arguments that fill in dynamic slots in the translated message (optional).
 * Be aware that translation keys can change between Minecraft versions.
 *
 * @Usage
 * // Narrates the display name of a diamond sword.
 * - narrate "Reward: <&translate[key=item.minecraft.diamond_sword]>"
 *
 * // Translates a command feedback message with dynamic arguments.
 * - narrate <&translate[key=commands.give.success.single;with=32|<&translate[key=item.minecraft.diamond_sword]>|<player.name>]>
 *
 * // Uses a custom resource pack translation key with a fallback.
 * - narrate <&translate[key=my.custom.translation;fallback=Please use the resource pack!]>
 *
 * @Implements &translate
 */
public class TranslateFormatter implements AbstractFormatter {

    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull String getName() {
        return "&translate";
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;

        String param = attribute.getParam();
        MapTag map = new MapTag(param);

        String key = getValue(map, "key").orElse("").strip();
        if (key.isBlank()) return INSTANCE;

        String fallback = getValue(map, "fallback").orElse(null);
        String withRaw = getValue(map, "with").orElse(null);

        try {
            TranslatableComponent.Builder builder = Component.translatable().key(key);

            if (fallback != null && !fallback.isBlank()) {
                builder.fallback(fallback);
            }

            if (withRaw != null && !withRaw.isBlank()) {
                List<ComponentLike> args = new ArrayList<>();
                for (String part : withRaw.split("\\|")) {
                    args.add(Component.text(part));
                }
                builder.arguments(args);
            }

            return new ComponentTag(builder.build());
        } catch (Exception e) {
            return INSTANCE;
        }
    }

    private Optional<String> getValue(MapTag map, String key) {
        return Optional.ofNullable(map.getObject(key))
                .map(AbstractTag::identify)
                .filter(v -> !v.isBlank());
    }

    @Override
    public String getTestParam() {
        return "key=item.minecraft.diamond_sword;fallback=Fallbackness!";
    }
}
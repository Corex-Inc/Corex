package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

/* @doc formatter
 *
 * @Name &font
 * @Syntax <&font[<font>]>
 * @ArgRequired
 * @Description
 * Returns a MiniMessage open tag that makes the following text render with the specified font.
 * The font identifier should be a namespaced key such as "minecraft:default", "minecraft:uniform",
 * "minecraft:alt", or a custom font from a resource pack.
 * The default font is "minecraft:default".
 * The font applies to all subsequent text until the next reset or font tag.
 *
 * @Usage
 * // Renders text in the "minecraft:uniform" font.
 * - narrate "<&font[minecraft:uniform]>Uniform font text<&r>"
 *
 * // Uses a custom resource pack font.
 * - narrate "<&font[myrp:custom_font]>Custom text here<&r>"
 *
 * @Implements &font
 */
public class FontFormatter implements AbstractFormatter {

    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull String getName() {
        return "&font";
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;
        String font = attribute.getParam()
                .strip()
                .replace("<", "")
                .replace(">", "");
        if (font.isBlank()) return INSTANCE;
        return new ComponentTag(MiniMessage.miniMessage().deserialize("<font:" + font + ">"));
    }

    @Override
    public String getTestParam() {
        return "minecraft:uniform";
    }
}
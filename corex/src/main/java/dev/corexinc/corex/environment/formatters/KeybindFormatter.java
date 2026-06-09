package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

/* @doc formatter
 *
 * @Name &keybind
 * @Syntax <&keybind[<key>]>
 * @ArgRequired
 * @Description
 * Returns a component that displays the player's configured keybind for the given key identifier.
 * The client resolves the actual key based on the player's input settings.
 * Common keys: key.jump, key.sneak, key.attack, key.use, key.inventory.
 *
 * @Usage
 * // Displays the player's configured jump key.
 * - narrate "Press your <&keybind[key.jump]> key to jump!"
 *
 * // Shows the attack key.
 * - narrate "Use <&keybind[key.attack]> to attack."
 *
 * @Implements &keybind
 */
public class KeybindFormatter implements AbstractFormatter {

    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull String getName() {
        return "&keybind";
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;
        String key = attribute.getParam().strip();
        if (key.isBlank()) return INSTANCE;
        try {
            return new ComponentTag(Component.keybind(key));
        } catch (Exception e) {
            return INSTANCE;
        }
    }

    @Override
    public String getTestParam() {
        return "key.jump";
    }
}
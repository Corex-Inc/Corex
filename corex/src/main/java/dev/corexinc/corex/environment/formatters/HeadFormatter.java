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
 * @Name &head
 * @ArgRequired
 * @Description
 * Generates a MiniMessage component that displays a player head icon within text, based on the provided player name or UUID.
 * This is useful for integrating player heads directly into chat messages or other text displays.
 *
 * @Usage
 * // Displays a player head for 'Notch' followed by their name.
 * - narrate "<&head[Notch]> Notch"
 *
 * // Shows a player head for the player executing the command.
 * - narrate "Welcome, <&head[<player.name>]> <player.name>!"
 */
public class HeadFormatter implements AbstractFormatter {
    @Override public @NonNull String getName() { return "&head"; }
    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull AbstractTag parse(Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;

        String safeName = attribute.getParam().replace("<", "").replace(">", "");

        try {
            return new ComponentTag(MiniMessage.miniMessage().deserialize("<head:" + safeName + ">"));
        } catch (Exception e) {
            return INSTANCE;
        }
    }
}
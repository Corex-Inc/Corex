package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.jspecify.annotations.NonNull;

import java.util.Base64;
import java.util.UUID;

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
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;
        String safeParam = attribute.getParam().replace("<", "").replace(">", "");

        try {
            if (safeParam.length() > 20 && safeParam.matches("^[a-fA-F0-9]+$")) {
                try {
                    Class.forName("net.kyori.adventure.text.object.PlayerHeadObjectContents");
                    return new ComponentTag(ModernHeadHandler.create(safeParam));
                } catch (ClassNotFoundException e) {
                    return INSTANCE;
                }
            }
            return new ComponentTag(MiniMessage.miniMessage().deserialize("<head:" + safeParam + ">"));
        } catch (Exception e) {
            return INSTANCE;
        }
    }

    private static class ModernHeadHandler {
        static Component create(String hash) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + hash + "\"}}}";
            String base64 = Base64.getEncoder().encodeToString(json.getBytes());

            var headContents = ObjectContents.playerHead()
                    .id(UUID.nameUUIDFromBytes(hash.getBytes()))
                    .name("CustomHead")
                    .profileProperty(new SimpleProfileProperty("textures", base64))
                    .build();

            return Component.object(headContents);
        }

        private record SimpleProfileProperty(String name, String value)
                implements PlayerHeadObjectContents.ProfileProperty {
            @Override public String signature() { return null; }
        }
    }
}
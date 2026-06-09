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
 * @Name &score
 * @Syntax <&score[<name>|<objective>]>
 * @ArgRequired
 * @Description
 * Returns a component that displays a scoreboard entry for the given entity name and objective.
 * The client resolves the current scoreboard value at display time.
 * Optionally, a static value can be provided as the third pipe-separated argument,
 * though modern clients may ignore it in favour of the live scoreboard value.
 * Use '*' as the name to target the viewing player.
 *
 * @Usage
 * // Displays the 'kills' score of the viewing player.
 * - narrate "Your kills: <&score[*|kills]>"
 *
 * // Displays the 'coins' score of a specific player.
 * - narrate "<player.name>'s coins: <&score[<player.name>|coins]>"
 *
 * // Provides a static fallback value.
 * - narrate "Score: <&score[Steve|points]>"
 *
 * @Implements &score
 */
public class ScoreFormatter implements AbstractFormatter {

    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull String getName() {
        return "&score";
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;

        String param = attribute.getParam();
        String[] parts = param.split("\\|", 3);
        if (parts.length < 2) return INSTANCE;

        String name = parts[0].strip();
        String objective = parts[1].strip();

        if (name.isBlank() || objective.isBlank()) return INSTANCE;

        try {
            return new ComponentTag(Component.score(name, objective));
        } catch (Exception e) {
            return INSTANCE;
        }
    }

    @Override
    public String getTestParam() {
        return "*|kills";
    }
}
package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ColorTag;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/* @doc formatter
 *
 * @Name &gradient
 * @Syntax <&gradient[<color>(;<color>...)(;style={RGB}/HSB/PHASE)]>
 * @ArgRequired
 * @Description
 * Returns a MiniMessage open tag that makes the following text render with a color gradient.
 * The gradient runs from the tag's position until the next color/reset tag.
 * Accepts two or more colors as semicolon-separated values.
 * Colors can be specified as hex codes (#RRGGBB or RRGGBB), named Minecraft colors,
 * or any value accepted by ColorTag.
 * Optionally append "style=RGB", "style=HSB", or "style=PHASE" to control interpolation mode.
 * The default style is RGB.
 *
 * Two-color shorthand (map form) is also supported for compatibility:
 * <&gradient[from=<color>;to=<color>;(style=RGB/HSB)]>
 *
 * @Usage
 * // Three-color gradient using hex codes.
 * - narrate "<&gradient[#FF0000;#FFFF00;#00FF00]>Red to yellow to green<&r>"
 *
 * @Usage
 * // Five-color rainbow.
 * - narrate "<&gradient[#FF0000;#FF8800;#FFFF00;#00FF00;#0000FF]>Rainbow text<&r>"
 *
 * @Usage
 * // With HSB interpolation.
 * - narrate "<&gradient[#FF0000;#0000FF;style=HSB]>HSB gradient<&r>"
 *
 * @Implements &gradient[from=<color>;to=<color>;(style={RGB}/HSB)]
 */
public class GradientFormatter implements AbstractFormatter {

    private static final ElementTag INSTANCE = new ElementTag("");

    @Override
    public @NonNull String getName() {
        return "&gradient";
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return INSTANCE;

        String param = attribute.getParam();
        List<String> colors = new ArrayList<>();
        String style = null;

        if (param.contains("=")) {
            MapTag map = new MapTag(param);

            String styleVal = getValue(map, "style").orElse(null);
            if (styleVal != null) style = styleVal.toUpperCase();

            Optional<String> colorsKey = getValue(map, "colors");
            if (colorsKey.isPresent()) {
                for (String c : colorsKey.get().split("\\|")) {
                    String resolved = resolveColor(c.strip());
                    if (resolved != null) colors.add(resolved);
                }
            } else {
                for (int i = 1; ; i++) {
                    Optional<String> ci = getValue(map, "color" + i);
                    if (ci.isEmpty()) break;
                    String resolved = resolveColor(ci.get().strip());
                    if (resolved != null) colors.add(resolved);
                }

                if (colors.isEmpty()) {
                    getValue(map, "from").map(c -> resolveColor(c.strip())).ifPresent(colors::add);
                    getValue(map, "to").map(c -> resolveColor(c.strip())).ifPresent(colors::add);
                }
            }
        } else {
            String[] parts = param.split(";");
            for (String part : parts) {
                String trimmed = part.strip();
                if (trimmed.toLowerCase().startsWith("style=")) {
                    style = trimmed.substring(6).strip().toUpperCase();
                } else {
                    String resolved = resolveColor(trimmed);
                    if (resolved != null) colors.add(resolved);
                }
            }
        }

        if (colors.size() < 2) return INSTANCE;

        StringBuilder sb = new StringBuilder("<gradient");
        for (String color : colors) {
            sb.append(':').append(color);
        }
        if (style != null) {
            switch (style) {
                case "HSB"   -> sb.append(":!hsb:0");
                case "PHASE" -> sb.append(":0");
            }
        }
        sb.append('>');

        return new ComponentTag(MiniMessage.miniMessage().deserialize(sb.toString()));
    }

    private String resolveColor(String input) {
        if (input == null || input.isBlank()) return null;

        if (input.startsWith("#") && (input.length() == 7 || input.length() == 4)) {
            return input;
        }
        if (input.matches("[0-9a-fA-F]{6}")) {
            return "#" + input;
        }

        try {
            ColorTag colorTag = new ColorTag(input);
            String hex = colorTag.getHex(true);
            if (hex != null && !hex.isBlank()) return hex;
        } catch (Exception ignored) {
        }

        return input;
    }

    private Optional<String> getValue(MapTag map, String key) {
        return Optional.ofNullable(map.getObject(key))
                .map(AbstractTag::identify)
                .filter(v -> !v.isBlank());
    }

    @Override
    public String getTestParam() {
        return "#FF0000;#0000FF";
    }
}
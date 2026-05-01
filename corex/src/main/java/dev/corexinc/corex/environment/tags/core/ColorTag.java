package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.bukkit.Color;
import org.jspecify.annotations.NonNull;

/* @doc object
 *
 * @Name ColorTag
 * @Prefix color
 * @Format
 * The identity format for ColorTags is 'color@R,G,B' or 'color@R,G,B,A' where each channel is 0-255.
 * For example, red would be 'color@255,0,0' and half-transparent blue would be 'color@0,0,255,127'.
 * Hex input is also accepted as '#RRGGBB' or '#RRGGBBAA'.
 *
 * @Description
 * A ColorTag represents an RGBA color value.
 * Each channel (red, green, blue, alpha) is an integer from 0 to 255.
 * Alpha defaults to 255 (fully opaque) if not specified.
 */
public class ColorTag implements AbstractTag {

    private static final String prefix = "color";

    public final int red;
    public final int green;
    public final int blue;
    public final int alpha;

    public static final TagProcessor<ColorTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(prefix, attr -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(attr.getParam());
        });
        ObjectFetcher.registerFetcher(prefix, ColorTag::new);

        /* @doc tag
         *
         * @Name red
         * @RawName <ColorTag.red>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the red channel of this color (0-255).
         * @Usage
         * // Narrates "255"
         * - narrate <color[255,0,0].red>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "red", (attr, obj) -> new ElementTag(obj.red));

        /* @doc tag
         *
         * @Name green
         * @RawName <ColorTag.green>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the green channel of this color (0-255).
         * @Usage
         * // Narrates "128"
         * - narrate <color[0,128,0].green>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "green", (attr, obj) -> new ElementTag(obj.green));

        /* @doc tag
         *
         * @Name blue
         * @RawName <ColorTag.blue>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the blue channel of this color (0-255).
         * @Usage
         * // Narrates "200"
         * - narrate <color[0,0,200].blue>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "blue", (attr, obj) -> new ElementTag(obj.blue));

        /* @doc tag
         *
         * @Name alpha
         * @RawName <ColorTag.alpha>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the alpha channel of this color (0-255). 255 is fully opaque, 0 is fully transparent.
         * @Usage
         * // Narrates "255"
         * - narrate <color[255,0,0].alpha>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "alpha", (attr, obj) -> new ElementTag(obj.alpha));

        /* @doc tag
         *
         * @Name hex
         * @RawName <ColorTag.hex>
         * @Object ColorTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the color as an uppercase hex string in '#RRGGBB' format.
         * Append '.withAlpha' to include the alpha channel as '#RRGGBBAA'.
         * @Usage
         * // Narrates "#FF0000"
         * - narrate <color[255,0,0].hex>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hex", (attr, obj) -> {
            if (attr.matchesNext("withAlpha")) {
                attr.fulfill(1);
                return new ElementTag(obj.getHex(true));
            }
            return new ElementTag(obj.getHex(false));
        });

        /* @doc tag
         *
         * @Name rgb
         * @RawName <ColorTag.rgb>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the color as a packed RGB integer (as used by Bukkit/NMS color APIs).
         * @Usage
         * // Narrates "16711680"
         * - narrate <color[255,0,0].rgb>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "rgb", (attr, obj) ->
                new ElementTag((obj.red << 16) | (obj.green << 8) | obj.blue));

        /* @doc tag
         *
         * @Name argb
         * @RawName <ColorTag.argb>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the color as a packed ARGB integer (alpha in the highest byte).
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "argb", (attr, obj) ->
                new ElementTag((obj.alpha << 24) | (obj.red << 16) | (obj.green << 8) | obj.blue));

        /* @doc tag
         *
         * @Name toList
         * @RawName <ColorTag.toList>
         * @Object ColorTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns the color channels as a ListTag in R|G|B or R|G|B|A order.
         * Append '.withAlpha' to include the alpha channel.
         * @Usage
         * // Narrates "255|0|0"
         * - narrate <color[255,0,0].toList>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "toList", (attr, obj) -> {
            ListTag result = new ListTag();
            result.addString(String.valueOf(obj.red));
            result.addString(String.valueOf(obj.green));
            result.addString(String.valueOf(obj.blue));
            if (attr.matchesNext("withAlpha")) {
                attr.fulfill(1);
                result.addString(String.valueOf(obj.alpha));
            }
            return result;
        });

        /* @doc tag
         *
         * @Name withRed[]
         * @RawName <ColorTag.withRed[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Description
         * Returns a copy of this color with the red channel replaced by the given value.
         * @Usage
         * // Narrates "color@128,0,0"
         * - narrate <color[255,0,0].withRed[128]>
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withRed", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(clamp(new ElementTag(attr.getParam()).asInt()), obj.green, obj.blue, obj.alpha);
        }).test("128");

        /* @doc tag
         *
         * @Name withGreen[]
         * @RawName <ColorTag.withGreen[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Description
         * Returns a copy of this color with the green channel replaced by the given value.
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withGreen", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(obj.red, clamp(new ElementTag(attr.getParam()).asInt()), obj.blue, obj.alpha);
        }).test("200");

        /* @doc tag
         *
         * @Name withBlue[]
         * @RawName <ColorTag.withBlue[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Description
         * Returns a copy of this color with the blue channel replaced by the given value.
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withBlue", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(obj.red, obj.green, clamp(new ElementTag(attr.getParam()).asInt()), obj.alpha);
        }).test("200");

        /* @doc tag
         *
         * @Name withAlpha[]
         * @RawName <ColorTag.withAlpha[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Description
         * Returns a copy of this color with the alpha channel replaced by the given value.
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withAlpha", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(obj.red, obj.green, obj.blue, clamp(new ElementTag(attr.getParam()).asInt()));
        }).test("128");

        /* @doc tag
         *
         * @Name mix[]
         * @RawName <ColorTag.mix[<color>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Description
         * Returns a new color that is the average of this color and the given color, channel by channel.
         * Optionally append '.by[<0.0-1.0>]' to control the blend factor (0.0 = fully this, 1.0 = fully other).
         * @Usage
         * // Narrates "color@127,127,0,255"
         * - narrate <color[255,0,0].mix[color@0,255,0]>
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "mix", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ColorTag other = new ColorTag(attr.getParam());
            float factor = 0.5f;
            if (attr.matchesNext("by") && attr.hasNextParam()) {
                factor = (float) new ElementTag(attr.getNextParam()).asDouble();
                factor = Math.max(0.0f, Math.min(1.0f, factor));
                attr.fulfill(1);
            }
            return new ColorTag(
                    clamp(Math.round(obj.red   + (other.red   - obj.red)   * factor)),
                    clamp(Math.round(obj.green + (other.green - obj.green) * factor)),
                    clamp(Math.round(obj.blue  + (other.blue  - obj.blue)  * factor)),
                    clamp(Math.round(obj.alpha + (other.alpha - obj.alpha) * factor))
            );
        }).test("color@0,255,0");

        /* @doc tag
         *
         * @Name lighter
         * @RawName <ColorTag.lighter>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @NoArg
         * @Description
         * Returns a lighter version of this color by blending it 50% toward white.
         * @Usage
         * // Narrates "color@255,127,127,255"
         * - narrate <color[255,0,0].lighter>
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "lighter", (attr, obj) ->
                new ColorTag(
                        clamp(obj.red   + (255 - obj.red)   / 2),
                        clamp(obj.green + (255 - obj.green) / 2),
                        clamp(obj.blue  + (255 - obj.blue)  / 2),
                        obj.alpha
                ));

        /* @doc tag
         *
         * @Name darker
         * @RawName <ColorTag.darker>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @NoArg
         * @Description
         * Returns a darker version of this color by halving each channel.
         * @Usage
         * // Narrates "color@127,0,0,255"
         * - narrate <color[255,0,0].darker>
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "darker", (attr, obj) ->
                new ColorTag(obj.red / 2, obj.green / 2, obj.blue / 2, obj.alpha));

        /* @doc tag
         *
         * @Name inverted
         * @RawName <ColorTag.inverted>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @NoArg
         * @Description
         * Returns the inverse of this color (255 - each channel). Alpha is preserved.
         * @Usage
         * // Narrates "color@0,255,255,255"
         * - narrate <color[255,0,0].inverted>
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "inverted", (attr, obj) ->
                new ColorTag(255 - obj.red, 255 - obj.green, 255 - obj.blue, obj.alpha));

        /* @doc tag
         *
         * @Name grayscale
         * @RawName <ColorTag.grayscale>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @NoArg
         * @Description
         * Returns a grayscale version of this color using standard luminance weights.
         * @Usage
         * // Narrates "color@54,54,54,255"
         * - narrate <color[255,0,0].grayscale>
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "grayscale", (attr, obj) -> {
            int luminance = clamp((int) (obj.red * 0.2126 + obj.green * 0.7152 + obj.blue * 0.0722));
            return new ColorTag(luminance, luminance, luminance, obj.alpha);
        });
    }

    public ColorTag(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public ColorTag(int red, int green, int blue, int alpha) {
        this.red   = clamp(red);
        this.green = clamp(green);
        this.blue  = clamp(blue);
        this.alpha = clamp(alpha);
    }

    public ColorTag(int packedRgb) {
        this((packedRgb >> 16) & 0xFF, (packedRgb >> 8) & 0xFF, packedRgb & 0xFF);
    }

    public ColorTag(String raw) {
        if (raw == null || raw.isEmpty()) { red = green = blue = 0; alpha = 255; return; }
        if (raw.startsWith(prefix + "@")) raw = raw.substring(prefix.length() + 1);
        if (raw.startsWith("#")) {
            String hex = raw.substring(1);
            int r = 0, g = 0, b = 0, a = 255;
            try {
                if (hex.length() >= 6) {
                    r = Integer.parseInt(hex.substring(0, 2), 16);
                    g = Integer.parseInt(hex.substring(2, 4), 16);
                    b = Integer.parseInt(hex.substring(4, 6), 16);
                }
                if (hex.length() >= 8) {
                    a = Integer.parseInt(hex.substring(6, 8), 16);
                }
            } catch (NumberFormatException ignored) {}
            red = r; green = g; blue = b; alpha = a;
            return;
        }
        String[] parts = raw.split(",", 4);
        int r = 0, g = 0, b = 0, a = 255;
        try {
            if (parts.length > 0) r = Integer.parseInt(parts[0].trim());
            if (parts.length > 1) g = Integer.parseInt(parts[1].trim());
            if (parts.length > 2) b = Integer.parseInt(parts[2].trim());
            if (parts.length > 3) a = Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException ignored) {}
        red = clamp(r); green = clamp(g); blue = clamp(b); alpha = clamp(a);
    }

    public String getHex(boolean hasAlpha) {
        if (hasAlpha) {
            return String.format("#%02X%02X%02X%02X", red, green, blue, alpha);
        }
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public int asRGB() {
        return red << 16 | green << 8 | blue;
    }

    public Color asBukkitColor() {
        return Color.fromARGB(alpha, red, green, blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + red + "," + green + "," + blue + "," + alpha;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return TAG_PROCESSOR.process(this, attribute); }

    @Override
    public @NonNull TagProcessor<ColorTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NonNull String getTestValue() { return "color@255,0,0,255"; }
}
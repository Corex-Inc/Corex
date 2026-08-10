package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.HexFormat;

/* @doc object
 *
 * @Name ColorTag
 * @Prefix color
 * @Format
 * The basic syntax for instantiating a ColorTag is 'color@R,G,B' or 'color@R,G,B,A'.
 * The color channels (red, green, blue, alpha) must be integer values from 0 to 255.
 * Additionally, standard hex strings like '#RRGGBB' and '#RRGGBBAA' are natively supported.
 * For example, a fully opaque red is 'color@255,0,0', and a semi-transparent blue could be 'color@0,0,255,128'.
 *
 * @Description
 * A ColorTag is an optimized wrapper representing an RGBA color model.
 * Each of the four channels (Red, Green, Blue, Alpha) is strictly clamped to an 8-bit range (0-255).
 * If the alpha channel is not explicitly provided during construction, it defaults to 255 (completely opaque).
 * This tag provides highly performant conversions to various color spaces like RGB, ARGB integers, and HSV/HSB.
 *
 * @Implements ColorTag
 */
public class ColorTag implements AbstractTag {

    private static final String PREFIX = "color";
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    public final int red;
    public final int green;
    public final int blue;
    public final int alpha;

    public static final TagProcessor<ColorTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(PREFIX, attr -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(attr.getParam());
        });
        ObjectFetcher.registerFetcher(PREFIX, ColorTag::new);

        /* @doc tag
         *
         * @Name red
         * @RawName <ColorTag.red>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Retrieves the red component of this color, constrained between 0 and 255.
         *
         * @Implements ColorTag.red
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "red", (attr, obj) -> new ElementTag(obj.red)).setAsyncSafe();

        /* @doc tag
         *
         * @Name green
         * @RawName <ColorTag.green>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Retrieves the green component of this color, constrained between 0 and 255.
         *
         * @Implements ColorTag.green
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "green", (attr, obj) -> new ElementTag(obj.green)).setAsyncSafe();

        /* @doc tag
         *
         * @Name blue
         * @RawName <ColorTag.blue>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Retrieves the blue component of this color, constrained between 0 and 255.
         *
         * @Implements ColorTag.blue
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "blue", (attr, obj) -> new ElementTag(obj.blue)).setAsyncSafe();

        /* @doc tag
         *
         * @Name alpha
         * @RawName <ColorTag.alpha>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Retrieves the alpha (opacity) component of this color. 255 means fully visible, 0 means invisible.
         *
         * @Implements ColorTag.alpha
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "alpha", (attr, obj) -> new ElementTag(obj.alpha)).setAsyncSafe();

        /* @doc tag
         *
         * @Name hex
         * @RawName <ColorTag.hex>
         * @Object ColorTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Yields the hexadecimal string representation of the color in '#RRGGBB' format.
         * Using the sub-tag '.withAlpha' appends the alpha channel, resulting in '#RRGGBBAA'.
         *
         * @Implements ColorTag.hex
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hex", (attr, obj) -> {
            boolean hasAlpha = attr.matchesNext("withAlpha");
            if (hasAlpha) attr.fulfill(1);
            return new ElementTag(obj.getHex(hasAlpha));
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name rgbInteger
         * @RawName <ColorTag.rgbInteger>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Packs the Red, Green, and Blue channels into a single 24-bit integer.
         * This format is often required by low-level server APIs or NMS tools.
         *
         * @Implements ColorTag.rgb_integer
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "rgbInteger", (attr, obj) ->
                new ElementTag((obj.red << 16) | (obj.green << 8) | obj.blue)).setAsyncSafe();

        /* @doc tag
         *
         * @Name argbInteger
         * @RawName <ColorTag.argbInteger>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Packs the Alpha, Red, Green, and Blue channels into a single 32-bit integer.
         * Alpha occupies the highest-order bytes.
         *
         * @Implements ColorTag.argb_integer
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "argbInteger", (attr, obj) ->
                new ElementTag((obj.alpha << 24) | (obj.red << 16) | (obj.green << 8) | obj.blue)).setAsyncSafe();

        /* @doc tag
         *
         * @Name rgb
         * @RawName <ColorTag.rgb>
         * @Object ColorTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Outputs a simple comma-separated string containing the red, green, and blue values (e.g., '255,0,0').
         *
         * @Implements ColorTag.rgb
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "rgb", (attr, obj) ->
                new ElementTag(obj.red + "," + obj.green + "," + obj.blue)).setAsyncSafe();

        /* @doc tag
         *
         * @Name rgba
         * @RawName <ColorTag.rgba>
         * @Object ColorTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Outputs a comma-separated string containing the red, green, blue, and alpha values.
         *
         * @Implements ColorTag.rgba
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "rgba", (attr, obj) ->
                new ElementTag(obj.red + "," + obj.green + "," + obj.blue + "," + obj.alpha)).setAsyncSafe();

        /* @doc tag
         *
         * @Name hue
         * @RawName <ColorTag.hue>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Extracts the hue component from the color's HSV model, converted to a 0-255 scale.
         *
         * @Implements ColorTag.hue
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hue", (attr, obj) ->
                new ElementTag(obj.toHSV()[0])).setAsyncSafe();

        /* @doc tag
         *
         * @Name saturation
         * @RawName <ColorTag.saturation>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Extracts the saturation value from the color's HSV model, converted to a 0-255 scale.
         *
         * @Implements ColorTag.saturation
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "saturation", (attr, obj) ->
                new ElementTag(obj.toHSV()[1])).setAsyncSafe();

        /* @doc tag
         *
         * @Name brightness
         * @RawName <ColorTag.brightness>
         * @Object ColorTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Extracts the brightness (value) component from the color's HSV model, converted to a 0-255 scale.
         *
         * @Implements ColorTag.brightness
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "brightness", (attr, obj) ->
                new ElementTag(obj.toHSV()[2])).setAsyncSafe();

        /* @doc tag
         *
         * @Name hsv
         * @RawName <ColorTag.hsv>
         * @Object ColorTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Retrieves a comma-separated string of the Hue, Saturation, and Brightness metrics, each scaled 0-255.
         *
         * @Implements ColorTag.hsv
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hsv", (attr, obj) -> {
            int[] hsv = obj.toHSV();
            return new ElementTag(hsv[0] + "," + hsv[1] + "," + hsv[2]);
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name toList
         * @RawName <ColorTag.toList>
         * @Object ColorTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
         * @Description
         * Transforms the RGB channels into a ListTag formatted as R|G|B.
         * Appending '.withAlpha' modifies the output to R|G|B|A.
         *
         * @Implements ColorTag.to_list
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "toList", (attr, obj) -> {
            ListTag result = new ListTag();
            result.addString(Integer.toString(obj.red));
            result.addString(Integer.toString(obj.green));
            result.addString(Integer.toString(obj.blue));
            if (attr.matchesNext("withAlpha")) {
                attr.fulfill(1);
                result.addString(Integer.toString(obj.alpha));
            }
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name withRed
         * @RawName <ColorTag.withRed[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a cloned ColorTag with its red channel substituted by the specified value (0-255).
         *
         * @Implements ColorTag.with_red[<red>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withRed", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(clamp(new ElementTag(attr.getParam()).asInt()), obj.green, obj.blue, obj.alpha);
        }).test("128").setAsyncSafe();

        /* @doc tag
         *
         * @Name withGreen
         * @RawName <ColorTag.withGreen[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a cloned ColorTag with its green channel substituted by the specified value (0-255).
         *
         * @Implements ColorTag.with_green[<green>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withGreen", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(obj.red, clamp(new ElementTag(attr.getParam()).asInt()), obj.blue, obj.alpha);
        }).test("200").setAsyncSafe();

        /* @doc tag
         *
         * @Name withBlue
         * @RawName <ColorTag.withBlue[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a cloned ColorTag with its blue channel substituted by the specified value (0-255).
         *
         * @Implements ColorTag.with_blue[<blue>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withBlue", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(obj.red, obj.green, clamp(new ElementTag(attr.getParam()).asInt()), obj.alpha);
        }).test("200").setAsyncSafe();

        /* @doc tag
         *
         * @Name withAlpha
         * @RawName <ColorTag.withAlpha[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a cloned ColorTag with its alpha channel substituted by the specified value (0-255).
         *
         * @Implements ColorTag.with_alpha[<alpha>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withAlpha", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ColorTag(obj.red, obj.green, obj.blue, clamp(new ElementTag(attr.getParam()).asInt()));
        }).test("128").setAsyncSafe();

        /* @doc tag
         *
         * @Name withHue
         * @RawName <ColorTag.withHue[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Generates a new ColorTag using a replaced hue value (0-255), keeping the original saturation and brightness intact.
         *
         * @Implements ColorTag.with_hue[<hue>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withHue", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int[] hsv = obj.toHSV();
            return fromHSV(clamp(new ElementTag(attr.getParam()).asInt()), hsv[1], hsv[2], obj.alpha);
        }).test("150").setAsyncSafe();

        /* @doc tag
         *
         * @Name withSaturation
         * @RawName <ColorTag.withSaturation[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Generates a new ColorTag using a replaced saturation value (0-255), keeping original hue and brightness.
         *
         * @Implements ColorTag.with_saturation[<saturation>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withSaturation", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int[] hsv = obj.toHSV();
            return fromHSV(hsv[0], clamp(new ElementTag(attr.getParam()).asInt()), hsv[2], obj.alpha);
        }).test("150").setAsyncSafe();

        /* @doc tag
         *
         * @Name withBrightness
         * @RawName <ColorTag.withBrightness[<#>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Generates a new ColorTag using a replaced brightness value (0-255), keeping original hue and saturation.
         *
         * @Implements ColorTag.with_brightness[<brightness>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "withBrightness", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int[] hsv = obj.toHSV();
            return fromHSV(hsv[0], hsv[1], clamp(new ElementTag(attr.getParam()).asInt()), obj.alpha);
        }).test("150").setAsyncSafe();

        /* @doc tag
         *
         * @Name mix
         * @RawName <ColorTag.mix[<color>]>
         * @Object ColorTag
         * @ReturnType ColorTag
         * @ArgRequired
         * @Async
         * @Description
         * Computes the blended result of this color mixed with another.
         * You can chain '.by[<0.0-1.0>]' to set a custom blending ratio (the default is 0.5 for an exact 50/50 split).
         *
         * @Implements ColorTag.mix[<color>]
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "mix", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ColorTag other = new ColorTag(attr.getParam());
            float factor = 0.5f;
            if (attr.matchesNext("by") && attr.hasNextParam()) {
                factor = (float) Math.clamp(new ElementTag(attr.getNextParam()).asDouble(), 0.0, 1.0);
                attr.fulfill(1);
            }
            return new ColorTag(
                    clamp(Math.round(obj.red   + (other.red   - obj.red)   * factor)),
                    clamp(Math.round(obj.green + (other.green - obj.green) * factor)),
                    clamp(Math.round(obj.blue  + (other.blue  - obj.blue)  * factor)),
                    clamp(Math.round(obj.alpha + (other.alpha - obj.alpha) * factor))
            );
        }).test("color@0,255,0").setAsyncSafe();
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
        if (raw == null || raw.isBlank()) {
            this.red = this.green = this.blue = 0;
            this.alpha = 255;
            return;
        }

        String cleanRaw = raw.startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;

        if (cleanRaw.startsWith("#")) {
            String hex = cleanRaw.substring(1);
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
            this.red = clamp(r); this.green = clamp(g); this.blue = clamp(b); this.alpha = clamp(a);
            return;
        }

        String[] parts = cleanRaw.split(",", 4);
        int r = 0, g = 0, b = 0, a = 255;
        try {
            if (parts.length > 0) r = Integer.parseInt(parts[0].trim());
            if (parts.length > 1) g = Integer.parseInt(parts[1].trim());
            if (parts.length > 2) b = Integer.parseInt(parts[2].trim());
            if (parts.length > 3) a = Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException ignored) {}
        this.red = clamp(r); this.green = clamp(g); this.blue = clamp(b); this.alpha = clamp(a);
    }

    public String getHex(boolean hasAlpha) {
        if (hasAlpha) {
            return "#" + HEX_FORMAT.formatHex(new byte[]{(byte) red, (byte) green, (byte) blue, (byte) alpha});
        } else {
            return "#" + HEX_FORMAT.formatHex(new byte[]{(byte) red, (byte) green, (byte) blue});
        }
    }

    public int asRGB() {
        return (red << 16) | (green << 8) | blue;
    }

    public int[] toHSV() {
        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0f;
        if (delta != 0f) {
            if (max == r) {
                h = (g - b) / delta;
            } else if (max == g) {
                h = 2f + (b - r) / delta;
            } else {
                h = 4f + (r - g) / delta;
            }
        }

        h /= 6.0f;
        if (h < 0f) h += 1.0f;

        float s = (max == 0f) ? 0f : delta / max;
        return new int[]{
                Math.round(h * 255f),
                Math.round(s * 255f),
                Math.round(max * 255f)
        };
    }

    public static ColorTag fromHSV(int h, int s, int v, int alpha) {
        float hue = h / 255.0f;
        float sat = s / 255.0f;
        float val = v / 255.0f;

        int r = 0, g = 0, b = 0;
        if (sat == 0f) {
            r = g = b = Math.round(val * 255f);
        } else {
            float hueScaled = (hue - (float) Math.floor(hue)) * 6.0f;
            float fragment = hueScaled - (float) Math.floor(hueScaled);
            float p = val * (1.0f - sat);
            float q = val * (1.0f - sat * fragment);
            float t = val * (1.0f - sat * (1.0f - fragment));

            switch ((int) hueScaled) {
                case 0 -> { r = Math.round(val * 255f); g = Math.round(t * 255f); b = Math.round(p * 255f); }
                case 1 -> { r = Math.round(q * 255f); g = Math.round(val * 255f); b = Math.round(p * 255f); }
                case 2 -> { r = Math.round(p * 255f); g = Math.round(val * 255f); b = Math.round(t * 255f); }
                case 3 -> { r = Math.round(p * 255f); g = Math.round(q * 255f); b = Math.round(val * 255f); }
                case 4 -> { r = Math.round(t * 255f); g = Math.round(p * 255f); b = Math.round(val * 255f); }
                case 5 -> { r = Math.round(val * 255f); g = Math.round(p * 255f); b = Math.round(q * 255f); }
            }
        }
        return new ColorTag(r, g, b, alpha);
    }

    private static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        if (alpha == 255) return PREFIX + "@" + red + "," + green + "," + blue;
        return PREFIX + "@" + red + "," + green + "," + blue + "," + alpha;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<ColorTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "color@255,0,0,255";
    }
}
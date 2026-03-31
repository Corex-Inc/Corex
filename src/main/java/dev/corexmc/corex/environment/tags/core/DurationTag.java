package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.tags.TagManager;
import org.jspecify.annotations.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a duration of time stored internally as ticks (doubles for sub-tick precision).
 *
 * <pre>
 * Syntax:  du@&lt;expression&gt;
 *
 * Expression is a sequence of number+unit pairs (in any order):
 *   h  → hours     (1h = 72 000t)
 *   m  → minutes   (1m = 1 200t)
 *   s  → seconds   (1s = 20t)
 *   t  → ticks     (1t = 1t)
 *
 * Examples:
 *   &lt;duration[2m34s5t]&gt;   →  2 min + 34 sec + 5 ticks
 *   &lt;duration[20m]&gt;        →  20 minutes
 *   &lt;duration[1t]&gt;         →  1 tick
 *   &lt;duration[0.1t]&gt;       →  0.1 ticks  (≈ 5 ms)
 *   &lt;duration[1h30m]&gt;      →  1 hour 30 minutes
 * </pre>
 */
public class DurationTag implements AbstractTag {

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------

    public static final double TICKS_PER_SECOND = 20.0;
    public static final double TICKS_PER_MINUTE = TICKS_PER_SECOND * 60.0;
    public static final double TICKS_PER_HOUR   = TICKS_PER_MINUTE * 60.0;
    public static final double MS_PER_TICK       = 50.0; // 1 tick = 50 ms

    /** Matches one segment of the duration string, e.g. "2m", "34s", "0.1t" */
    private static final Pattern SEGMENT = Pattern.compile("([0-9]*\\.?[0-9]+)([hmst])");

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private static final String PREFIX = "du";
    private final double ticks; // canonical internal unit

    public static final TagProcessor<DurationTag> PROCESSOR = new TagProcessor<>();

    // ---------------------------------------------------------------
    // Registration
    // ---------------------------------------------------------------

    public static void register() {
        // Base tag: <duration[2m34s]>
        TagManager.registerBaseTag("duration", attr -> new DurationTag(attr.getParam()));

        // Object fetcher: du@2m34s
        ObjectFetcher.registerFetcher(PREFIX, DurationTag::new);

        // --- Attribute tags ---

        // Raw ticks (double)
        PROCESSOR.registerTag(ElementTag.class, "ticks",
                        (attr, obj) -> new ElementTag(obj.ticks))
                .test("40.0");

        // Whole ticks (long, rounded)
        PROCESSOR.registerTag(ElementTag.class, "ticks_long",
                        (attr, obj) -> new ElementTag((long) Math.round(obj.ticks)))
                .test("40");

        // Total seconds
        PROCESSOR.registerTag(ElementTag.class, "seconds",
                        (attr, obj) -> new ElementTag(obj.ticks / TICKS_PER_SECOND))
                .test("2.0");

        // Total minutes
        PROCESSOR.registerTag(ElementTag.class, "minutes",
                        (attr, obj) -> new ElementTag(obj.ticks / TICKS_PER_MINUTE))
                .test("0.5");

        // Total hours
        PROCESSOR.registerTag(ElementTag.class, "hours",
                        (attr, obj) -> new ElementTag(obj.ticks / TICKS_PER_HOUR))
                .test("0.008333333333333333");

        // Total milliseconds
        PROCESSOR.registerTag(ElementTag.class, "milliseconds",
                        (attr, obj) -> new ElementTag(obj.ticks * MS_PER_TICK))
                .test("2000.0");

        // Human-readable string, e.g. "2m 34s 5t"
        PROCESSOR.registerTag(ElementTag.class, "formatted",
                        (attr, obj) -> new ElementTag(obj.format()))
                .test("2m 34s 5t");
    }

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Parse a human-readable duration expression like {@code 2m34s5t} or {@code 0.1t}. */
    public DurationTag(String raw) {
        this.ticks = parse(raw);
    }

    /** Construct directly from a known tick count. */
    public DurationTag(double ticks) {
        this.ticks = Math.max(0.0, ticks);
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    /** Raw tick count (double for sub-tick precision). */
    public double getTicks() {
        return ticks;
    }

    /** Ticks rounded to the nearest long — safe to pass to Bukkit schedulers. */
    public long getTicksLong() {
        return Math.max(1L, Math.round(ticks));
    }

    /** Total milliseconds — safe to pass to Folia's async scheduler. */
    public long getMilliseconds() {
        return Math.max(1L, Math.round(ticks * MS_PER_TICK));
    }

    // ---------------------------------------------------------------
    // Parsing
    // ---------------------------------------------------------------

    /**
     * Converts a duration expression into ticks.
     * Accepts any combination of {@code h}, {@code m}, {@code s}, {@code t} segments.
     * Unrecognised strings fall back to 0.
     */
    private static double parse(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;

        // Strip "du@" prefix if present
        String expr = raw.trim().toLowerCase();
        if (expr.startsWith(PREFIX + "@")) {
            expr = expr.substring(PREFIX.length() + 1);
        }

        double total = 0.0;
        Matcher m = SEGMENT.matcher(expr);
        boolean matched = false;

        while (m.find()) {
            matched = true;
            double value = Double.parseDouble(m.group(1));
            char unit = m.group(2).charAt(0);
            total += switch (unit) {
                case 'h' -> value * TICKS_PER_HOUR;
                case 'm' -> value * TICKS_PER_MINUTE;
                case 's' -> value * TICKS_PER_SECOND;
                case 't' -> value;
                default  -> 0.0;
            };
        }

        // Fallback: bare number with no unit → treat as seconds
        if (!matched) {
            try {
                total = Double.parseDouble(expr) * TICKS_PER_SECOND;
            } catch (NumberFormatException ignored) {}
        }

        return Math.max(0.0, total);
    }

    // ---------------------------------------------------------------
    // Formatting
    // ---------------------------------------------------------------

    /**
     * Returns a compact human-readable string, e.g. {@code "1h 30m 5s 2t"}.
     * Only non-zero components are included.
     */
    private String format() {
        long remaining = (long) ticks;
        double fractional = ticks - remaining;

        long hours   = remaining / (long) TICKS_PER_HOUR;
        remaining   %= (long) TICKS_PER_HOUR;
        long minutes = remaining / (long) TICKS_PER_MINUTE;
        remaining   %= (long) TICKS_PER_MINUTE;
        long seconds = remaining / (long) TICKS_PER_SECOND;
        long leftover = remaining % (long) TICKS_PER_SECOND;

        // Include sub-tick fraction in the tick component
        double ticksComponent = leftover + fractional;

        StringBuilder sb = new StringBuilder();
        if (hours   > 0)                    sb.append(hours).append("h ");
        if (minutes > 0)                    sb.append(minutes).append("m ");
        if (seconds > 0)                    sb.append(seconds).append("s ");
        if (ticksComponent > 0.0 || sb.isEmpty()) {
            // Show as integer if whole, otherwise one decimal place
            if (ticksComponent == Math.floor(ticksComponent)) {
                sb.append((long) ticksComponent).append("t");
            } else {
                sb.append(String.format("%.1f", ticksComponent)).append("t");
            }
        }

        return sb.toString().trim();
    }

    // ---------------------------------------------------------------
    // AbstractTag
    // ---------------------------------------------------------------

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull AbstractTag setPrefix(@NonNull String prefix) {
        // Prefix is a constant for DurationTag
        return this;
    }

    @Override
    public @NonNull String identify() {
        // Canonical form always in ticks so it round-trips cleanly
        return PREFIX + "@" + ticks + "t";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public TagProcessor<DurationTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public String getTestValue() {
        return "du@2m34s5t";
    }
}
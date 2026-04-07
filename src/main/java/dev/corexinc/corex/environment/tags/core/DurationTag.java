package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* @doc object
 *
 * @Name DurationTag
 * @Prefix dur
 * @Format
 * The identity format for DurationTags' is the number of ticks, followed by an "t".
 *
 * @Description
 * Durations are a unified and convenient way to get a 'unit of time' throughout Corex.
 * Many commands and features that require a duration can be satisfied by specifying a number and unit of time, especially command arguments that are prefixed 'duration:', etc.
 * The unit of time can be specified by using one of the following:
 * t=ticks (0.05 seconds), s=seconds, m=minutes (60 seconds), h=hours (60 minutes), d=days (24 hours), w=weeks (7 days), y=years (365 days).
 * Not using a unit will imply seconds.
 * You can use multiple types in a single DurationTag in any order.
 * Examples: 10s, 50m6s, 1d54m8s1t, 20.
 *
 * The input of 'inst' or 'inf' will be interpreted as 0 (for use with commands where instant/infinite logic applies).
 *
 * @Usage
 * // Use for delay queue for 1 minute 10 ticks
 * - wait <duration[1s10t]>
 */
public class DurationTag implements AbstractTag {

    public static final double TICKS_PER_SECOND = 20.0;
    public static final double TICKS_PER_MINUTE = TICKS_PER_SECOND * 60.0;
    public static final double TICKS_PER_HOUR = TICKS_PER_MINUTE * 60.0;
    public static final double MS_PER_TICK = 50.0;

    private static final Pattern SEGMENT = Pattern.compile("([0-9]*\\.?[0-9]+)([hmst])");

    private static final String prefix = "dur";
    private final double ticks;

    public static final TagProcessor<DurationTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("duration", attr -> new DurationTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, DurationTag::new);

        /* @doc tag
         *
         * @Name ticks
         * @RawName <DurationTag.ticks>
         * @Object DurationTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of ticks in the duration.
         *
         * @Implements DurationTag.in_ticks
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ticks", (attr, obj) ->
                new ElementTag(obj.ticks));

        /* @doc tag
         *
         * @Name seconds
         * @RawName <DurationTag.seconds>
         * @Object DurationTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of seconds in the duration.
         *
         * @Implements DurationTag.in_seconds
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "seconds", (attr, obj) ->
                new ElementTag(obj.ticks / TICKS_PER_SECOND));

        /* @doc tag
         *
         * @Name minutes
         * @RawName <DurationTag.minutes>
         * @Object DurationTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of minutes in the duration.
         *
         * @Implements DurationTag.in_minutes
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "minutes", (attr, obj) ->
                new ElementTag(obj.ticks / TICKS_PER_MINUTE));

        /* @doc tag
         *
         * @Name hours
         * @RawName <DurationTag.hours>
         * @Object DurationTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of hours in the duration.
         *
         * @Implements DurationTag.in_hours
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hours", (attr, obj) ->
                new ElementTag(obj.ticks / TICKS_PER_HOUR));

        /* @doc tag
         *
         * @Name milliseconds
         * @RawName <DurationTag.milliseconds>
         * @Object DurationTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of milliseconds in the duration.
         *
         * @Implements DurationTag.in_milliseconds
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "milliseconds", (attr, obj) ->
                new ElementTag(obj.ticks * MS_PER_TICK));

        /* @doc tag
         *
         * @Name formatted
         * @RawName <DurationTag.formatted>
         * @Object DurationTag
         * @ReturnType ElementTag
         * @Description
         * Returns the value of the duration in an easily readable format like 2h 30m.
         * Will show seconds, minutes, hours, days, and/or years.
         *
         * @Implements DurationTag.formatted
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "formatted", (attr, obj) ->
                new ElementTag(obj.format()));

        /* @doc tag
         *
         * @Name add[]
         * @RawName <DurationTag.add[<duration>]>
         * @Object DurationTag
         * @ReturnType DurationTag
         * @Description
         * Returns this duration + another.
         *
         * @Implements DurationTag.add[<duration>]
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "add", (attr, obj) ->
                new DurationTag(obj.ticks + new DurationTag(attr.getParam()).getTicks())).test("4s");

        /* @doc tag
         *
         * @Name sub[]
         * @RawName <DurationTag.sub[<duration>]>
         * @Object DurationTag
         * @ReturnType DurationTag
         * @Description
         * Returns this duration - another.
         *
         * @Implements DurationTag.sub[<duration>]
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "sub", (attr, obj) ->
                new DurationTag(Math.max(0.0, obj.ticks - new DurationTag(attr.getParam()).getTicks()))).test("4s");

        /* @doc tag
         *
         * @Name mul[]
         * @RawName <DurationTag.mul[<duration>]>
         * @Object DurationTag
         * @ReturnType DurationTag
         * @Description
         * Returns this duration * another.
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "mul", (attr, obj) -> {
            String param = attr.getParam();
            if (param == null || param.isBlank()) return obj;
            return new DurationTag(obj.ticks * new DurationTag(param).getTicks());
        }).test("4s");

        /* @doc tag
         *
         * @Name div[]
         * @RawName <DurationTag.div[<duration>]>
         * @Object DurationTag
         * @ReturnType DurationTag
         * @Description
         * Returns this duration / another.
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "div", (attr, obj) -> {
            String param = attr.getParam();
            if (param == null || param.isBlank()) return obj;
            double divisor = new DurationTag(param).getTicks();
            return divisor != 0 ? new DurationTag(obj.ticks / divisor) : new DurationTag(0.0);
        }).test("4s");
    }

    public DurationTag(String raw) {
        this.ticks = parse(raw);
    }

    public DurationTag(double ticks) {
        this.ticks = Math.max(0.0, ticks);
    }


    public double getTicks() {
        return ticks;
    }

    public long getTicksLong() {
        return Math.max(1L, Math.round(ticks));
    }

    public long getMilliseconds() {
        return Math.max(1L, Math.round(ticks * MS_PER_TICK));
    }

    private static double parse(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;

        String expr = raw.trim().toLowerCase();
        if (expr.startsWith(prefix + "@")) {
            expr = expr.substring(prefix.length() + 1);
        }

        double total = 0.0;
        Matcher matcher = SEGMENT.matcher(expr);
        boolean matched = false;

        while (matcher.find()) {
            matched = true;
            double value = Double.parseDouble(matcher.group(1));
            char unit = matcher.group(2).charAt(0);
            total += switch (unit) {
                case 'h' -> value * TICKS_PER_HOUR;
                case 'm' -> value * TICKS_PER_MINUTE;
                case 's' -> value * TICKS_PER_SECOND;
                case 't' -> value;
                default -> 0.0;
            };
        }

        if (!matched) {
            try {
                total = Double.parseDouble(expr) * TICKS_PER_SECOND;
            } catch (NumberFormatException ignored) {}
        }

        return Math.max(0.0, total);
    }

    private String format() {
        long remaining = (long) ticks;
        double fractional = ticks - remaining;

        long hours = remaining / (long) TICKS_PER_HOUR;
        remaining %= (long) TICKS_PER_HOUR;
        long minutes = remaining / (long) TICKS_PER_MINUTE;
        remaining %= (long) TICKS_PER_MINUTE;
        long seconds = remaining / (long) TICKS_PER_SECOND;
        long leftover = remaining % (long) TICKS_PER_SECOND;

        double ticksComponent = leftover + fractional;

        StringBuilder stringBuilder = new StringBuilder();
        if (hours   > 0) stringBuilder.append(hours).append("h ");
        if (minutes > 0) stringBuilder.append(minutes).append("m ");
        if (seconds > 0) stringBuilder.append(seconds).append("s ");
        if (ticksComponent > 0.0 || stringBuilder.isEmpty()) {
            if (ticksComponent == Math.floor(ticksComponent)) {
                stringBuilder.append((long) ticksComponent).append("t");
            } else {
                stringBuilder.append(String.format("%.1f", ticksComponent)).append("t");
            }
        }

        return stringBuilder.toString().trim();
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + ticks + "t";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<DurationTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "dur@2m34s5t";
    }
}
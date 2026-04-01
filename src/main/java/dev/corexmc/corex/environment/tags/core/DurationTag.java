package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationTag implements AbstractTag {

    public static final double TICKS_PER_SECOND = 20.0;
    public static final double TICKS_PER_MINUTE = TICKS_PER_SECOND * 60.0;
    public static final double TICKS_PER_HOUR   = TICKS_PER_MINUTE * 60.0;
    public static final double MS_PER_TICK       = 50.0; // 1 tick = 50 ms

    private static final Pattern SEGMENT = Pattern.compile("([0-9]*\\.?[0-9]+)([hmst])");

    private static final String prefix = "dur";
    private final double ticks;

    public static final TagProcessor<DurationTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("duration", attr -> new DurationTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, DurationTag::new);

        PROCESSOR.registerTag(ElementTag.class, "ticks", (attr, obj) ->
                new ElementTag(obj.ticks));

        PROCESSOR.registerTag(ElementTag.class, "seconds", (attr, obj) ->
                new ElementTag(obj.ticks / TICKS_PER_SECOND));

        PROCESSOR.registerTag(ElementTag.class, "minutes", (attr, obj) ->
                new ElementTag(obj.ticks / TICKS_PER_MINUTE));

        PROCESSOR.registerTag(ElementTag.class, "hours", (attr, obj) ->
                new ElementTag(obj.ticks / TICKS_PER_HOUR));

        PROCESSOR.registerTag(ElementTag.class, "milliseconds", (attr, obj) ->
                new ElementTag(obj.ticks * MS_PER_TICK));

        PROCESSOR.registerTag(ElementTag.class, "formatted", (attr, obj) ->
                new ElementTag(obj.format()));
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

        long hours   = remaining / (long) TICKS_PER_HOUR;
        remaining   %= (long) TICKS_PER_HOUR;
        long minutes = remaining / (long) TICKS_PER_MINUTE;
        remaining   %= (long) TICKS_PER_MINUTE;
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
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public TagProcessor<DurationTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public String getTestValue() {
        return "dur@2m34s5t";
    }
}
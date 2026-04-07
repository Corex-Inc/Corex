package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;

import java.nio.charset.Charset;

public class UtilTag implements AbstractTag {

    private static final String PREFIX = "util";
    public static final TagProcessor<UtilTag> TAG_PROCESSOR = new TagProcessor<>();

    private static final UtilTag INSTANCE = new UtilTag();

    public static void register() {
        ObjectFetcher.registerFetcher(PREFIX, s -> INSTANCE);
        BaseTagProcessor.registerBaseTag("util", attr -> INSTANCE);

        /* @doc tag
         *
         * @Name serverTick
         * @RawName <UtilTag.serverTick>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of ticks since the server was started.
         * Note that this is NOT an accurate indicator for real server uptime, as ticks fluctuate based on server lag.
         *
         * @Implements util.current_tick
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "serverTick", (attr, obj) ->
                new ElementTag(Bukkit.getServer().getCurrentTick()));

        /* @doc tag
         *
         * @Name timeMillis
         * @RawName <UtilTag.timeMillis>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the number of milliseconds since Jan 1, 1970.
         *
         * @Implements util.current_time_millis
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "timeMillis", (attr, obj) ->
                new ElementTag(System.currentTimeMillis()));

        /* @doc tag
         *
         * @Name uptime
         * @RawName <UtilTag.uptime>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the uptime of the Java virtual machine (server) in milliseconds.
         *
         * @Implements util.real_time_since_start
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uptime", (attr, obj) ->
                new ElementTag(java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime()));

        /* @doc tag
         *
         * @Name debugMode
         * @RawName <UtilTag.debugMode>
         * @Object UtilTag
         * @ReturnType ElementTag
         * @Description
         * Returns the debug mode: NONE, ERROR, DEFAULT, VERBOSE
         *
         * @Implements util.debug_enabled
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "debugMode", (attr, obj) ->
                new ElementTag(Debugger.getMode().name()));

        /* @doc tag
         *
         * @Name defaultEncoding
         * @RawName <UtilTag.defaultEncoding>
         * @Object UtilTag
         * @ReturnType ElementTag
         * @Description
         * Returns the name of the default system text encoding charset, such as "UTF-8".
         *
         * @Implements util.default_encoding
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "defaultEncoding", (attr, obj) ->
                new ElementTag(Charset.defaultCharset().name()));

        /* @doc tag
         *
         * @Name pi
         * @RawName <UtilTag.pi>
         * @Object UtilTag
         * @ReturnType ElementTag(Decimal)
         * @Description
         * Returns PI: 3.14159265358979323846
         *
         * @Implements util.pi
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "pi", (attr, obj) -> new ElementTag(Math.PI));

        /* @doc tag
         *
         * @Name e
         * @RawName <UtilTag.e>
         * @Object UtilTag
         * @ReturnType ElementTag(Decimal)
         * @Description
         * Returns e (Euler's number): 2.7182818284590452354
         *
         * @Implements util.e
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "e", (attr, obj) -> new ElementTag(Math.E));

        /* @doc tag
         *
         * @Name tau
         * @RawName <UtilTag.tau>
         * @Object UtilTag
         * @ReturnType ElementTag(Decimal)
         * @Description
         * Returns Tau: 6.28318530717958647692
         *
         * @Implements util.tau
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "tau", (attr, obj) -> new ElementTag(Math.PI * 2));

        /* @doc tag
         *
         * @Name intMax
         * @RawName <UtilTag.intMax>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the maximum value of a 32 bit signed integer (a java 'int'): 2147483647
         *
         * @Implements util.int_max
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "intMax", (attr, obj) -> new ElementTag(Integer.MAX_VALUE));

        /* @doc tag
         *
         * @Name intMin
         * @RawName <UtilTag.intMin>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the minimum value of a 32 bit signed integer (a java 'int'): -2147483648
         *
         * @Implements util.int_min
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "intMin", (attr, obj) -> new ElementTag(Integer.MIN_VALUE));

        /* @doc tag
         *
         * @Name longMax
         * @RawName <UtilTag.longMax>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the maximum value of a 64 bit signed integer (a java 'long'): 9223372036854775807
         *
         * @Implements util.long_max
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "longMax", (attr, obj) -> new ElementTag(Long.MAX_VALUE));

        /* @doc tag
         *
         * @Name longMin
         * @RawName <UtilTag.longMin>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the minimum value of a 64 bit signed integer (a java 'long'): -9223372036854775808
         *
         * @Implements util.long_min
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "longMin", (attr, obj) -> new ElementTag(Long.MIN_VALUE));

        /* @doc tag
         *
         * @Name shortMax
         * @RawName <UtilTag.shortMax>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the maximum value of a 16 bit signed integer (a java 'short'): 32767
         *
         * @Implements util.short_max
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "shortMax", (attr, obj) -> new ElementTag(Short.MAX_VALUE));

        /* @doc tag
         *
         * @Name shortMin
         * @RawName <UtilTag.shortMin>
         * @Object UtilTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the minimum value of a 16 bit signed integer (a java 'short'): -32768
         *
         * @Implements util.short_min
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "shortMin", (attr, obj) -> new ElementTag(Short.MIN_VALUE));

        /* @doc tag
         *
         * @Name listNumbers
         * @RawName <UtilTag.listNumbers[to=<#>;(from=<#>/{1});(every=<#>/{1})]>
         * @Object UtilTag
         * @ReturnType ListTag
         * @Description
         * Returns a list of integer numbers in the specified range.
         * You must specify at least the 'to' input, you can optionally specify 'from' (default 1), and 'every' (default 1).
         * Note that this generally should not be used as input to the 'foreach' command. Instead, use {@link command repeat}.
         * @example
         * // Narrates "1, 2, and 3"
         * - narrate <util.listNumbers[to=3].formatted>
         * @example
         * // Narrates "3, 4, and 5"
         * - narrate <util.listNumbers[from=3;to=5].formatted>
         * @example
         * // Narrates "4, 8, and 12"
         * - narrate <util.listNumbers[from=4;to=12;every=4].formatted>
         *
         * @Implements util.list_numbers
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "listNumbers", (attr, obj) -> {
            if (!attr.hasParam()) return new ListTag("");

            long from = 1;
            long to = 1;
            long every = 1;

            String param = attr.getParam();

            if (param.contains("=")) {
                if (!param.startsWith("[")) param = "[" + param + "]";

                MapTag map = new MapTag(param);

                AbstractTag toTag = map.getObject("to");
                if (toTag != null) to = new ElementTag(toTag.identify()).asLong();

                AbstractTag fromTag = map.getObject("from");
                if (fromTag != null) from = new ElementTag(fromTag.identify()).asLong();

                AbstractTag everyTag = map.getObject("every");
                if (everyTag != null) every = new ElementTag(everyTag.identify()).asLong();

            } else {
                to = new ElementTag(param).asLong();
            }

            if (every <= 0) every = 1;

            ListTag resultList = new ListTag("");

            if (from <= to) {
                for (long i = from; i <= to; i += every) {
                    resultList.addString(String.valueOf(i));
                }
            } else {
                for (long i = from; i >= to; i -= every) {
                    resultList.addString(String.valueOf(i));
                }
            }

            return resultList;
        });

        /* @doc tag
         *
         * @Name random
         * @RawName <UtilTag.random>
         * @Object UtilTag
         * @ReturnType RandomTag
         * @Description
         * Returns a RandomTag object.
         * If a seed is specified, the generator becomes deterministic.
         */
        TAG_PROCESSOR.registerTag(RandomTag.class, "random", (attr, obj) -> {
            if (attr.hasParam()) {
                try { return new RandomTag(Long.parseLong(attr.getParam())); }
                catch (NumberFormatException ignored) {}
            }
            return RandomTag.getShared();
        });
    }

    public UtilTag(String raw) {}

    private UtilTag() {}

    @Override public @NonNull String getPrefix() { return PREFIX; }
    @Override public @NonNull String identify() { return PREFIX + "@"; }
    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return TAG_PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<UtilTag> getProcessor() { return TAG_PROCESSOR; }
    @Override public String getTestValue() { return PREFIX + "@"; }
}
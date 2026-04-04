package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;

public class UtilTag implements AbstractTag {

    private static final String PREFIX = "util";
    public static final TagProcessor<UtilTag> PROCESSOR = new TagProcessor<>();

    private static final UtilTag INSTANCE = new UtilTag();

    public static void register() {
        ObjectFetcher.registerFetcher(PREFIX, s -> INSTANCE);
        BaseTagProcessor.registerBaseTag("util", attr -> INSTANCE);

        PROCESSOR.registerTag(ElementTag.class, "serverTick", (attr, obj) ->
                new ElementTag(Bukkit.getServer().getCurrentTick()));

        PROCESSOR.registerTag(ElementTag.class, "timeMillis", (attr, obj) ->
                new ElementTag(System.currentTimeMillis()));

        PROCESSOR.registerTag(ElementTag.class, "uptime", (attr, obj) ->
                new ElementTag(java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime()));

        PROCESSOR.registerTag(ElementTag.class, "debugEnabled", (attr, obj) ->
                new ElementTag(Debugger.isDebugEnabled));

        PROCESSOR.registerTag(ElementTag.class, "defaultEncoding", (attr, obj) ->
                new ElementTag(java.nio.charset.Charset.defaultCharset().name()));

        PROCESSOR.registerTag(ElementTag.class, "pi", (attr, obj) -> new ElementTag(Math.PI));
        PROCESSOR.registerTag(ElementTag.class, "e", (attr, obj) -> new ElementTag(Math.E));
        PROCESSOR.registerTag(ElementTag.class, "tau", (attr, obj) -> new ElementTag(Math.PI * 2));

        PROCESSOR.registerTag(ElementTag.class, "intMax", (attr, obj) -> new ElementTag(Integer.MAX_VALUE));
        PROCESSOR.registerTag(ElementTag.class, "intMin", (attr, obj) -> new ElementTag(Integer.MIN_VALUE));
        PROCESSOR.registerTag(ElementTag.class, "longMax", (attr, obj) -> new ElementTag(Long.MAX_VALUE));
        PROCESSOR.registerTag(ElementTag.class, "longMin", (attr, obj) -> new ElementTag(Long.MIN_VALUE));
        PROCESSOR.registerTag(ElementTag.class, "shortMax", (attr, obj) -> new ElementTag(Short.MAX_VALUE));
        PROCESSOR.registerTag(ElementTag.class, "shortMin", (attr, obj) -> new ElementTag(Short.MIN_VALUE));

        PROCESSOR.registerTag(ListTag.class, "listNumbers", (attr, obj) -> {
            if (!attr.hasParam()) return null;

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

        PROCESSOR.registerTag(RandomTag.class, "random", (attr, obj) -> {
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
    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<UtilTag> getProcessor() { return PROCESSOR; }
    @Override public String getTestValue() { return PREFIX + "@"; }
}
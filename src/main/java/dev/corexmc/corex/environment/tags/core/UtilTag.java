package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

public class UtilTag implements AbstractTag {

    private static final String PREFIX = "util";
    public static final TagProcessor<UtilTag> PROCESSOR = new TagProcessor<>();

    private static final UtilTag INSTANCE = new UtilTag();

    public static void register() {
        ObjectFetcher.registerFetcher(PREFIX, s -> INSTANCE);
        BaseTagProcessor.registerBaseTag("util", attr -> INSTANCE);

        PROCESSOR.registerTag(RandomTag.class, "random", (attr, obj) -> {
            if (attr.hasParam()) {
                try {
                    long seed = Long.parseLong(attr.getParam());
                    return new RandomTag(seed);
                } catch (NumberFormatException ignored) {}
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
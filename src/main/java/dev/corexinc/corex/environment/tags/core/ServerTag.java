package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.jspecify.annotations.NonNull;

public class ServerTag implements AbstractTag {

    private final String prefix = "server";

    public static final TagProcessor<ServerTag> PROCESSOR = new TagProcessor<>();

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }



    @Override
    public @NonNull String identify() {
        return prefix + "@";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", (attribute) -> new ServerTag());
    }

    @Override
    public @NonNull TagProcessor<MaterialTag> getProcessor() {
        return null;
    }

    @Override
    public @NonNull String getTestValue() {
        return null;
    }
}

package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.environment.tags.world.MaterialTag;
import org.jspecify.annotations.NonNull;

public class ServerTag implements AbstractTag {

    private String prefix = "server";

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
    public TagProcessor<MaterialTag> getProcessor() {
        return null;
    }

    @Override
    public String getTestValue() {
        return null;
    }
}

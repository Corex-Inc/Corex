package dev.corexmc.corex.environment.tags;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.TagManager;
import org.jspecify.annotations.NonNull;

public class ServerTag implements AbstractTag {

    private String prefix = "server";

    public static final TagProcessor<ServerTag> PROCESSOR = new TagProcessor<>();

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull AbstractTag setPrefix(@NonNull String prefix) {
        this.prefix = prefix;
        return this;
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
        TagManager.registerBaseTag("server", (attribute) -> {
            return new ServerTag();
        });
    }
}

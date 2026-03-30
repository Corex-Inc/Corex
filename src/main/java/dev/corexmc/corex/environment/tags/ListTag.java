package dev.corexmc.corex.environment.tags;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.tags.TagManager;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ListTag implements AbstractTag {

    private static String prefix = "li";
    private final List<String> list = new ArrayList<>();

    public static final TagProcessor<ListTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        TagManager.registerBaseTag("list", attr -> new ListTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, ListTag::new);

        PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.list.size()));

        PROCESSOR.registerTag(ElementTag.class, "commaSeparated", (attr, obj) ->
                new ElementTag(String.join(", ", obj.list)));

        PROCESSOR.registerTag(ElementTag.class, "join", (attr, obj) ->
                new ElementTag(String.join(attr.getParam(), obj.list)));

        PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int index = new ElementTag(attr.getParam()).asInt() - 1;
            if (index >= 0 && index < obj.list.size()) {
                return ObjectFetcher.pickObject(obj.list.get(index));
            }
            return null;
        });

    }

    public ListTag(String raw) {
        if (raw == null || raw.isEmpty()) return;

        this.list.addAll(ObjectFetcher.splitIgnoringBrackets(raw, '|'));
    }

    public <T extends AbstractTag> List<T> filter(Class<T> clazz) {
        List<T> results = new java.util.ArrayList<>();
        for (String item : this.list) {
            AbstractTag obj = ObjectFetcher.pickObject(item);

            if (clazz.isInstance(obj)) {
                results.add(clazz.cast(obj));
            }
        }
        return results;
    }

    @Override public @NonNull String getPrefix() { return prefix; }
    @Override public @NonNull AbstractTag setPrefix(@NonNull String prefix) { this.prefix = prefix; return this; }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + String.join("|", list);
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }
}
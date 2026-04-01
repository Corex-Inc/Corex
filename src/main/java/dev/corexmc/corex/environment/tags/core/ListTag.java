package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ListTag implements AbstractTag {

    private static String prefix = "li";
    private final List<String> list = new ArrayList<>();

    public static final TagProcessor<ListTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("list", attr -> new ListTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, ListTag::new);

        PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.list.size()));

        PROCESSOR.registerTag(ElementTag.class, "join", (attr, obj) ->
                new ElementTag(String.join(attr.getParam(), obj.list))).test(", ");

        PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int index = new ElementTag(attr.getParam()).asInt() - 1;
            if (index >= 0 && index < obj.list.size()) {
                return ObjectFetcher.pickObject(obj.list.get(index));
            }
            return null;
        }).test("3");

        PROCESSOR.registerTag(AbstractTag.class, "random", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;

            if (!attr.hasParam()) {
                int index = ThreadLocalRandom.current().nextInt(obj.list.size());
                return ObjectFetcher.pickObject(obj.list.get(index));
            }

            int count = new ElementTag(attr.getParam()).asInt();
            if (count <= 0) return new ListTag("");

            List<String> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);

            int limit = Math.min(count, copy.size());
            List<String> subList = copy.subList(0, limit);

            return new ListTag(String.join("|", subList));
        }).test("2");

        PROCESSOR.registerTag(ListTag.class, "shuffled", (attr, obj) -> {
            if (obj.list.isEmpty()) return new ListTag("");

            List<String> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);

            return new ListTag(String.join("|", copy));
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

    public List<AbstractTag> getList() {
        List<AbstractTag> results = new ArrayList<>();
        for (String item : this.list) {
            AbstractTag obj = ObjectFetcher.pickObject(item);
            results.add(obj);
        }
        return results;
    }

    public int size() {
        return list.size();
    }

    public String get(int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + String.join("|", list);
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }

    @Override
    public TagProcessor<ListTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public String getTestValue() {
        return "li@a|b|c";
    }
}
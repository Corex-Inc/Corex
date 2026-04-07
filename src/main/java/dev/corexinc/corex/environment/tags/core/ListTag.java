package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ListTag implements AbstractTag {

    private static final String prefix = "li";
    private final List<AbstractTag> list = new ArrayList<>();

    public static final TagProcessor<ListTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("list", attr -> new ListTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, ListTag::new);

        TAG_PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.list.size()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "join", (attr, obj) -> {
            List<String> strings = new ArrayList<>();
            for (AbstractTag t : obj.list) strings.add(t.identify());
            return new ElementTag(String.join(attr.getParam(), strings));
        }).test(", ");

        TAG_PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int index = new ElementTag(attr.getParam()).asInt() - 1;
            if (index >= 0 && index < obj.list.size()) {
                return obj.list.get(index);
            }
            return null;
        }).test("3");

        TAG_PROCESSOR.registerTag(AbstractTag.class, "random", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;

            if (!attr.hasParam()) {
                int index = ThreadLocalRandom.current().nextInt(obj.list.size());
                return obj.list.get(index);
            }

            int count = new ElementTag(attr.getParam()).asInt();
            if (count <= 0) return new ListTag();

            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);

            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (AbstractTag t : copy.subList(0, limit)) result.addObject(t);

            return result;
        }).test("2");

        TAG_PROCESSOR.registerTag(ListTag.class, "shuffled", (attr, obj) -> {
            if (obj.list.isEmpty()) return new ListTag();

            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);

            ListTag result = new ListTag();
            for (AbstractTag t : copy) result.addObject(t);

            return result;
        });
    }

    public ListTag() {}

    public ListTag(String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (String s : ObjectFetcher.splitIgnoringBrackets(raw, '|')) {
            this.list.add(ObjectFetcher.pickObject(s));
        }
    }

    public ListTag(List<?> list) {
        for (Object element : list) {
            if (element == null) continue;
            this.list.add(ObjectFetcher.pickObject(element.toString()));
        }
    }

    public <T extends AbstractTag> List<T> filter(Class<T> clazz) {
        List<T> results = new ArrayList<>();
        for (AbstractTag item : this.list) {
            if (clazz.isInstance(item)) {
                results.add(clazz.cast(item));
            }
        }
        return results;
    }

    public List<AbstractTag> getList() {
        return new ArrayList<>(list);
    }

    public int size() {
        return list.size();
    }

    public String get(int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index).identify();
        }
        return null;
    }

    public void addString(String value) {
        if (value != null) {
            this.list.add(new ElementTag(value));
        }
    }

    public void addObject(AbstractTag tag) {
        if (tag != null) {
            this.list.add(tag);
        }
    }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        List<String> strings = new ArrayList<>();
        for (AbstractTag t : list) strings.add(t.identify());
        return prefix + "@" + String.join("|", strings);
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return TAG_PROCESSOR.process(this, attribute); }

    @Override
    public @NonNull TagProcessor<ListTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "li@a|b|c";
    }
}
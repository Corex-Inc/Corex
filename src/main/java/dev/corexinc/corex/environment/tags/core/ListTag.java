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

/* @doc object
 *
 * @Name ListTag
 * @Prefix li
 * @Format
 * The identity format for ListTags is each item, one after the other, in order, separated by a pipe '|' symbol.
 * For example, for a list of 'taco', 'potatoes', and 'cheese', it would be 'li@taco|potatoes|cheese|'
 * A list with zero items in it is simply 'li@',
 * and a list with one item is just the one item and a pipe on the end.
 *
 * @Description
 * A ListTag is a list of any data. It can hold any number of objects in any order.
 * The objects can be of any Corex object type, including another list.
 *
 * List indices start at 1 (so, the tag 'get[1]' gets the very first entry)
 * and extend to however many entries the list has (so, if a list has 15 entries, the tag 'get[15]' gets the very last entry).
 *
 * Inputs that accept list indices will generally accept 'first' to mean '1', 'last' to mean the last entry in the list,
 * or negative numbers to automatically select an index starting at the end - so for example 'get[-1]' gets the last entry, 'get[-2]' gets the second-to-last, etc.
 */
public class ListTag implements AbstractTag {

    private static final String prefix = "li";
    private final List<AbstractTag> list = new ArrayList<>();

    public static final TagProcessor<ListTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("list", attr -> new ListTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, ListTag::new);

        /* @doc tag
         *
         * @Name size
         * @RawName <ListTag.size>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns the size of the list.
         * @Usage
         * // Narrates "3"
         * - narrate <list[one|two|three].size>
         *
         * @Implements ListTag.size
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.list.size()));

        /* @doc tag
         *
         * @Name join[]
         * @RawName <ListTag.separated_by[<text>]>
         * @Object ListTag
         * @ReturnType ElementTag
         * @Description
         * Returns the list formatted, with each item separated by the defined text.
         * @Usege
         * // Narrates "bob and joe and john"
         * - narrate "<list[bob|joe|john].separated_by[ and ]>"
         *
         * @Implements ListTag.separated_by[<element>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "join", (attr, obj) -> {
            List<String> strings = new ArrayList<>();
            for (AbstractTag t : obj.list) strings.add(t.identify());
            return new ElementTag(String.join(attr.getParam(), strings));
        }).test(", ");

        /* @doc tag
         *
         * @Name get[]
         * @RawName <ListTag.get[<number>|...]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Returns an element of the value specified by the supplied context.
         * Specify more than one index to get a list of results.
         * Note the index input options described at {@link object ListTag}
         * @Usage
         * // Narrates "one"
         * - narrate <list[one|two|three].get[1]>

         * @Usage
         * // Narrates a list of "one|three"
         * - narrate <list[one|two|three].get[1|3]>
         *
         * @Implements ListTag.get[<#>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int index = new ElementTag(attr.getParam()).asInt() - 1;
            if (index >= 0 && index < obj.list.size()) {
                return obj.list.get(index);
            }
            return null;
        }).test("3");

        /* @doc tag
         *
         * @Name random[]
         * @OptionalArg true
         * @RawName <ListTag.random[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Gets a random item in the list and returns it.
         * Optionally, add [<#>] to instead get a list of multiple randomly chosen list entries.
         * Unlike Denizen, you're better off not using .random[9999]. Use {@link tag ListTag.shuffled} instead.
         *
         * @Usage
         * // Narrates EITHER "one" OR "two" - different each time!
         * - narrate "<list[one|two].random>"
         *
         * @Usage
         * // Could narrate "one|two", "two|three", OR "one|three" - different each time!
         * - narrate "<list[one|two|three].random[2]>"
         *
         * @Implements ListTag.random[(<#>)]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "random", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;

            if (!attr.hasParam()) {
                int index = ThreadLocalRandom.current().nextInt(obj.list.size());
                return obj.list.get(index);
            }

            int count = new ElementTag(attr.getParam()).asInt();
            if (count <= 0) return new ListTag("");

            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);

            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag("");
            for (AbstractTag t : copy.subList(0, limit)) result.addObject(t);

            return result;
        }).test("2");

        /* @doc tag
         *
         * @Name shuffled
         * @RawName <ListTag.shuffled>
         * @Object ListTag
         * @ReturnType ListTag
         * @Description
         * Returns a new list with the items in a randomized order.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "shuffled", (attr, obj) -> {
            if (obj.list.isEmpty()) return new ListTag("");

            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);

            ListTag result = new ListTag("");
            for (AbstractTag t : copy) result.addObject(t);

            return result;
        });
    }

    public ListTag(String raw) {
        if (raw == null || raw.isEmpty()) return;

        List<String> split = ObjectFetcher.splitIgnoringBrackets(raw, '|');
        for (String s : split) {
            this.list.add(ObjectFetcher.pickObject(s));
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
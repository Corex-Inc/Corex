package dev.corexinc.corex.environment.tags.core;

import com.google.gson.JsonElement;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.utils.scripts.JsonHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/* @doc object
 *
 * @Name ListTag
 * @Prefix li
 * @Format
 * The identity format for ListTags is each item, one after the other, in order, separated by a pipe '|' symbol.
 * For example, for a list of 'taco', 'potatoes', and 'cheese', it would be 'li@taco|potatoes|cheese'
 * A list with zero items in it is simply 'li@'.
 *
 * @Description
 * A ListTag is a list of any data. It can hold any number of objects in any order.
 * The objects can be of any Corex object type, including another list.
 *
 * List indices start at 1 (so, the tag 'get[1]' gets the very first entry)
 * and extend to however many entries the list has (so, if a list has 15 entries, 'get[15]' gets the very last entry).
 *
 * Inputs that accept list indices will generally accept:
 * - 'first' to mean index 1
 * - 'last'  to mean the final entry in the list
 * - Negative numbers to select from the end - 'get[-1]' is the last entry, 'get[-2]' is the second-to-last, etc.
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
         * @NoArg
         * @Description
         * Returns how many items are in the list.
         *
         * @Usage
         * // Narrates "3"
         * - narrate <list[one|two|three].size>
         *
         * @Implements ListTag.size
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) ->
                new ElementTag(obj.list.size()));

        /* @doc tag
         *
         * @Name isEmpty
         * @RawName <ListTag.isEmpty>
         * @Object ListTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the list has no entries.
         *
         * @Usage
         * // Narrates "true"
         * - narrate <list[].isEmpty>
         *
         * @Implements ListTag.is_empty
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isEmpty", (attr, obj) ->
                new ElementTag(obj.list.isEmpty()));

        /* @doc tag
         *
         * @Name get[]
         * @RawName <ListTag.get[<index>|...]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @ArgRequired
         * @Description
         * Returns the item at the given 1-based index.
         * Supply multiple pipe-separated indices to receive a ListTag of results.
         * Append {@code .to[<index>]} to receive a contiguous range.
         * Accepts 'first', 'last', and negative indices (see object description).
         *
         * @Usage
         * // Narrates "one"
         * - narrate <list[one|two|three].get[1]>
         *
         * @Usage
         * // Narrates "one|three"
         * - narrate <list[one|two|three].get[1|3]>
         *
         * @Usage
         * // Narrates "two|three"
         * - narrate <list[one|two|three|four].get[2].to[3]>
         *
         * @Implements ListTag.get[<#>|...], ListTag.get[<#>].to[<#>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (attr.matchesNext("to") && attr.hasNextParam()) {
                int from = resolveIndex(attr.getParam(), obj.list.size());
                int to   = resolveIndex(attr.getNextParam(), obj.list.size());
                attr.fulfill(1);
                if (from < 0 || to < 0 || from > to) return new ListTag();
                ListTag result = new ListTag();
                for (int index = from; index <= to; index++) result.addObject(obj.list.get(index));
                return result;
            }
            ListTag indices = new ListTag(attr.getParam());
            if (indices.size() == 1) {
                int index = resolveIndex(attr.getParam(), obj.list.size());
                return index >= 0 ? obj.list.get(index) : null;
            }
            ListTag result = new ListTag();
            for (AbstractTag indexTag : indices.getList()) {
                int index = resolveIndex(indexTag.identify(), obj.list.size());
                if (index >= 0) result.addObject(obj.list.get(index));
            }
            return result;
        }).test("2");

        /* @doc tag
         *
         * @Name first[]
         * @RawName <ListTag.first[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Returns the first element, equivalent to get[1].
         * Optionally specify a count to receive the first N elements as a ListTag.
         * Returns null for an empty list.
         *
         * @Usage
         * // Narrates "one"
         * - narrate <list[one|two|three].first>
         *
         * @Usage
         * // Narrates "one|two"
         * - narrate <list[one|two|three].first[2]>
         *
         * @Implements ListTag.first[(<#>)]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "first", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            if (!attr.hasParam()) return obj.list.getFirst();
            int count = Math.min(new ElementTag(attr.getParam()).asInt(), obj.list.size());
            if (count <= 0) return new ListTag();
            ListTag result = new ListTag();
            for (int index = 0; index < count; index++) result.addObject(obj.list.get(index));
            return result;
        });

        /* @doc tag
         *
         * @Name last[]
         * @RawName <ListTag.last[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Returns the last element, equivalent to get[-1].
         * Optionally specify a count to receive the last N elements in original order.
         * Returns null for an empty list.
         *
         * @Usage
         * // Narrates "three"
         * - narrate <list[one|two|three].last>
         *
         * @Usage
         * // Narrates "two|three"
         * - narrate <list[one|two|three].last[2]>
         *
         * @Implements ListTag.last[(<#>)]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "last", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            int size = obj.list.size();
            if (!attr.hasParam()) return obj.list.get(size - 1);
            int count = Math.min(new ElementTag(attr.getParam()).asInt(), size);
            if (count <= 0) return new ListTag();
            ListTag result = new ListTag();
            for (int index = size - count; index < size; index++) result.addObject(obj.list.get(index));
            return result;
        });

        /* @doc tag
         *
         * @Name contains[]
         * @RawName <ListTag.contains[<object>|...]>
         * @Object ListTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Returns whether the list contains ALL the given elements.
         *
         * @Usage
         * // Narrates "true"
         * - narrate <list[one|two|three].contains[two]>
         *
         * @Implements ListTag.contains[<element>|...]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "contains", (attr, obj) -> {
            if (!attr.hasParam()) return null;

            /* @doc tag
             *
             * @Name contains[].any
             * @RawName <ListTag.contains[<element>|...].any>
             * @Object ListTag
             * @ReturnType ElementTag(Boolean)
             * @ArgRequired 1
             * @Description
             * Returns true if at least ONE of the elements is present.
             * @Usage
             * // Narrates "true" - at least one of these is present
             * - narrate <list[one|two|three].contains[two|four].any>
             *
             * @Implements ListTag.contains_any[<element>|...]
             */
            boolean matchAny = attr.matchesNext("any");
            if (matchAny) attr.fulfill(1);
            List<String> identities = obj.list.stream().map(AbstractTag::identify).toList();
            for (AbstractTag needle : new ListTag(attr.getParam()).getList()) {
                boolean found = identities.contains(needle.identify());
                if (matchAny  &&  found) return new ElementTag(true);
                if (!matchAny && !found) return new ElementTag(false);
            }
            return new ElementTag(!matchAny);
        }).test("b");

        /* @doc tag
         *
         * @Name find[]
         * @RawName <ListTag.find[<object>]>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @ArgRequired
         * @Description
         * Returns the 1-based index of the first exact match in the list, or -1 if not found.
         *
         * @Usage
         * // Narrates "2"
         * - narrate <list[one|two|three].find[two]>
         *
         * @Implements ListTag.find[<element>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "find", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String needle = attr.getParam().toLowerCase();
            boolean returnAll = false, partial = false;

            /* @doc tag
             *
             * @Name find[].all
             * @RawName <ListTag.find[<element>].all>
             * @Object ListTag
             * @ReturnType ListTag(Number)
             * @ArgRequired 1
             * @Description
             * Returns the ListTag of 1-based index of ALL matching indices, or empty list if not found.
             *
             * @Usage
             * // Narrates "2|4"
             * - narrate <list[one|two|three|two].find[two].all>
             *
             * @Implements ListTag.find_all[<element>]
             */
            if (attr.matchesNext("all")) {
                returnAll = true;
                attr.fulfill(1);
            }

            /* @doc tag
             *
             * @Name find[].partial
             * @RawName <ListTag.find[<element>].partial>
             * @Object ListTag
             * @ReturnType ElementTag(Number)
             * @ArgRequired 1
             * @Description
             * Returns the 1-based index of match any element that CONTAINS the given text (not requiring an exact match), or -1 if not found.
             *
             * @Usage
             * // Narrates "2"
             * - narrate <list[one|two|three].find[tw].partial>
             *
             * @Implements ListTag.find_partial[<element>]
             */
            if (attr.matchesNext("partial")) {
                partial = true;
                attr.fulfill(1);
            }

            /* @doc tag
             *
             * @Name find[].partialAll
             * @RawName <ListTag.find[<element>].partialAll>
             * @Object ListTag
             * @ReturnType ListTag(Number)
             * @ArgRequired 1
             * @Description
             * Returns the ListTag of 1-based index of match any element that CONTAINS the given text (not requiring an exact match), or empty list if not found.
             *
             * @Usage
             * // Narrates "2|4"
             * - narrate <list[one|two|three|twenty].find[tw].partialAll>
             *
             * @Implements ListTag.find_all_partial[<element>]
             */
            if (attr.matchesNext("partialAll")) {
                returnAll = true;
                partial = true;
                attr.fulfill(1);
            }

            if (returnAll) {
                ListTag result = new ListTag();
                for (int index = 0; index < obj.list.size(); index++) {
                    String value = obj.list.get(index).identify().toLowerCase();
                    if (partial ? value.contains(needle) : value.equals(needle)) result.addString(String.valueOf(index + 1));
                }
                return result;
            }

            for (int index = 0; index < obj.list.size(); index++) {
                String value = obj.list.get(index).identify().toLowerCase();
                if (partial ? value.contains(needle) : value.equals(needle)) return new ElementTag(index + 1);
            }
            return new ElementTag(-1);
        }).test("b");

        /* @doc tag
         *
         * @Name count[]
         * @RawName <ListTag.count[<object>]>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @ArgRequired
         * @Description
         * Returns how many times the given value appears in the list.
         *
         * @Usage
         * // Narrates "2"
         * - narrate <list[one|two|two|three].count[two]>
         *
         * @Implements ListTag.count[<element>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "count", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String needle = attr.getParam();
            long matches = obj.list.stream().filter(tag -> tag.identify().equals(needle)).count();
            return new ElementTag((int) matches);
        }).test("1");

        /* @doc tag
         *
         * @Name join[]
         * @RawName <ListTag.join[(<text>)]>
         * @Object ListTag
         * @ReturnType ElementTag
         * @Description
         * Returns the list as a single string with items separated by the given text.
         * Defaults to ", " when no separator is provided.
         *
         * @Usage
         * // Narrates "one, two, three"
         * - narrate <list[one|two|three].join>
         *
         * @Usage
         * // Narrates "one and two and three"
         * - narrate <list[one|two|three].join[ and ]>
         *
         * @Implements ListTag.separated_by[<element>], ListTag.comma_separated
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "join", (attr, obj) -> {
            String separator = attr.hasParam() ? attr.getParam() : "";
            List<String> strings = new ArrayList<>();
            for (AbstractTag tag : obj.list) strings.add(tag.identify());
            return new ElementTag(String.join(separator, strings));
        }).test(", ");

        /* @doc tag
         *
         * @Name include[]
         * @RawName <ListTag.include[...|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with the specified items appended to the end.
         *
         * @Usage
         * // Narrates "one|two|three|four"
         * - narrate <list[one|two].include[three|four]>
         *
         * @Implements ListTag.include[...|...]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "include", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) result.addObject(tag);
            for (AbstractTag tag : new ListTag(attr.getParam()).getList()) result.addObject(tag);
            return result;
        }).test("d|e");

        /* @doc tag
         *
         * @Name exclude[]
         * @RawName <ListTag.exclude[...|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with all matching items removed.
         *
         * @Usage
         * // Narrates "one|three"
         * - narrate <list[one|two|three|two].exclude[two]>
         *
         * @Implements ListTag.exclude[...|...]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "exclude", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int maxRemovals = Integer.MAX_VALUE;

            /* @doc tag
             *
             * @Name exclude[].max[]
             * @RawName <ListTag.exclude[...|...].max[<#>]>
             * @Object ListTag
             * @ReturnType ListTag
             * @ArgRequired
             * @Description
             * Returns a new list with all matching items removed with limit how many occurrences are removed.
             *
             * @Usage
             * // Narrates "taco|taco|potato" - only removes two 'potato' entries
             * - narrate <list[taco|potato|taco|potato|potato].exclude[potato].max[2]>
             *
             * @Implements ListTag.exclude[...|...].max[<#>]
             */
            if (attr.matchesNext("max") && attr.hasNextParam()) {
                maxRemovals = new ElementTag(attr.getNextParam()).asInt();
                attr.fulfill(1);
            }

            Set<String> excluded = new ListTag(attr.getParam()).getList().stream()
                    .map(AbstractTag::identify).collect(Collectors.toSet());
            Map<String, Integer> removalCount = new HashMap<>();
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) {
                String id = tag.identify();
                if (excluded.contains(id) && removalCount.getOrDefault(id, 0) < maxRemovals) {
                    removalCount.merge(id, 1, Integer::sum);
                } else {
                    result.addObject(tag);
                }
            }
            return result;
        }).test("b");

        /* @doc tag
         *
         * @Name insert[].at[]
         * @RawName <ListTag.insert[...|...].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with the given items inserted before the item at the specified index.
         * Items from that index onward are shifted right.
         * An out-of-range index appends to the end.
         *
         * @Usage
         * // Narrates "one|two|three|four"
         * - narrate <list[one|four].insert[two|three].at[2]>
         *
         * @Implements ListTag.insert[...|...].at[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "insert", (attr, obj) -> {
            if (!attr.hasParam() || !attr.matchesNext("at") || !attr.hasNextParam()) return null;
            List<AbstractTag> toInsert = new ListTag(attr.getParam()).getList();
            int insertAt = resolveIndex(attr.getNextParam(), obj.list.size() + 1);
            attr.fulfill(1);
            if (insertAt < 0) insertAt = obj.list.size();
            ListTag result = new ListTag();
            for (int index = 0; index < obj.list.size(); index++) {
                if (index == insertAt) for (AbstractTag tag : toInsert) result.addObject(tag);
                result.addObject(obj.list.get(index));
            }
            if (insertAt >= obj.list.size()) for (AbstractTag tag : toInsert) result.addObject(tag);
            return result;
        }).test("x", "at[2]");

        /* @doc tag
         *
         * @Name set[].at[]
         * @RawName <ListTag.set[...|...].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with the item at the given index replaced by the provided value(s).
         * If more than one replacement value is given, extras are inserted at that position and shift following items right.
         *
         * @Usage
         * // Narrates "one|potato|three"
         * - narrate <list[one|two|three].set[potato].at[2]>
         *
         * @Usage
         * // Narrates "one|potato|taco|three"
         * - narrate <list[one|two|three].set[potato|taco].at[2]>
         *
         * @Implements ListTag.set[...|...].at[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "set", (attr, obj) -> {
            if (!attr.hasParam() || !attr.matchesNext("at") || !attr.hasNextParam()) return null;
            List<AbstractTag> replacements = new ListTag(attr.getParam()).getList();
            int target = resolveIndex(attr.getNextParam(), obj.list.size());
            attr.fulfill(1);
            if (target < 0) return null;
            ListTag result = new ListTag();
            for (int index = 0; index < obj.list.size(); index++) {
                if (index == target) for (AbstractTag tag : replacements) result.addObject(tag);
                else result.addObject(obj.list.get(index));
            }
            return result;
        }).test("x", "at[2]");

        /* @doc tag
         *
         * @Name overwrite[].at[]
         * @RawName <ListTag.overwrite[...|...].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with entries starting at the given index overwritten by the provided values.
         * If the replacement extends past the end of the original list, the list grows to accommodate.
         *
         * @Usage
         * // Narrates "one|potato|taco|four"
         * - narrate <list[one|two|three|four].overwrite[potato|taco].at[2]>
         *
         * @Implements ListTag.overwrite[...|...].at[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "overwrite", (attr, obj) -> {
            if (!attr.hasParam() || !attr.matchesNext("at") || !attr.hasNextParam()) return null;
            List<AbstractTag> replacements = new ListTag(attr.getParam()).getList();
            int target = resolveIndex(attr.getNextParam(), obj.list.size());
            attr.fulfill(1);
            if (target < 0) return null;
            List<AbstractTag> copy = new ArrayList<>(obj.list);
            for (int offset = 0; offset < replacements.size(); offset++) {
                int position = target + offset;
                if (position < copy.size()) copy.set(position, replacements.get(offset));
                else copy.add(replacements.get(offset));
            }
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        }).test("x|y", "at[2]");

        /* @doc tag
         *
         * @Name remove[]
         * @RawName <ListTag.remove[<#>|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with the item(s) at the specified index (or indices) removed.
         * Append {@code .to[<index>]} to remove a contiguous range.
         * Accepts 'first', 'last', and negative indices.
         *
         * @Usage
         * // Narrates "one|three|four"
         * - narrate <list[one|two|three|four].remove[2]>
         *
         * @Usage
         * // Narrates "one|five"
         * - narrate <list[one|two|three|four|five].remove[2].to[4]>
         *
         * @Implements ListTag.remove[<#>|...]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "remove", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Set<Integer> toRemove = new HashSet<>();

            /* @doc tag
             *
             * @Name remove[].to[]
             * @RawName <ListTag.remove[<#>|...].to[<#>]>
             * @Object ListTag
             * @ReturnType ListTag
             * @ArgRequired
             * @Description
             * Returns a new list with the item(s) at the specified index contiguous range removed.
             * Accepts 'first', 'last', and negative indices.
             *
             * @Usage
             * // Narrates "one|five"
             * - narrate <list[one|two|three|four|five].remove[2].to[4]>
             *
             * @Implements ListTag.remove[<#>].to[<#>]
             */
            if (attr.matchesNext("to") && attr.hasNextParam()) {
                int from = resolveIndex(attr.getParam(), obj.list.size());
                int to   = resolveIndex(attr.getNextParam(), obj.list.size());
                attr.fulfill(1);
                if (from >= 0 && to >= from) for (int index = from; index <= to; index++) toRemove.add(index);
            } else {
                for (AbstractTag indexTag : new ListTag(attr.getParam()).getList()) {
                    int index = resolveIndex(indexTag.identify(), obj.list.size());
                    if (index >= 0) toRemove.add(index);
                }
            }
            ListTag result = new ListTag();
            for (int index = 0; index < obj.list.size(); index++) {
                if (!toRemove.contains(index)) result.addObject(obj.list.get(index));
            }
            return result;
        }).test("2");

        /* @doc tag
         *
         * @Name replace[].with[]
         * @RawName <ListTag.replace[<object>].with[<object>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with all occurrences of the given element replaced matches with a different value.
         *
         * @Usage
         * // Narrates "one|three"
         * - narrate <list[one|two|three].replace[two]>
         *
         * @Usage
         * // Narrates "one|potato|three"
         * - narrate <list[one|two|three].replace[two].with[potato]>
         *
         * @Implements ListTag.replace[<element>].with[<element>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "replace", (attr, obj) -> {
            if (!attr.hasParam() || !attr.matchesNext("with") || !attr.hasNextParam()) return null;
            String target = attr.getParam();
            AbstractTag replacement = ObjectFetcher.pickObject(attr.getNextParam());
            attr.fulfill(1);
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) {
                if (tag.identify().equals(target)) {
                    result.addObject(replacement);
                } else {
                    result.addObject(tag);
                }
            }
            return result;
        }).test("b", "with[lol]");

        /* @doc tag
         *
         * @Name deduplicate
         * @RawName <ListTag.deduplicate>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a copy of the list with all duplicate entries removed, keeping only the first occurrence.
         *
         * @Usage
         * // Narrates "one|two|three"
         * - narrate <list[one|one|two|three].deduplicate>
         *
         * @Implements ListTag.deduplicate
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "deduplicate", (attr, obj) -> {
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) {
                if (seen.add(tag.identify())) result.addObject(tag);
            }
            return result;
        });

        /* @doc tag
         *
         * @Name sharedContents[]
         * @RawName <ListTag.sharedContents[<element>|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a list of items that appear in BOTH this list and the given list.
         * Preserves the order of the original list and deduplicates automatically.
         *
         * @Usage
         * // Narrates "two|four"
         * - narrate <list[one|two|three|four].sharedContents[two|four|five]>
         *
         * @Implements ListTag.shared_contents[...|...]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "sharedContents", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Set<String> other = new ListTag(attr.getParam()).getList().stream()
                    .map(AbstractTag::identify).collect(Collectors.toSet());
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) {
                String id = tag.identify();
                if (other.contains(id) && seen.add(id)) result.addObject(tag);
            }
            return result;
        }).test("b|c");

        /* @doc tag
         *
         * @Name padLeft[]
         * @RawName <ListTag.padLeft[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns the list extended to the specified minimum length by prepending entries.
         *
         * @Usage
         * // Narrates "|one|two" (padded to 3 entries with empty strings)
         * - narrate <list[one|two].padLeft[3]>
         *
         * @Usage
         * // Narrates "0|one|two"
         * - narrate <list[one|two].padLeft[3].with[0]>
         *
         * @Implements ListTag.pad_left[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "padLeft", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int targetSize = new ElementTag(attr.getParam()).asInt();
            String fill = "";

            /* @doc tag
             *
             * @Name padLeft[].with[]
             * @RawName <ListTag.padLeft[<#>].with[<#>]>
             * @Object ListTag
             * @ReturnType ListTag
             * @ArgRequired
             * @Description
             * Returns the list extended to the specified minimum length by prepending entries uses a custom fill value instead of an empty string.
             *
             * @Usage
             * // Narrates "0|one|two"
             * - narrate <list[one|two].padLeft[3].with[0]>
             *
             * @Implements ListTag.pad_left[<#>].with[<element>]
             */
            if (attr.matchesNext("with") && attr.hasNextParam()) { fill = attr.getNextParam(); attr.fulfill(1); }
            ListTag result = new ListTag();
            for (int padding = obj.list.size(); padding < targetSize; padding++) result.addString(fill);
            for (AbstractTag tag : obj.list) result.addObject(tag);
            return result;
        }).test("5");

        /* @doc tag
         *
         * @Name padRight[]
         * @RawName <ListTag.padRight[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns the list extended to the specified minimum length by appending entries.
         *
         * @Usage
         * // Narrates "one|two|" (padded to 3 entries with empty strings)
         * - narrate <list[one|two].padRight[3]>
         *
         * @Implements ListTag.pad_right[<#>], ListTag.pad_right[<#>].with[<element>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "padRight", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int targetSize = new ElementTag(attr.getParam()).asInt();
            String fill = "";

            /* @doc tag
             *
             * @Name padRight[]
             * @RawName <ListTag.padRight[<#>].with[<#>]>
             * @Object ListTag
             * @ReturnType ListTag
             * @ArgRequired
             * @Description
             * Returns the list extended to the specified minimum length by appending entries uses a custom fill value instead of an empty string.
             *
             * @Usage
             * // Narrates "one|two|0"
             * - narrate <list[one|two].padRight[3].with[0]>
             *
             * @Implements ListTag.pad_right[<#>], ListTag.pad_right[<#>].with[<element>]
             */
            if (attr.matchesNext("with") && attr.hasNextParam()) { fill = attr.getNextParam(); attr.fulfill(1); }
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) result.addObject(tag);
            for (int padding = obj.list.size(); padding < targetSize; padding++) result.addString(fill);
            return result;
        }).test("5");

        /* @doc tag
         *
         * @Name reverse
         * @RawName <ListTag.reverse>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a copy of the list with all items in reversed order.
         *
         * @Usage
         * // Narrates "three|two|one"
         * - narrate <list[one|two|three].reverse>
         *
         * @Implements ListTag.reverse
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "reverse", (attr, obj) -> {
            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.reverse(copy);
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        });

        /* @doc tag
         *
         * @Name sort[]
         * @RawName <ListTag.sort[(<mode>)]>
         * @Object ListTag
         * @ReturnType ListTag
         * @Description
         * Returns a sorted copy of the list. Mode controls the sort strategy:
         * <ul>
         *   <li>alph (alphabetical) - case-insensitive lexicographic order.</li>
         *   <li>nat (natural) - mixed letter/number natural order (e.g. "a2" before "a10").</li>
         *   <li>num (numerical) - ascending numeric order; non-numbers sort as 0.</li>
         * </ul>
         *
         * @Usage
         * // Narrates "a|c|d|g|q"
         * - narrate <list[c|d|q|a|g].sort>
         *
         * @Usage
         * // Narrates "a1|a2|a10|b"
         * - narrate <list[b|a10|a2|a1].sort[natural]>
         *
         * @Usage
         * // Narrates "1|2|3|10"
         * - narrate <list[3|2|1|10].sort[numerical]>
         *
         * @Implements ListTag.alphabetical, ListTag.alphanumeric, ListTag.numerical
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "sort", (attr, obj) -> {
            String mode = attr.hasParam() ? attr.getParam().toLowerCase() : "alph";
            List<AbstractTag> copy = new ArrayList<>(obj.list);
            switch (mode) {
                case "num", "numerical" -> copy.sort(Comparator.comparingDouble(ListTag::numericValue));
                case "nat", "natural" -> copy.sort((a, b) -> naturalCompare(a.identify(), b.identify()));
                case "alph", "alphabetical" -> copy.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.identify(), b.identify()));
                default -> {
                    Debugger.echoError(attr.getQueue(), "Sorting type '<red>" + mode + "</red>' is unknown!");
                    return null;
                }
            }
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        });

        /* @doc tag
         *
         * @Name shuffled
         * @RawName <ListTag.shuffled>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a copy of the list in a random order.
         * Do NOT use .random[9999] for shuffle the list!
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "shuffled", (attr, obj) -> {
            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy);
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        });

        /* @doc tag
         *
         * @Name random[]
         * @RawName <ListTag.random[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Returns a randomly chosen item from the list.
         * Optionally specify a count to return that many distinct random items as a ListTag.
         * For shuffling the whole list, prefer {@link tag ListTag.shuffled}.
         *
         * @Usage
         * // Narrates either "one" or "two" - different each time
         * - narrate <list[one|two].random>
         *
         * @Usage
         * // Returns 2 distinct random items
         * - narrate <list[one|two|three].random[2]>
         *
         * @Implements ListTag.random[(<#>)]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "random", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            if (!attr.hasParam()) return obj.list.get(ThreadLocalRandom.current().nextInt(obj.list.size()));
            int count = new ElementTag(attr.getParam()).asInt();
            if (count <= 0) return new ListTag();
            List<AbstractTag> copy = new ArrayList<>(obj.list);
            Collections.shuffle(copy, ThreadLocalRandom.current());
            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (int index = 0; index < limit; index++) result.addObject(copy.get(index));
            return result;
        }).test("2");

        /* @doc tag
         *
         * @Name sum
         * @RawName <ListTag.sum>
         * @Object ListTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the sum of all numeric values in the list. Non-numeric entries are ignored.
         *
         * @Usage
         * // Narrates "6"
         * - narrate <list[1|2|3].sum>
         *
         * @Implements ListTag.sum
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "sum", (attr, obj) -> {
            double total = 0;
            for (AbstractTag tag : obj.list) {
                ElementTag element = new ElementTag(tag.identify());
                if (element.isDouble()) total += element.asDouble();
            }
            return new ElementTag(total);
        });

        /* @doc tag
         *
         * @Name product
         * @RawName <ListTag.product>
         * @Object ListTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the product of all numeric values in the list. Non-numeric entries are ignored.
         * Returns 1 if no numeric values are present.
         *
         * @Usage
         * // Narrates "24"
         * - narrate <list[2|3|4].product>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "product", (attr, obj) -> {
            double total = 1;
            for (AbstractTag tag : obj.list) {
                ElementTag element = new ElementTag(tag.identify());
                if (element.isDouble()) total *= element.asDouble();
            }
            return new ElementTag(total);
        });

        /* @doc tag
         *
         * @Name average
         * @RawName <ListTag.average>
         * @Object ListTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the mean average of all numeric values in the list. Non-numeric entries are ignored.
         * Returns 0 if no numeric values are present.
         *
         * @Usage
         * // Narrates "3"
         * - narrate <list[1|2|4|5].average>
         *
         * @Implements ListTag.average
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "average", (attr, obj) -> {
            double total = 0;
            int count = 0;
            for (AbstractTag tag : obj.list) {
                ElementTag element = new ElementTag(tag.identify());
                if (element.isDouble()) { total += element.asDouble(); count++; }
            }
            return new ElementTag(count == 0 ? 0 : total / count);
        });

        /* @doc tag
         *
         * @Name highest[]
         * @RawName <ListTag.highest[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Returns the item with the highest numeric value.
         * Optionally specify a count to return the top N items as a ListTag, in descending order.
         *
         * @Usage
         * // Narrates "10"
         * - narrate <list[3|2|1|10].highest>
         *
         * @Usage
         * // Narrates "10|3"
         * - narrate <list[3|2|1|10].highest[2]>
         *
         * @Implements ListTag.highest, ListTag.highest.count[<#>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "highest", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            int count = attr.hasParam() ? new ElementTag(attr.getParam()).asInt() : 1;

            List<AbstractTag> copy = new ArrayList<>(obj.list);
            copy.sort(Comparator.comparingDouble(ListTag::numericValue).reversed());

            if (count == 1) return copy.getFirst();
            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (int index = 0; index < limit; index++) result.addObject(copy.get(index));

            return result;
        });

        /* @doc tag
         *
         * @Name lowest[]
         * @RawName <ListTag.lowest[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Description
         * Returns the item with the lowest numeric value.
         * Optionally specify a count to return the N smallest items as a ListTag, in ascending order.
         *
         * @Usage
         * // Narrates "1"
         * - narrate <list[3|2|1|10].lowest>
         *
         * @Usage
         * // Narrates "1|2"
         * - narrate <list[3|2|1|10].lowest[2]>
         *
         * @Implements ListTag.lowest, ListTag.lowest.count[<#>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "lowest", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            int count = attr.hasParam() ? new ElementTag(attr.getParam()).asInt() : 1;

            List<AbstractTag> copy = new ArrayList<>(obj.list);
            copy.sort(Comparator.comparingDouble(ListTag::numericValue));

            if (count == 1) return copy.getFirst();
            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (int index = 0; index < limit; index++) result.addObject(copy.get(index));

            return result;
        });

        /* @doc tag
         *
         * @Name combine
         * @RawName <ListTag.combine>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Treats each item in this list as a sub-list and returns a single flat list of all their contents.
         *
         * @Usage
         * // foreach entry narrates "a", then "b", then "c", then "d"
         * - foreach <list[a|b|c|d].subLists[2].combine> as:entry:
         *     - narrate <[entry]>
         *
         * @Implements ListTag.combine
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "combine", (attr, obj) -> {
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) {
                for (AbstractTag subTag : new ListTag(tag.identify()).getList()) result.addObject(subTag);
            }
            return result;
        });

        /* @doc tag
         *
         * @Name subLists[]
         * @RawName <ListTag.subLists[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Splits this list into a ListTag of sub-lists, each of the specified maximum length.
         * The final sub-list may be shorter if the count doesn't divide evenly.
         *
         * @Usage
         * // foreach entry narrates "a|b", then "c|d", then "e|f"
         * - foreach <list[a|b|c|d|e|f].subLists[2]> as:sublist:
         *     - narrate <[sublist]>
         *
         * @Implements ListTag.sub_lists[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "subLists", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int chunkSize = new ElementTag(attr.getParam()).asInt();
            if (chunkSize <= 0) return new ListTag();
            ListTag result = new ListTag();
            for (int start = 0; start < obj.list.size(); start += chunkSize) {
                int end = Math.min(start + chunkSize, obj.list.size());
                ListTag chunk = new ListTag();
                for (int index = start; index < end; index++) chunk.addObject(obj.list.get(index));
                result.addString(chunk.identify());
            }
            return result;
        }).test("2");

        /* @doc tag
         *
         * @Name mapWith[]
         * @RawName <ListTag.mapWith[<value>|...]>
         * @Object ListTag
         * @ReturnType MapTag
         * @ArgRequired
         * @Description
         * Treats this list as keys and the parameter list as values,
         * pairing them by index to form a MapTag.
         * Extra entries on either side are ignored.
         *
         * @Usage
         * // Narrates "map@[a=1;b=2;c=3]"
         * - narrate <list[a|b|c].mapWith[1|2|3]>
         *
         * @Implements ListTag.map_with[<value>|...]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "mapWith", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            List<AbstractTag> values = new ListTag(attr.getParam()).getList();
            MapTag result = new MapTag();
            int length = Math.min(obj.list.size(), values.size());
            for (int index = 0; index < length; index++) {
                result.putObject(obj.list.get(index).identify(), values.get(index));
            }
            return result;
        }).test("1|2|3");

        /* @doc tag
         *
         * @Name toMap[]
         * @RawName <ListTag.toMap[(<separator>)]>
         * @Object ListTag
         * @ReturnType MapTag
         * @Description
         * Interprets each list entry as a "key/value" pair and builds a MapTag.
         * The separator defaults to '/' but can be customised.
         *
         * @Usage
         * // Narrates "map@[a=1;b=2]"
         * - narrate <list[a/1|b/2].toMap>
         *
         * @Usage
         * // Narrates "map@[name=bob;role=admin]"
         * - narrate <list[name:bob|role:admin].toMap[:]>
         *
         * @Implements ListTag.to_map[(<separator>)]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "toMap", (attr, obj) -> {
            String separator = attr.hasParam() ? attr.getParam() : "/";
            MapTag result = new MapTag();
            for (AbstractTag tag : obj.list) {
                String entry = tag.identify();
                int splitAt = entry.indexOf(separator);
                if (splitAt >= 0) result.putObject(
                        entry.substring(0, splitAt),
                        ObjectFetcher.pickObject(entry.substring(splitAt + separator.length()))
                );
            }
            return result;
        });

        /* @doc tag
         *
         * @Name toJson
         * @RawName <ListTag.toJson>
         * @Object ListTag
         * @ReturnType ElementTag
         * @NoArg
         *
         * @Description
         * Converts the ListTag into a strict JSON string.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "toJson", (attr, obj) -> {
            JsonElement json = JsonHelper.toJson(obj);

            /* @doc tag
             *
             * @Name toJson.pretty
             * @RawName <ListTag.toJson.pretty>
             * @Object ListTag
             * @ReturnType ElementTag
             * @NoArg
             *
             * @Description
             * Converts the ListTag into an element with nicely formatted multiline JSON.
             */
            if (attr.matchesNext("pretty")) {
                attr.fulfill(1);
                return new ElementTag(JsonHelper.toPrettyString(json));
            }
            return new ElementTag(json.toString());
        });

        /* @doc tag
         *
         * @Name dotProduct[]
         * @RawName <ListTag.dotProduct[<list>]>
         * @Object ListTag
         * @ReturnType ElementTag
         * @ArgRequired
         *
         * @Description
         * Calculates the dot product between this list and the input list.
         * If lengths differ, the smaller size is used.
         * All values are treated as numbers.
         *
         * @Usage
         * // Narrates "32"
         * - narrate <list[1|2|3].dotProduct[4|5|6]>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "dotProduct", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            List<AbstractTag> other = new ListTag(attr.getParam()).getList();

            double sum = 0;
            int size = Math.min(obj.list.size(), other.size());

            for (int i = 0; i < size; i++) {
                double a = numericValue(obj.list.get(i));
                double b = numericValue(other.get(i));
                sum += a * b;
            }
            return new ElementTag(sum);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name activation[]
         * @RawName <ListTag.activation[(<type>)]>
         * @Object ListTag
         * @ReturnType ListTag
         *
         * @Description
         * Applies an activation function to all numeric values in the list.
         * Defaults to "relu" if no type is specified.
         * Supports "relu", "sigmoid", "tanh", and "softmax".
         * Softmax is applied to the whole list, other functions are applied element-wise.
         *
         * @Usage
         * // Narrates "li@0|0|3"
         * - narrate <list[-1|0|3].activation>
         *
         * @Usage
         * // Narrates softmax probabilities
         * - narrate <list[1|2|3].activation[softmax]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "activation", (attr, obj) -> {
            String type = attr.hasParam() ? attr.getParam().toLowerCase() : "relu";

            if (type.equals("softmax")) {
                double max = obj.getList().stream().mapToDouble(ListTag::numericValue).max().orElse(0);
                double sumExp = 0;
                List<Double> exps = new ArrayList<>();

                for (AbstractTag item : obj.getList()) {
                    double exp = Math.exp(numericValue(item) - max);
                    exps.add(exp);
                    sumExp += exp;
                }

                ListTag result = new ListTag();
                for (double exp : exps) result.addObject(new ElementTag(sumExp == 0 ? 0 : exp / sumExp));
                return result;
            }

            return applyActivationRecursively(obj, type);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name matrixMul[]
         * @RawName <ListTag.matrixMul[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         *
         * @Description
         * Multiplies this list (as a matrix of rows) by the input list (as a vector).
         * Each row is dot-multiplied with the input list, producing a result list.
         * If lengths differ, the smaller size is used. Non-list rows are ignored.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "matrixMul", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ListTag other = attr.getParamObject(ListTag.class, ListTag::new);
            if (other == null) return null;

            ListTag result = new ListTag();
            List<AbstractTag> otherItems = other.getList();

            for (AbstractTag rowTag : obj.getList()) {
                if (!(rowTag instanceof ListTag row)) continue;

                double sum = 0;
                List<AbstractTag> rowItems = row.getList();
                int size = Math.min(rowItems.size(), otherItems.size());

                for (int i = 0; i < size; i++) {
                    sum += numericValue(rowItems.get(i)) * numericValue(otherItems.get(i));
                }
                result.addObject(new ElementTag(sum));
            }

            return result;
        }).ignoreTest();
    }

    public ListTag() {}

    public ListTag(String raw) {
        if (raw == null || raw.isEmpty()) return;
        if (raw.startsWith(prefix + "@")) raw = raw.substring(prefix.length() + 1);
        for (String entry : ObjectFetcher.splitIgnoringBrackets(raw, '|')) {
            if (!entry.isEmpty()) this.list.add(ObjectFetcher.pickObject(entry));
        }
    }

    public ListTag(List<?> list) {
        for (Object element : list) {
            if (element == null) continue;
            if (element instanceof AbstractTag tag) {
                this.list.add(tag);
            } else {
                this.list.add(ObjectFetcher.pickObject(element.toString()));
            }
        }
    }

    /**
     * Returns all list entries that are an instance of {@code clazz}.
     * Entries that do not match log an error to {@code queue} (pass {@code null} to suppress).
     */
    public <T extends AbstractTag> List<T> filter(Class<T> clazz, @Nullable ScriptQueue queue) {
        List<T> results = new ArrayList<>();
        for (AbstractTag item : this.list) {
            if (clazz.isInstance(item)) {
                results.add(clazz.cast(item));
            } else if (queue != null) {
                Debugger.echoError(queue, "Cannot process list-entry '" + item.identify()
                        + "' as type '" + clazz.getSimpleName() + "' (does not match expected type).");
            }
        }
        return results;
    }

    @SafeVarargs
    public final List<AbstractTag> filter(@Nullable ScriptQueue queue, Class<? extends AbstractTag>... classes) {
        String typeLabel = Arrays.stream(classes)
                .map(Class::getSimpleName)
                .collect(Collectors.joining("/"));

        List<AbstractTag> results = new ArrayList<>(this.list.size());
        for (AbstractTag item : this.list) {
            boolean matched = false;
            for (Class<?> clazz : classes) {
                if (clazz.isInstance(item)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                results.add(item);
            } else if (queue != null) {
                Debugger.echoError(queue, "Cannot process list-entry '" + item.identify()
                        + "' as type '" + typeLabel + "' (does not match expected type).");
            }
        }
        return results;
    }

    private static ListTag applyActivationRecursively(ListTag source, String type) {
        ListTag result = new ListTag();
        for (AbstractTag item : source.getList()) {
            if (item instanceof ListTag subList) {
                result.addObject(applyActivationRecursively(subList, type));
                continue;
            }

            double val = numericValue(item);
            double calc = switch (type) {
                case "relu" -> Math.max(0, val);
                case "sigmoid" -> 1.0 / (1.0 + Math.exp(-val));
                case "tanh" -> Math.tanh(val);
                default -> val;
            };
            result.addObject(new ElementTag(calc));
        }
        return result;
    }

    public List<AbstractTag> getList() { return new ArrayList<>(list); }

    public int size() { return list.size(); }

    public String get(int index) {
        return (index >= 0 && index < list.size()) ? list.get(index).identify() : null;
    }

    public void addString(String value) {
        if (value != null) this.list.add(new ElementTag(value));
    }

    public void addObject(AbstractTag tag) {
        if (tag != null) this.list.add(tag);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    private static int resolveIndex(String param, int size) {
        if (size == 0) return -1;
        String normalized = param.trim().toLowerCase();
        if (normalized.equals("first")) return 0;
        if (normalized.equals("last"))  return size - 1;
        int parsed;
        try { parsed = Integer.parseInt(normalized); }
        catch (NumberFormatException ex) { return -1; }
        int index = parsed < 0 ? size + parsed : parsed - 1;
        return (index >= 0 && index < size) ? index : -1;
    }

    private static double numericValue(AbstractTag tag) {
        return tag instanceof ElementTag element ? element.asDouble() : new ElementTag(tag.identify()).asDouble();
    }

    private static int naturalCompare(String first, String second) {
        int posFirst = 0, posSecond = 0;
        while (posFirst < first.length() && posSecond < second.length()) {
            char charFirst = first.charAt(posFirst), charSecond = second.charAt(posSecond);
            if (Character.isDigit(charFirst) && Character.isDigit(charSecond)) {
                int startFirst = posFirst, startSecond = posSecond;
                while (posFirst  < first.length()  && Character.isDigit(first.charAt(posFirst)))   posFirst++;
                while (posSecond < second.length() && Character.isDigit(second.charAt(posSecond))) posSecond++;
                long numFirst  = Long.parseLong(first.substring(startFirst, posFirst));
                long numSecond = Long.parseLong(second.substring(startSecond, posSecond));
                if (numFirst != numSecond) return Long.compare(numFirst, numSecond);
            } else {
                int cmp = Character.compare(Character.toLowerCase(charFirst), Character.toLowerCase(charSecond));
                if (cmp != 0) return cmp;
                posFirst++;
                posSecond++;
            }
        }
        return Integer.compare(first.length() - posFirst, second.length() - posSecond);
    }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        List<String> strings = new ArrayList<>();
        for (AbstractTag tag : list) strings.add(tag.identify());
        return prefix + "@" + String.join("|", strings);
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return TAG_PROCESSOR.process(this, attribute); }

    @Override
    public @NonNull TagProcessor<ListTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NonNull String getTestValue() { return "li@a|b|c"; }
}
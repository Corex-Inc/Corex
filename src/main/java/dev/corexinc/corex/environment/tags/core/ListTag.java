package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

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
 * - Negative numbers to select from the end — 'get[-1]' is the last entry, 'get[-2]' is the second-to-last, etc.
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
         * @OptionalArg
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
         * @OptionalArg
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
         * @RawName <ListTag.contains[<element>|...]>
         * @Object ListTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Returns whether the list contains ALL the given elements.
         * Append {@code .any} to instead return true if at least ONE of the elements is present.
         *
         * @Usage
         * // Narrates "true"
         * - narrate <list[one|two|three].contains[two]>
         *
         * @Usage
         * // Narrates "false" — requires both two AND four
         * - narrate <list[one|two|three].contains[two|four]>
         *
         * @Usage
         * // Narrates "true" — at least one of these is present
         * - narrate <list[one|two|three].contains[two|four].any>
         *
         * @Implements ListTag.contains[<element>|...], ListTag.contains_any[<element>|...]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "contains", (attr, obj) -> {
            if (!attr.hasParam()) return new ElementTag(false);
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
         * @RawName <ListTag.find[<element>]>
         * @Object ListTag
         * @ReturnType ElementTag(Number), ListTag
         * @ArgRequired
         * @Description
         * Returns the 1-based index of the first exact match in the list, or -1 if not found.
         * Append {@code .all} to receive a ListTag of ALL matching indices instead.
         * Append {@code .partial} to match any element that CONTAINS the given text (not requiring an exact match).
         * {@code .partial} and {@code .all} can be combined in either order.
         *
         * @Usage
         * // Narrates "2"
         * - narrate <list[one|two|three].find[two]>
         *
         * @Usage
         * // Narrates "2|4"
         * - narrate <list[one|two|three|two].find[two].all>
         *
         * @Usage
         * // Narrates "2"
         * - narrate <list[one|two|three].find[tw].partial>
         *
         * @Usage
         * // Narrates "2|4"
         * - narrate <list[one|two|three|twenty].find[tw].partial.all>
         *
         * @Implements ListTag.find[<element>], ListTag.find_all[<element>], ListTag.find_partial[<element>], ListTag.find_all_partial[<element>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "find", (attr, obj) -> {
            if (!attr.hasParam()) return new ElementTag(-1);
            String needle = attr.getParam().toLowerCase();
            boolean returnAll = false, partial = false;
            if (attr.matchesNext("all"))     { returnAll = true; attr.fulfill(1); }
            if (attr.matchesNext("partial")) { partial   = true; attr.fulfill(1); }
            if (attr.matchesNext("all"))     { returnAll = true; attr.fulfill(1); } // handles .partial.all order
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
         * @RawName <ListTag.count[<element>]>
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
            if (!attr.hasParam()) return new ElementTag(0);
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
         * @OptionalArg
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
            String separator = attr.hasParam() ? attr.getParam() : ", ";
            List<String> strings = new ArrayList<>();
            for (AbstractTag tag : obj.list) strings.add(tag.identify());
            return new ElementTag(String.join(separator, strings));
        }).test(", ");

        /* @doc tag
         *
         * @Name include[]
         * @RawName <ListTag.include[<element>|...]>
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
            if (!attr.hasParam()) return obj;
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) result.addObject(tag);
            for (AbstractTag tag : new ListTag(attr.getParam()).getList()) result.addObject(tag);
            return result;
        }).test("d|e");

        /* @doc tag
         *
         * @Name exclude[]
         * @RawName <ListTag.exclude[<element>|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with all matching items removed.
         * Append {@code .max[<#>]} to limit how many occurrences are removed.
         *
         * @Usage
         * // Narrates "one|three"
         * - narrate <list[one|two|three|two].exclude[two]>
         *
         * @Usage
         * // Narrates "taco|taco|potato" — only removes two 'potato' entries
         * - narrate <list[taco|potato|taco|potato|potato].exclude[potato].max[2]>
         *
         * @Implements ListTag.exclude[...|...], ListTag.exclude[...|...].max[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "exclude", (attr, obj) -> {
            if (!attr.hasParam()) return obj;
            int maxRemovals = Integer.MAX_VALUE;
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
         * @RawName <ListTag.insert[<element>|...].at[<index>]>
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
         * @RawName <ListTag.set[<element>|...].at[<index>]>
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
            if (target < 0) return obj;
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
         * @RawName <ListTag.overwrite[<element>|...].at[<index>]>
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
            if (target < 0) return obj;
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
         * @RawName <ListTag.remove[<index>|...]>
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
         * @Implements ListTag.remove[<#>|...], ListTag.remove[<#>].to[<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "remove", (attr, obj) -> {
            if (!attr.hasParam()) return obj;
            Set<Integer> toRemove = new HashSet<>();
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
         * @Name replace[]
         * @RawName <ListTag.replace[<element>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Description
         * Returns a new list with all occurrences of the given element removed.
         * Append {@code .with[<element>]} to replace matches with a different value instead.
         *
         * @Usage
         * // Narrates "one|three"
         * - narrate <list[one|two|three].replace[two]>
         *
         * @Usage
         * // Narrates "one|potato|three"
         * - narrate <list[one|two|three].replace[two].with[potato]>
         *
         * @Implements ListTag.replace[<element>], ListTag.replace[<element>].with[<element>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "replace", (attr, obj) -> {
            if (!attr.hasParam()) return obj;
            String target = attr.getParam();
            String replacement = null;
            if (attr.matchesNext("with") && attr.hasNextParam()) {
                replacement = attr.getNextParam();
                attr.fulfill(1);
            }
            ListTag result = new ListTag();
            for (AbstractTag tag : obj.list) {
                if (tag.identify().equals(target)) {
                    if (replacement != null) result.addString(replacement);
                } else {
                    result.addObject(tag);
                }
            }
            return result;
        }).test("b");

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
            if (!attr.hasParam()) return new ListTag();
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
         * Append {@code .with[<element>]} to use a custom fill value instead of an empty string.
         *
         * @Usage
         * // Narrates "|one|two" (padded to 3 entries with empty strings)
         * - narrate <list[one|two].padLeft[3]>
         *
         * @Usage
         * // Narrates "0|one|two"
         * - narrate <list[one|two].padLeft[3].with[0]>
         *
         * @Implements ListTag.pad_left[<#>], ListTag.pad_left[<#>].with[<element>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "padLeft", (attr, obj) -> {
            if (!attr.hasParam()) return obj;
            int targetSize = new ElementTag(attr.getParam()).asInt();
            String fill = "";
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
         * Append {@code .with[<element>]} to use a custom fill value instead of an empty string.
         *
         * @Usage
         * // Narrates "one|two|" (padded to 3 entries with empty strings)
         * - narrate <list[one|two].padRight[3]>
         *
         * @Usage
         * // Narrates "one|two|0"
         * - narrate <list[one|two].padRight[3].with[0]>
         *
         * @Implements ListTag.pad_right[<#>], ListTag.pad_right[<#>].with[<element>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "padRight", (attr, obj) -> {
            if (!attr.hasParam()) return obj;
            int targetSize = new ElementTag(attr.getParam()).asInt();
            String fill = "";
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
         * @OptionalArg
         * @Description
         * Returns a sorted copy of the list. Mode controls the sort strategy:
         * <ul>
         *   <li>{@code alphabetical} (default) — case-insensitive lexicographic order.</li>
         *   <li>{@code natural} / {@code alphanumeric} — mixed letter/number natural order (e.g. "a2" before "a10").</li>
         *   <li>{@code numerical} — ascending numeric order; non-numbers sort as 0.</li>
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
            String mode = attr.hasParam() ? attr.getParam().toLowerCase() : "alphabetical";
            List<AbstractTag> copy = new ArrayList<>(obj.list);
            switch (mode) {
                case "numerical" -> copy.sort(Comparator.comparingDouble(ListTag::numericValue));
                case "natural", "alphanumeric" -> copy.sort((a, b) -> naturalCompare(a.identify(), b.identify()));
                default -> copy.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.identify(), b.identify()));
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
         * Prefer this over random[9999] when you need the entire list shuffled.
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
         * @OptionalArg
         * @Description
         * Returns a randomly chosen item from the list.
         * Optionally specify a count to return that many distinct random items as a ListTag.
         * For shuffling the whole list, prefer {@link tag ListTag.shuffled}.
         *
         * @Usage
         * // Narrates either "one" or "two" — different each time
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
         * @Name highest
         * @RawName <ListTag.highest>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @NoArg
         * @Description
         * Returns the item with the highest numeric value.
         * Append {@code .count[<#>]} to return the top N items as a ListTag, in descending order.
         *
         * @Usage
         * // Narrates "10"
         * - narrate <list[3|2|1|10].highest>
         *
         * @Usage
         * // Narrates "10|3"
         * - narrate <list[3|2|1|10].highest.count[2]>
         *
         * @Implements ListTag.highest, ListTag.highest.count[<#>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "highest", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            int count = 1;
            if (attr.matchesNext("count") && attr.hasNextParam()) {
                count = new ElementTag(attr.getNextParam()).asInt();
                attr.fulfill(1);
            }
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
         * @Name lowest
         * @RawName <ListTag.lowest>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @NoArg
         * @Description
         * Returns the item with the lowest numeric value.
         * Append {@code .count[<#>]} to return the N smallest items as a ListTag, in ascending order.
         *
         * @Usage
         * // Narrates "1"
         * - narrate <list[3|2|1|10].lowest>
         *
         * @Usage
         * // Narrates "1|2"
         * - narrate <list[3|2|1|10].lowest.count[2]>
         *
         * @Implements ListTag.lowest, ListTag.lowest.count[<#>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "lowest", (attr, obj) -> {
            if (obj.list.isEmpty()) return null;
            int count = 1;
            if (attr.matchesNext("count") && attr.hasNextParam()) {
                count = new ElementTag(attr.getNextParam()).asInt();
                attr.fulfill(1);
            }
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
            if (!attr.hasParam()) return obj;
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
         * @OptionalArg
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
            this.list.add(ObjectFetcher.pickObject(element.toString()));
        }
    }

    @SafeVarargs
    public final <T extends AbstractTag> List<T> filter(Class<? extends T>... classes) {
        List<T> results = new ArrayList<>();
        for (AbstractTag item : this.list) {
            for (Class<? extends T> clazz : classes) {
                if (clazz.isInstance(item)) { results.add(clazz.cast(item)); break; }
            }
        }
        return results;
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
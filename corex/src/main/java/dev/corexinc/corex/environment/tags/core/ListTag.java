package dev.corexinc.corex.environment.tags.core;

import com.google.gson.JsonElement;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.CorexComputePool;
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
 *
 * @Implements ListTag
 */
public class ListTag implements AbstractTag {

    private static final String prefix = "li";

    private volatile List<AbstractTag> boxedEntries;
    private volatile double[] numericEntries;
    private volatile boolean numericDerivationFailed;

    private static final double[] EMPTY_NUMBERS = new double[0];

    public static final TagProcessor<ListTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("list", attr -> {
            AbstractTag paramObj = attr.getParamObject();

            if (paramObj instanceof ComponentTag) {
                ListTag result = new ListTag();
                result.addObject(paramObj);
                return result;
            }

            return new ListTag(paramObj != null ? paramObj.identify() : null);
        });
        ObjectFetcher.registerFetcher(prefix, ListTag::new);

        /* @doc tag
         *
         * @Name size
         * @RawName <ListTag.size>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
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
                new ElementTag(obj.size())).setAsyncSafe();

        /* @doc tag
         *
         * @Name isEmpty
         * @RawName <ListTag.isEmpty>
         * @Object ListTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
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
                new ElementTag(obj.isEmpty())).setAsyncSafe();

        /* @doc tag
         *
         * @Name get[]
         * @RawName <ListTag.get[<index>|...]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @ArgRequired
         * @Async
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
                int size = obj.size();
                int from = resolveIndex(attr.getParam(), size);
                int to = resolveIndex(attr.getNextParam(), size);
                attr.fulfill(1);
                if (from < 0 || to < 0 || from > to) return new ListTag();

                double[] numbers = obj.numericView();
                if (numbers != null) {
                    double[] slice = new double[to - from + 1];
                    System.arraycopy(numbers, from, slice, 0, slice.length);
                    return ofNumbers(slice);
                }

                ListTag result = new ListTag();
                for (int index = from; index <= to; index++) result.addObject(obj.entryAt(index));
                return result;
            }
            ListTag indices = new ListTag(attr.getParam());
            if (indices.size() == 1) {
                int index = resolveIndex(attr.getParam(), obj.size());
                return index >= 0 ? obj.entryAt(index) : null;
            }
            ListTag result = new ListTag();
            for (AbstractTag indexTag : indices.getList()) {
                int index = resolveIndex(indexTag.identify(), obj.size());
                if (index >= 0) result.addObject(obj.entryAt(index));
            }
            return result;
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name first[]
         * @RawName <ListTag.first[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Async
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
            if (obj.isEmpty()) return null;
            if (!attr.hasParam()) return obj.entryAt(0);
            int count = Math.min(new ElementTag(attr.getParam()).asInt(), obj.size());
            if (count <= 0) return new ListTag();
            ListTag result = new ListTag();
            for (int index = 0; index < count; index++) result.addObject(obj.entryAt(index));
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name last[]
         * @RawName <ListTag.last[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Async
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
            if (obj.isEmpty()) return null;
            int size = obj.size();
            if (!attr.hasParam()) return obj.entryAt(size - 1);
            int count = Math.min(new ElementTag(attr.getParam()).asInt(), size);
            if (count <= 0) return new ListTag();
            ListTag result = new ListTag();
            for (int index = size - count; index < size; index++) result.addObject(obj.entryAt(index));
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name contains[]
         * @RawName <ListTag.contains[<object>|...]>
         * @Object ListTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Async
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
             * @Async
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
            List<String> identities = obj.items().stream().map(AbstractTag::identify).toList();
            for (AbstractTag needle : new ListTag(attr.getParam()).getList()) {
                boolean found = identities.contains(needle.identify());
                if (matchAny  &&  found) return new ElementTag(true);
                if (!matchAny && !found) return new ElementTag(false);
            }
            return new ElementTag(!matchAny);
        }).test("b").setAsyncSafe();

        /* @doc tag
         *
         * @Name find[]
         * @RawName <ListTag.find[<object>]>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @ArgRequired
         * @Async
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
             * @Async
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
             * @Async
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
             * @Async
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

            double[] numbers = partial ? null : obj.numericView();
            ElementTag needleTag = numbers != null ? new ElementTag(needle) : null;

            if (numbers != null && needleTag.isDouble()) {
                double target = needleTag.asDouble();

                if (returnAll) {
                    ListTag result = new ListTag();
                    for (int index = 0; index < numbers.length; index++) {
                        if (numbers[index] == target) result.addString(String.valueOf(index + 1));
                    }
                    return result;
                }

                for (int index = 0; index < numbers.length; index++) {
                    if (numbers[index] == target) return new ElementTag(index + 1);
                }
                return new ElementTag(-1);
            }

            if (returnAll) {
                ListTag result = new ListTag();
                for (int index = 0; index < obj.size(); index++) {
                    String value = obj.entryAt(index).identify().toLowerCase();
                    if (partial ? value.contains(needle) : value.equals(needle)) result.addString(String.valueOf(index + 1));
                }
                return result;
            }

            for (int index = 0; index < obj.size(); index++) {
                String value = obj.entryAt(index).identify().toLowerCase();
                if (partial ? value.contains(needle) : value.equals(needle)) return new ElementTag(index + 1);
            }
            return new ElementTag(-1);
        }).test("b").setAsyncSafe();

        /* @doc tag
         *
         * @Name count[]
         * @RawName <ListTag.count[<object>]>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @ArgRequired
         * @Async
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
            long matches = obj.items().stream().filter(tag -> tag.identify().equals(needle)).count();
            return new ElementTag((int) matches);
        }).test("1").setAsyncSafe();

        /* @doc tag
         *
         * @Name join[]
         * @RawName <ListTag.join[(<text>)]>
         * @Object ListTag
         * @ReturnType ElementTag
         * @Async
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
            for (AbstractTag tag : obj.items()) strings.add(tag.identify());
            return new ElementTag(String.join(separator, strings));
        }).test(", ").setAsyncSafe();

        /* @doc tag
         *
         * @Name include[]
         * @RawName <ListTag.include[...|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            for (AbstractTag tag : obj.items()) result.addObject(tag);
            for (AbstractTag tag : new ListTag(attr.getParam()).getList()) result.addObject(tag);
            return result;
        }).test("d|e").setAsyncSafe();

        /* @doc tag
         *
         * @Name exclude[]
         * @RawName <ListTag.exclude[...|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
             * @Async
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
            for (AbstractTag tag : obj.items()) {
                String id = tag.identify();
                if (excluded.contains(id) && removalCount.getOrDefault(id, 0) < maxRemovals) {
                    removalCount.merge(id, 1, Integer::sum);
                } else {
                    result.addObject(tag);
                }
            }
            return result;
        }).test("b").setAsyncSafe();

        /* @doc tag
         *
         * @Name insert[].at[]
         * @RawName <ListTag.insert[...|...].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            int insertAt = resolveIndex(attr.getNextParam(), obj.size() + 1);
            attr.fulfill(1);
            if (insertAt < 0) insertAt = obj.size();
            ListTag result = new ListTag();
            for (int index = 0; index < obj.size(); index++) {
                if (index == insertAt) for (AbstractTag tag : toInsert) result.addObject(tag);
                result.addObject(obj.entryAt(index));
            }
            if (insertAt >= obj.size()) for (AbstractTag tag : toInsert) result.addObject(tag);
            return result;
        }).test("x", "at[2]").setAsyncSafe();

        /* @doc tag
         *
         * @Name set[].at[]
         * @RawName <ListTag.set[...|...].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            int target = resolveIndex(attr.getNextParam(), obj.size());
            attr.fulfill(1);
            if (target < 0) return null;
            ListTag result = new ListTag();
            for (int index = 0; index < obj.size(); index++) {
                if (index == target) for (AbstractTag tag : replacements) result.addObject(tag);
                else result.addObject(obj.entryAt(index));
            }
            return result;
        }).test("x", "at[2]").setAsyncSafe();

        /* @doc tag
         *
         * @Name overwrite[].at[]
         * @RawName <ListTag.overwrite[...|...].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            int target = resolveIndex(attr.getNextParam(), obj.size());
            attr.fulfill(1);
            if (target < 0) return null;
            List<AbstractTag> copy = new ArrayList<>(obj.items());
            for (int offset = 0; offset < replacements.size(); offset++) {
                int position = target + offset;
                if (position < copy.size()) copy.set(position, replacements.get(offset));
                else copy.add(replacements.get(offset));
            }
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        }).test("x|y", "at[2]").setAsyncSafe();

        /* @doc tag
         *
         * @Name remove[]
         * @RawName <ListTag.remove[<#>|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
             * @Async
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
                int from = resolveIndex(attr.getParam(), obj.size());
                int to = resolveIndex(attr.getNextParam(), obj.size());
                attr.fulfill(1);
                if (from >= 0 && to >= from) for (int index = from; index <= to; index++) toRemove.add(index);
            } else {
                for (AbstractTag indexTag : new ListTag(attr.getParam()).getList()) {
                    int index = resolveIndex(indexTag.identify(), obj.size());
                    if (index >= 0) toRemove.add(index);
                }
            }
            ListTag result = new ListTag();
            for (int index = 0; index < obj.size(); index++) {
                if (!toRemove.contains(index)) result.addObject(obj.entryAt(index));
            }
            return result;
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name replace[].with[]
         * @RawName <ListTag.replace[<object>].with[<object>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                if (tag.identify().equals(target)) {
                    result.addObject(replacement);
                } else {
                    result.addObject(tag);
                }
            }
            return result;
        }).test("b", "with[lol]").setAsyncSafe();

        /* @doc tag
         *
         * @Name deduplicate
         * @RawName <ListTag.deduplicate>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                if (seen.add(tag.identify())) result.addObject(tag);
            }
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name sharedContents[]
         * @RawName <ListTag.sharedContents[<element>|...]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                String id = tag.identify();
                if (other.contains(id) && seen.add(id)) result.addObject(tag);
            }
            return result;
        }).test("b|c").setAsyncSafe();

        /* @doc tag
         *
         * @Name padLeft[]
         * @RawName <ListTag.padLeft[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
             * @Async
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
            for (int padding = obj.size(); padding < targetSize; padding++) result.addString(fill);
            for (AbstractTag tag : obj.items()) result.addObject(tag);
            return result;
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name padRight[]
         * @RawName <ListTag.padRight[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
             * @Async
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
            for (AbstractTag tag : obj.items()) result.addObject(tag);
            for (int padding = obj.size(); padding < targetSize; padding++) result.addString(fill);
            return result;
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name reverse
         * @RawName <ListTag.reverse>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
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
            List<AbstractTag> copy = new ArrayList<>(obj.items());
            Collections.reverse(copy);
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name sort[]
         * @RawName <ListTag.sort[(<mode>)]>
         * @Object ListTag
         * @ReturnType ListTag
         * @Async
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
            List<AbstractTag> copy = new ArrayList<>(obj.items());
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
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name shuffled
         * @RawName <ListTag.shuffled>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
         * @Description
         * Returns a copy of the list in a random order.
         * Do NOT use .random[9999] for shuffle the list!
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "shuffled", (attr, obj) -> {
            List<AbstractTag> copy = new ArrayList<>(obj.items());
            Collections.shuffle(copy);
            ListTag result = new ListTag();
            for (AbstractTag tag : copy) result.addObject(tag);
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name random[]
         * @RawName <ListTag.random[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Async
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
            if (obj.isEmpty()) return null;
            if (!attr.hasParam()) return obj.entryAt(ThreadLocalRandom.current().nextInt(obj.size()));
            int count = new ElementTag(attr.getParam()).asInt();
            if (count <= 0) return new ListTag();
            List<AbstractTag> copy = new ArrayList<>(obj.items());
            Collections.shuffle(copy, ThreadLocalRandom.current());
            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (int index = 0; index < limit; index++) result.addObject(copy.get(index));
            return result;
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name sum
         * @RawName <ListTag.sum>
         * @Object ListTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                ElementTag element = new ElementTag(tag.identify());
                if (element.isDouble()) total += element.asDouble();
            }
            return new ElementTag(total);
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name product
         * @RawName <ListTag.product>
         * @Object ListTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                ElementTag element = new ElementTag(tag.identify());
                if (element.isDouble()) total *= element.asDouble();
            }
            return new ElementTag(total);
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name average
         * @RawName <ListTag.average>
         * @Object ListTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                ElementTag element = new ElementTag(tag.identify());
                if (element.isDouble()) { total += element.asDouble(); count++; }
            }
            return new ElementTag(count == 0 ? 0 : total / count);
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name highest[]
         * @RawName <ListTag.highest[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Async
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
            if (obj.isEmpty()) return null;
            int count = attr.hasParam() ? new ElementTag(attr.getParam()).asInt() : 1;

            AbstractTag extreme = extremeOf(obj, count, true);
            if (extreme != null) return extreme;

            List<AbstractTag> copy = new ArrayList<>(obj.items());
            copy.sort(Comparator.comparingDouble(ListTag::numericValue).reversed());

            if (count == 1) return copy.getFirst();
            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (int index = 0; index < limit; index++) result.addObject(copy.get(index));

            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name lowest[]
         * @RawName <ListTag.lowest[(<#>)]>
         * @Object ListTag
         * @ReturnType ObjectTag
         * @Async
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
            if (obj.isEmpty()) return null;
            int count = attr.hasParam() ? new ElementTag(attr.getParam()).asInt() : 1;

            AbstractTag extreme = extremeOf(obj, count, false);
            if (extreme != null) return extreme;

            List<AbstractTag> copy = new ArrayList<>(obj.items());
            copy.sort(Comparator.comparingDouble(ListTag::numericValue));

            if (count == 1) return copy.getFirst();
            int limit = Math.min(count, copy.size());
            ListTag result = new ListTag();
            for (int index = 0; index < limit; index++) result.addObject(copy.get(index));

            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name combine
         * @RawName <ListTag.combine>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                for (AbstractTag subTag : new ListTag(tag.identify()).getList()) result.addObject(subTag);
            }
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name subLists[]
         * @RawName <ListTag.subLists[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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
            for (int start = 0; start < obj.size(); start += chunkSize) {
                int end = Math.min(start + chunkSize, obj.size());
                ListTag chunk = new ListTag();
                for (int index = start; index < end; index++) chunk.addObject(obj.entryAt(index));
                result.addString(chunk.identify());
            }
            return result;
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name mapWith[]
         * @RawName <ListTag.mapWith[<value>|...]>
         * @Object ListTag
         * @ReturnType MapTag
         * @ArgRequired
         * @Async
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
            int length = Math.min(obj.size(), values.size());
            for (int index = 0; index < length; index++) {
                result.putObject(obj.entryAt(index).identify(), values.get(index));
            }
            return result;
        }).test("1|2|3").setAsyncSafe();

        /* @doc tag
         *
         * @Name toMap[]
         * @RawName <ListTag.toMap[(<separator>)]>
         * @Object ListTag
         * @ReturnType MapTag
         * @Async
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
            for (AbstractTag tag : obj.items()) {
                String entry = tag.identify();
                int splitAt = entry.indexOf(separator);
                if (splitAt >= 0) result.putObject(
                        entry.substring(0, splitAt),
                        ObjectFetcher.pickObject(entry.substring(splitAt + separator.length()))
                );
            }
            return result;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name toJson
         * @RawName <ListTag.toJson>
         * @Object ListTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
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
             * @Async
             *
             * @Description
             * Converts the ListTag into an element with nicely formatted multiline JSON.
             */
            if (attr.matchesNext("pretty")) {
                attr.fulfill(1);
                return new ElementTag(JsonHelper.toPrettyString(json));
            }
            return new ElementTag(json.toString());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name dotProduct[]
         * @RawName <ListTag.dotProduct[<list>]>
         * @Object ListTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
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
            ListTag other = attr.getParamObject(ListTag.class, ListTag::new);
            if (other == null) return null;

            double[] left = obj.numericView();
            double[] right = other.numericView();

            if (left != null && right != null) {
                int size = Math.min(left.length, right.length);
                double sum = 0;
                for (int index = 0; index < size; index++) {
                    sum += left[index] * right[index];
                }
                return new ElementTag(sum);
            }

            List<AbstractTag> leftEntries = obj.items();
            List<AbstractTag> rightEntries = other.items();
            int size = Math.min(leftEntries.size(), rightEntries.size());
            double sum = 0;
            for (int index = 0; index < size; index++) {
                sum += numericValue(leftEntries.get(index)) * numericValue(rightEntries.get(index));
            }
            return new ElementTag(sum);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name activation[]
         * @RawName <ListTag.activation[(<type>)]>
         * @Object ListTag
         * @ReturnType ListTag
         * @Async
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

            double[] source = obj.numericView();

            if (type.equals("softmax")) {
                if (source == null) {
                    List<AbstractTag> entries = obj.items();
                    source = new double[entries.size()];
                    for (int index = 0; index < source.length; index++) {
                        source[index] = numericValue(entries.get(index));
                    }
                }

                int length = source.length;
                double max = Double.NEGATIVE_INFINITY;
                for (int index = 0; index < length; index++) {
                    if (source[index] > max) max = source[index];
                }

                double[] result = new double[length];
                double sumExp = 0;
                for (int index = 0; index < length; index++) {
                    double exp = Math.exp(source[index] - max);
                    result[index] = exp;
                    sumExp += exp;
                }

                if (sumExp != 0) {
                    double inverse = 1.0 / sumExp;
                    for (int index = 0; index < length; index++) result[index] *= inverse;
                }
                return ofNumbers(result);
            }

            if (source != null) {
                int length = source.length;
                double[] result = new double[length];
                double[] input = source;
                CorexComputePool.parallelFor(0, length, 4L, index ->
                        result[index] = applyActivation(input[index], type));
                return ofNumbers(result);
            }

            return applyActivationRecursively(obj, type);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name matrixMul[]
         * @RawName <ListTag.matrixMul[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
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

            List<AbstractTag> rows = obj.items();
            int rowCount = rows.size();
            double[] vector = other.numericView();

            if (vector != null) {
                double[][] matrix = new double[rowCount][];
                int usableRows = 0;
                for (int index = 0; index < rowCount; index++) {
                    if (rows.get(index) instanceof ListTag row) {
                        double[] rowValues = row.numericView();
                        if (rowValues == null) {
                            usableRows = -1;
                            break;
                        }
                        matrix[usableRows++] = rowValues;
                    }
                }

                if (usableRows >= 0) {
                    double[] products = new double[usableRows];
                    int vectorLength = vector.length;
                    CorexComputePool.parallelFor(0, usableRows, vectorLength, index -> {
                        double[] row = matrix[index];
                        int size = Math.min(row.length, vectorLength);
                        double sum = 0;
                        for (int column = 0; column < size; column++) {
                            sum += row[column] * vector[column];
                        }
                        products[index] = sum;
                    });
                    return ofNumbers(products);
                }
            }

            ListTag result = new ListTag();
            List<AbstractTag> otherItems = other.items();

            for (AbstractTag rowTag : rows) {
                if (!(rowTag instanceof ListTag row)) continue;

                double sum = 0;
                List<AbstractTag> rowItems = row.items();
                int size = Math.min(rowItems.size(), otherItems.size());

                for (int i = 0; i < size; i++) {
                    sum += numericValue(rowItems.get(i)) * numericValue(otherItems.get(i));
                }
                result.addObject(new ElementTag(sum));
            }

            return result;
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name reshape[]
         * @RawName <ListTag.reshape[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Splits a flat numeric list into rows of the given width, so it can be used as a matrix
         * by matrixMul. A trailing partial row is dropped. The rows are primitive-backed, so a
         * reshaped weight tensor costs the same memory as the flat list it came from.
         *
         * @Usage
         * // Turns 6 numbers into 3 rows of 2, narrates "3"
         * - narrate <list[1|2|3|4|5|6].reshape[2].size>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "reshape", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int columns = new ElementTag(attr.getParam()).asInt();
            if (columns <= 0) return null;

            double[] flat = obj.numericView();
            if (flat == null) return null;

            int rowCount = flat.length / columns;
            ListTag result = new ListTag();
            for (int row = 0; row < rowCount; row++) {
                double[] values = new double[columns];
                System.arraycopy(flat, row * columns, values, 0, columns);
                result.addObject(ofNumbers(values));
            }
            return result;
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name addList[]
         * @RawName <ListTag.addList[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Adds two numeric lists together entry by entry. The result is as long as the shorter
         * input. This is the residual connection in a neural network, not list concatenation,
         * which is 'include'.
         *
         * @Usage
         * // Narrates "li@5|7|9"
         * - narrate <list[1|2|3].addList[<list[4|5|6]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "addList", (attr, obj) ->
                elementwise(attr, obj, ElementwiseOperation.ADD)).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name subList[]
         * @RawName <ListTag.subList[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Subtracts the given numeric list from this one, entry by entry.
         *
         * @Usage
         * // Narrates "li@3|3|3"
         * - narrate <list[4|5|6].subList[<list[1|2|3]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "subList", (attr, obj) ->
                elementwise(attr, obj, ElementwiseOperation.SUBTRACT)).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name mulList[]
         * @RawName <ListTag.mulList[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Multiplies two numeric lists entry by entry. This is the elementwise product, used for
         * things like applying a layer norm gain, not the dot product and not a matrix multiply.
         *
         * @Usage
         * // Narrates "li@4|10|18"
         * - narrate <list[1|2|3].mulList[<list[4|5|6]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "mulList", (attr, obj) ->
                elementwise(attr, obj, ElementwiseOperation.MULTIPLY)).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name scale[]
         * @RawName <ListTag.scale[<#.#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Multiplies every numeric entry by one number.
         *
         * @Usage
         * // Narrates "li@2|4|6"
         * - narrate <list[1|2|3].scale[2]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "scale", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag factorTag = attr.getParamObject(ElementTag.class, ElementTag::new);
            if (factorTag == null || !factorTag.isDouble()) return null;
            double factor = factorTag.asDouble();

            double[] source = obj.numericView();
            if (source == null) return null;

            double[] result = new double[source.length];
            for (int index = 0; index < source.length; index++) result[index] = source[index] * factor;
            return ofNumbers(result);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name layerNorm
         * @RawName <ListTag.layerNorm>
         * @Object ListTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
         * @Description
         * Standardizes the list so it has mean 0 and variance 1, using the population variance and
         * an epsilon of 1e-5 to match what transformer implementations use. The learned gain and
         * bias are not applied here, chain mulList and addList for those.
         *
         * @Usage
         * // Normalize, then apply the layer's gain and bias.
         * - def normalized:<[hidden].layerNorm.mulList[<[gamma]>].addList[<[beta]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "layerNorm", (attr, obj) -> {
            double[] source = obj.numericView();
            if (source == null || source.length == 0) return null;

            int length = source.length;
            double mean = 0;
            for (int index = 0; index < length; index++) mean += source[index];
            mean /= length;

            double variance = 0;
            for (int index = 0; index < length; index++) {
                double delta = source[index] - mean;
                variance += delta * delta;
            }
            variance /= length;

            double inverse = 1.0 / Math.sqrt(variance + 1.0e-5);
            double[] result = new double[length];
            for (int index = 0; index < length; index++) result[index] = (source[index] - mean) * inverse;
            return ofNumbers(result);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name weightedSum[]
         * @RawName <ListTag.weightedSum[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Treats this list as rows and returns the rows added together, each scaled by the matching
         * entry of the given weight list. This is what attention does once it has its probabilities:
         * the weights are the attention scores and the rows are the cached values.
         * Rows shorter than the widest one are padded with zero, and extra weights are ignored.
         *
         * @Usage
         * // Half of the first row plus half of the second, narrates "li@2|3"
         * - narrate <list[<list[1|2]>|<list[3|4]>].weightedSum[<list[0.5|0.5]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "weightedSum", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ListTag weightList = attr.getParamObject(ListTag.class, ListTag::new);
            if (weightList == null) return null;
            double[] weights = weightList.numericView();
            if (weights == null) return null;

            List<AbstractTag> rows = obj.items();
            int rowCount = Math.min(rows.size(), weights.length);
            if (rowCount == 0) return new ListTag();

            double[][] matrix = new double[rowCount][];
            int width = 0;
            for (int index = 0; index < rowCount; index++) {
                if (!(rows.get(index) instanceof ListTag row)) return null;
                double[] values = row.numericView();
                if (values == null) return null;
                matrix[index] = values;
                if (values.length > width) width = values.length;
            }

            int columns = width;
            double[] result = new double[columns];
            CorexComputePool.parallelFor(0, columns, rowCount, column -> {
                double sum = 0;
                for (int index = 0; index < rowCount; index++) {
                    double[] row = matrix[index];
                    if (column < row.length) sum += row[column] * weights[index];
                }
                result[column] = sum;
            });
            return ofNumbers(result);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name indexOfHighest
         * @RawName <ListTag.indexOfHighest>
         * @Object ListTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the 1-based index of the largest numeric entry, in a single pass. Ties go to the
         * earliest entry. Prefer this over 'highest' followed by 'find': that pair walks the list
         * twice and compares text, which on a long list costs far more than the search itself.
         * Returns -1 for an empty list.
         *
         * @Usage
         * // Which vocabulary entry did the model score highest.
         * - def bestIndex:<[logits].indexOfHighest>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "indexOfHighest", (attr, obj) -> {
            double[] numbers = obj.numericView();

            if (numbers != null) {
                if (numbers.length == 0) return new ElementTag(-1);
                int best = 0;
                for (int index = 1; index < numbers.length; index++) {
                    if (numbers[index] > numbers[best]) best = index;
                }
                return new ElementTag(best + 1);
            }

            int size = obj.size();
            if (size == 0) return new ElementTag(-1);
            int best = 0;
            double bestValue = numericValue(obj.entryAt(0));
            for (int index = 1; index < size; index++) {
                double value = numericValue(obj.entryAt(index));
                if (value > bestValue) {
                    bestValue = value;
                    best = index;
                }
            }
            return new ElementTag(best + 1);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name setObject[]
         * @RawName <ListTag.setObject[<object>].at[<#>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a copy of the list with the entry at the given index replaced by the value, kept
         * whole. This is to 'set' what 'push' is to 'include': set splices a list parameter's
         * entries into place, this one stores it as a single entry. Use it to update one slot of a
         * list of lists.
         *
         * @Usage
         * // Replaces the second row, narrates "li@li@1|2|li@9|9"
         * - narrate <list[<list[1|2]>|<list[3|4]>].setObject[<list[9|9]>].at[2]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "setObject", (attr, obj) -> {
            if (!attr.hasParam() || !attr.matchesNext("at") || !attr.hasNextParam()) return null;
            AbstractTag value = attr.getParamObject();
            int target = resolveIndex(attr.getNextParam(), obj.size());
            attr.fulfill(1);
            if (value == null || target < 0) return null;

            ListTag result = new ListTag();
            for (int index = 0; index < obj.size(); index++) {
                result.addObject(index == target ? value : obj.entryAt(index));
            }
            return result;
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name rmsNorm[]
         * @RawName <ListTag.rmsNorm[(<#.#>)]>
         * @Object ListTag
         * @ReturnType ListTag
         * @Async
         * @Description
         * Divides every entry by the root mean square of the list. Unlike layerNorm this does not
         * subtract the mean and has no bias, which is what Llama, Qwen and most models newer than
         * GPT-2 use. The optional parameter is epsilon, default 1e-6. Apply the learned gain
         * separately with mulList.
         *
         * @Usage
         * // Normalize a hidden state and apply the layer's gain.
         * - def normalized:<[hidden].rmsNorm.mulList[<[gamma]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "rmsNorm", (attr, obj) -> {
            double[] source = obj.numericView();
            if (source == null || source.length == 0) return null;

            double epsilon = 1.0e-6;
            if (attr.hasParam()) {
                ElementTag epsilonParam = attr.getParamObject(ElementTag.class, ElementTag::new);
                if (epsilonParam != null && epsilonParam.isDouble()) epsilon = epsilonParam.asDouble();
            }

            int length = source.length;
            double sumOfSquares = 0;
            for (int index = 0; index < length; index++) sumOfSquares += source[index] * source[index];

            double inverse = 1.0 / Math.sqrt(sumOfSquares / length + epsilon);
            double[] result = new double[length];
            for (int index = 0; index < length; index++) result[index] = source[index] * inverse;
            return ofNumbers(result);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name rope[]
         * @RawName <ListTag.rope[<#>].theta[(<#.#>)]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Applies rotary position embedding for the given 0-based position, rotating the first
         * half of the list against the second half. This is how Llama and Qwen encode position:
         * instead of adding a position vector once at the start like GPT-2, every layer rotates
         * its queries and keys by an angle that depends on where the token sits.
         * The list must be one head's vector, not the whole hidden state, and its length must be
         * even. Optional '.theta[]' sets the frequency base, default 10000 (Qwen3 uses 1000000).
         *
         * @Usage
         * // Rotate one head's query vector for position 5.
         * - def rotated:<[queryHead].rope[5].theta[1000000]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "rope", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag positionParam = attr.getParamObject(ElementTag.class, ElementTag::new);
            if (positionParam == null || !positionParam.isDouble()) return null;
            double position = positionParam.asDouble();

            double theta = 10000.0;
            if (attr.matchesNext("theta") && attr.hasNextParam()) {
                ElementTag thetaParam = attr.getNextParamObject(ElementTag.class, ElementTag::new);
                attr.fulfill(1);
                if (thetaParam != null && thetaParam.isDouble()) theta = thetaParam.asDouble();
            }

            double[] source = obj.numericView();
            if (source == null || source.length == 0 || (source.length & 1) != 0) return null;

            int length = source.length;
            int half = length / 2;
            double[] result = new double[length];
            double base = theta;

            for (int index = 0; index < half; index++) {
                double frequency = 1.0 / Math.pow(base, (2.0 * index) / length);
                double angle = position * frequency;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double first = source[index];
                double second = source[index + half];
                result[index] = first * cos - second * sin;
                result[index + half] = second * cos + first * sin;
            }
            return ofNumbers(result);
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name concat[]
         * @RawName <ListTag.concat[<list>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Joins two numeric lists end to end and keeps the result primitive-backed. This is what
         * 'include' does, except include materializes an object per entry, which matters when you
         * are building a long vector one piece at a time. Falls back to include's behaviour if
         * either side is not purely numeric.
         *
         * @Usage
         * // Narrates "li@1|2|3|4"
         * - narrate <list[1|2].concat[<list[3|4]>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "concat", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ListTag other = attr.getParamObject(ListTag.class, ListTag::new);
            if (other == null) return null;

            double[] left = obj.numericView();
            double[] right = other.numericView();

            if (left != null && right != null) {
                double[] result = new double[left.length + right.length];
                System.arraycopy(left, 0, result, 0, left.length);
                System.arraycopy(right, 0, result, left.length, right.length);
                return ofNumbers(result);
            }

            ListTag result = new ListTag();
            for (AbstractTag tag : obj.items()) result.addObject(tag);
            for (AbstractTag tag : other.items()) result.addObject(tag);
            return result;
        }).ignoreTest().setAsyncSafe();

        /* @doc tag
         *
         * @Name push[]
         * @RawName <ListTag.push[<object>]>
         * @Object ListTag
         * @ReturnType ListTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a copy of the list with the given value added as one single entry, whatever it is.
         * Use this instead of 'include' when the value is itself a list and you want a list of lists,
         * since include would spread the inner entries into the outer list.
         *
         * @Usage
         * // Narrates "2" - one original entry plus one list entry
         * - narrate <list[a].push[<list[b|c]>].size>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "push", (attr, obj) -> {
            AbstractTag value = attr.getParamObject();
            if (value == null) return null;

            ListTag result = new ListTag();
            for (AbstractTag tag : obj.items()) result.addObject(tag);
            result.addObject(value);
            return result;
        }).ignoreTest().setAsyncSafe();
    }

    private enum ElementwiseOperation { ADD, SUBTRACT, MULTIPLY }

    private static ListTag elementwise(Attribute attr, ListTag obj, ElementwiseOperation operation) {
        if (!attr.hasParam()) return null;
        ListTag other = attr.getParamObject(ListTag.class, ListTag::new);
        if (other == null) return null;

        double[] left = obj.numericView();
        double[] right = other.numericView();
        if (left == null || right == null) return null;

        int length = Math.min(left.length, right.length);
        double[] result = new double[length];
        switch (operation) {
            case ADD -> {
                for (int index = 0; index < length; index++) result[index] = left[index] + right[index];
            }
            case SUBTRACT -> {
                for (int index = 0; index < length; index++) result[index] = left[index] - right[index];
            }
            case MULTIPLY -> {
                for (int index = 0; index < length; index++) result[index] = left[index] * right[index];
            }
        }
        return ofNumbers(result);
    }

    public ListTag() {}

    public ListTag(String raw) {
        if (raw == null || raw.isEmpty()) return;
        if (raw.startsWith(prefix + "@")) raw = raw.substring(prefix.length() + 1);
        for (String entry : ObjectFetcher.splitIgnoringBrackets(raw, '|')) {
            if (!entry.isEmpty()) appendTag(ObjectFetcher.pickObject(entry));
        }
    }

    public ListTag(List<?> list) {
        for (Object element : list) {
            if (element == null) continue;
            if (element instanceof AbstractTag tag) {
                appendTag(tag);
            } else {
                appendTag(ObjectFetcher.pickObject(element.toString()));
            }
        }
    }

    /**
     * Returns all list entries that are an instance of {@code clazz}.
     * Entries that do not match log an error to {@code queue} (pass {@code null} to suppress).
     */
    public <T extends AbstractTag> List<T> filter(Class<T> clazz, @Nullable ScriptQueue queue) {
        List<T> results = new ArrayList<>();
        for (AbstractTag item : items()) {
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

        List<AbstractTag> results = new ArrayList<>(size());
        for (AbstractTag item : items()) {
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

            result.addObject(new ElementTag(applyActivation(numericValue(item), type)));
        }
        return result;
    }

    private static final double GELU_COEFFICIENT = Math.sqrt(2.0 / Math.PI);

    private static double applyActivation(double value, String type) {
        return switch (type) {
            case "relu" -> Math.max(0, value);
            case "sigmoid" -> 1.0 / (1.0 + Math.exp(-value));
            case "tanh" -> Math.tanh(value);
            case "gelu" -> 0.5 * value
                    * (1.0 + Math.tanh(GELU_COEFFICIENT * (value + 0.044715 * value * value * value)));
            case "silu" -> value / (1.0 + Math.exp(-value));
            default -> value;
        };
    }

    /**
     * Wraps an array of numbers without allocating an {@link ElementTag} per entry.
     * <p>
     * The array is taken by reference and must not be modified afterwards. Entries are
     * materialized as tags only if something actually reads the list as objects, so a list
     * produced by numeric tags and consumed by numeric tags never boxes at all.
     *
     * @param values the backing values, taken by reference
     * @return a numeric-backed list of {@code values.length} entries
     */
    public static ListTag ofNumbers(double[] values) {
        ListTag result = new ListTag();
        result.numericEntries = values;
        return result;
    }

    /**
     * Returns the entries as primitive doubles, or {@code null} if any entry is not numeric.
     * <p>
     * The returned array is the live backing store, not a copy. Callers must treat it as
     * read-only. A boxed list is converted once and the result is cached, so repeated numeric
     * tags on the same list pay the conversion a single time.
     *
     * @return the backing values, or {@code null} when the list is not purely numeric
     */
    public double[] numericView() {
        double[] current = numericEntries;
        if (current != null) return current;
        if (numericDerivationFailed) return null;
        if (boxedEntries == null) return EMPTY_NUMBERS;

        List<AbstractTag> source = boxedEntries;
        int length = source.size();
        double[] derived = new double[length];
        for (int index = 0; index < length; index++) {
            if (!(source.get(index) instanceof ElementTag element) || !element.isDouble()) {
                this.numericDerivationFailed = true;
                return null;
            }
            derived[index] = element.asDouble();
        }

        this.numericEntries = derived;
        return derived;
    }

    public List<AbstractTag> getList() {
        return new ArrayList<>(items());
    }

    public int size() {
        List<AbstractTag> entries = boxedEntries;
        if (entries != null) return entries.size();
        double[] numbers = numericEntries;
        return numbers != null ? numbers.length : 0;
    }

    public String get(int index) {
        List<AbstractTag> entries = items();
        return (index >= 0 && index < entries.size()) ? entries.get(index).identify() : null;
    }

    public void addString(String value) {
        if (value != null) appendTag(new ElementTag(value));
    }

    public void addObject(AbstractTag tag) {
        if (tag != null) appendTag(tag);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns one entry without materializing the whole list.
     * <p>
     * Reading a single index out of a numeric-backed list must not force it into objects: on a
     * weight tensor that would turn a few megabytes of primitives into tens of millions of tags.
     */
    private AbstractTag entryAt(int index) {
        if (index < 0) return null;

        List<AbstractTag> current = boxedEntries;
        if (current != null) {
            return index < current.size() ? current.get(index) : null;
        }

        double[] numbers = numericEntries;
        if (numbers != null && index < numbers.length) {
            return new ElementTag(numbers[index]);
        }
        return null;
    }

    private List<AbstractTag> items() {
        List<AbstractTag> current = boxedEntries;
        if (current != null) return current;

        double[] numbers = numericEntries;
        int length = numbers != null ? numbers.length : 0;
        current = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            current.add(new ElementTag(numbers[index]));
        }
        this.boxedEntries = current;
        return current;
    }

    private void appendTag(AbstractTag tag) {
        if (tag == null) return;
        items().add(tag);
        this.numericEntries = null;
        this.numericDerivationFailed = false;
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

    /**
     * Picks the largest or smallest entries of a numeric list without boxing or sorting objects.
     * <p>
     * Returns {@code null} when the list is not numeric, leaving the caller to fall back to the
     * general path. Finding one extreme is a single linear pass; asking for several sorts a
     * primitive copy, which is still far cheaper than sorting materialized tags.
     */
    private static AbstractTag extremeOf(ListTag source, int count, boolean highest) {
        double[] numbers = source.numericView();
        if (numbers == null || numbers.length == 0 || count < 1) return null;

        if (count == 1) {
            double best = numbers[0];
            for (int index = 1; index < numbers.length; index++) {
                double value = numbers[index];
                if (highest ? value > best : value < best) best = value;
            }
            return new ElementTag(best);
        }

        double[] sorted = numbers.clone();
        Arrays.sort(sorted);
        int limit = Math.min(count, sorted.length);
        double[] picked = new double[limit];
        for (int index = 0; index < limit; index++) {
            picked[index] = highest ? sorted[sorted.length - 1 - index] : sorted[index];
        }
        return ofNumbers(picked);
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
                long numFirst = Long.parseLong(first.substring(startFirst, posFirst));
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

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        StringBuilder builder = new StringBuilder(prefix).append('@');

        double[] numbers = numericEntries;
        if (boxedEntries == null && numbers != null) {
            for (int index = 0; index < numbers.length; index++) {
                if (index > 0) builder.append('|');
                double value = numbers[index];
                if (value == (long) value) builder.append((long) value);
                else builder.append(value);
            }
            return builder.toString();
        }

        List<AbstractTag> entries = items();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) builder.append('|');
            builder.append(entries.get(index).identify());
        }
        return builder.toString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<ListTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "li@a|b|c";
    }
}
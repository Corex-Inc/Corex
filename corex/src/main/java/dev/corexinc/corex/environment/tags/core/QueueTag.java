package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.world.RegionTag;
import org.bukkit.Location;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Map;

/* @doc object
 *
 * @Name QueueTag
 * @Prefix q
 * @Format
 * The identity format for QueueTags is 'q@<id>', where '<id>' is the unique identifier of the script queue.
 *
 * @Description
 * A QueueTag represents an active or finished script queue.
 * This can be used to track the status of running scripts, retrieve their definitions (variables),
 * or identify which region/thread they are currently running on.
 */
public class QueueTag implements AbstractTag {

    private static final String PREFIX = "q";
    private final ScriptQueue queue;

    public static final TagProcessor<QueueTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("queue", attr -> {
            if (attr.hasParam()) return new QueueTag(attr.getParam());
            return new QueueTag(attr.getQueue());
        });

        ObjectFetcher.registerFetcher(PREFIX, QueueTag::new);

        /* @doc tag
         *
         * @Name id
         * @RawName <QueueTag.id>
         * @Object QueueTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the unique identifier of the queue.
         */
        PROCESSOR.registerTag(ElementTag.class, "id", (attr, obj) -> new ElementTag(obj.queue.getId()));

        /* @doc tag
         *
         * @Name isAsync
         * @RawName <QueueTag.isAsync>
         * @Object QueueTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns 'true' if the queue is running asynchronously (not on the main/region thread).
         */
        PROCESSOR.registerTag(ElementTag.class, "isAsync", (attr, obj) -> new ElementTag(obj.queue.isAsync()));

        /* @doc tag
         *
         * @Name isCancelled
         * @RawName <QueueTag.isCancelled>
         * @Object QueueTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns 'true' if the queue has been cancelled.
         */
        PROCESSOR.registerTag(ElementTag.class, "isCancelled", (attr, obj) -> new ElementTag(obj.queue.isCancelled()));

        /* @doc tag
         *
         * @Name region
         * @RawName <QueueTag.region>
         * @Object QueueTag
         * @ReturnType RegionTag
         * @NoArg
         * @Description
         * Returns the region (thread) that this queue belongs to.
         * If the queue has no anchor location, it returns the global region ('reg@global').
         */
        PROCESSOR.registerTag(RegionTag.class, "region", (attr, obj) -> {
            Location anchor = obj.queue.getAnchorLocation();
            if (anchor == null) return new RegionTag("global");
            return new RegionTag(anchor.getWorld(), anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4);
        });

        /* @doc tag
         *
         * @Name returns
         * @RawName <QueueTag.returns>
         * @Object QueueTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a ListTag of all values returned by the queue using the 'determine' command.
         */
        PROCESSOR.registerTag(ListTag.class, "returns", (attr, obj) -> {
            ListTag list = new ListTag("");
            for (AbstractTag tag : obj.queue.getReturns()) list.addObject(tag);
            return list;
        });

        /* @doc tag
         *
         * @Name definition[]
         * @RawName <QueueTag.definition[<name>]>
         * @Object QueueTag
         * @ReturnType ObjectTag
         * @ArgRequired
         * @Description
         * Returns the value of a specific definition (variable) stored within the queue.
         */
        PROCESSOR.registerTag(AbstractTag.class, "definition", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return obj.queue.getDefinition(attr.getParam());
        }).test("testDef");

        /* @doc tag
         *
         * @Name definitions
         * @RawName <QueueTag.definitions>
         * @Object QueueTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a MapTag of all definitions (variables) currently stored in the queue.
         */
        PROCESSOR.registerTag(MapTag.class, "definitions", (attr, obj) -> {
            MapTag map = new MapTag("");
            for (Map.Entry<String, AbstractTag> def : obj.queue.getDefinitionsMap().entrySet()) {
                map.putObject(def.getKey(), def.getValue());
            }
            return map;
        });
    }

    public QueueTag(String id) {
        String clean = id.toLowerCase().startsWith(PREFIX + "@") ? id.substring(2) : id;
        this.queue = ScriptQueue.getQueueById(clean);
    }

    public QueueTag(ScriptQueue queue) {
        this.queue = queue;
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }
    @Override public @NonNull String identify() { return queue != null ? PREFIX + "@" + queue.getId() : "null"; }
    @Override public @Nullable AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<QueueTag> getProcessor() { return PROCESSOR; }
    @Override public @NonNull String getTestValue() { return "q@test_queue"; }
}
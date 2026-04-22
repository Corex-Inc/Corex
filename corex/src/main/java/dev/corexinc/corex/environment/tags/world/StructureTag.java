package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/* @doc object
 *
 * @Name StructureTag
 * @Prefix structure
 *
 * @Format
 * The identity format for structures is `structure@namespace:key`.
 * For example: `structure@minecraft:igloo` or `structure@myplugin:my_house`.
 * The key portion follows Minecraft's NamespacedKey format and may include
 * slashes for nested paths, e.g. `structure@minecraft:village/plains/town_center_1`.
 *
 * @Description
 * A StructureTag represents a named structure template — a saved snapshot of
 * blocks and entities that can be placed back into the world.
 *
 * Structures are identified by their NamespacedKey and managed by the server's
 * StructureManager. In order for a structure to be resolvable, it must be
 * registered with the StructureManager, located in a DataPack, in the primary
 * world folder, or in the server's built-in default resources.
 */
public class StructureTag implements AbstractTag {

    private static final String PREFIX = "structure";

    public static final TagProcessor<StructureTag> TAG_PROCESSOR = new TagProcessor<>();

    private final @Nullable Structure structure;
    private final @Nullable NamespacedKey key;

    public StructureTag(@NonNull Structure structure, @NonNull NamespacedKey key) {
        this.structure = structure;
        this.key = key;
    }

    public StructureTag(@NonNull NamespacedKey key) {
        this.key = key;
        this.structure = Bukkit.getStructureManager().getStructure(key);
    }

    public StructureTag(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            this.key = null;
            this.structure = null;
            return;
        }

        String stripped = raw.toLowerCase();
        if (stripped.startsWith(PREFIX + "@")) {
            stripped = stripped.substring(PREFIX.length() + 1);
        }

        NamespacedKey resolvedKey = NamespacedKey.fromString(stripped);
        Structure resolvedStructure = null;

        if (resolvedKey != null) {
            try {
                resolvedStructure = Bukkit.getStructureManager().loadStructure(resolvedKey);
            } catch (Exception ignored) {

            }
        }

        this.key = resolvedKey;
        this.structure = resolvedStructure;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag(PREFIX, attr -> {
            if (!attr.hasParam()) return null;
            StructureTag tag = new StructureTag(attr.getParam());
            return tag.structure != null ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, StructureTag::new);

        /* @doc tag
         *
         * @Name key
         * @RawName <StructureTag.key>
         * @Object StructureTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the namespaced key of this structure in the format `namespace:key`.
         * For example: `minecraft:village/plains/houses/plains_small_house_1`.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "key", (attr, obj) ->
                obj.key != null ? new ElementTag(obj.key.toString()) : null);

        /* @doc tag
         *
         * @Name size
         * @RawName <StructureTag.size>
         * @Object StructureTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns the dimensions of this structure as a ListTag.
         * Contains width, height, and depth in blocks.
         * For example, a 5x4x3 structure returns `li@5|4|3`.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "size", (attr, obj) -> {
            if (obj.structure == null) return new ListTag();
            BlockVector size = obj.structure.getSize();
            return new ListTag(List.of(
                    new ElementTag(size.getBlockX()),
                    new ElementTag(size.getBlockY()),
                    new ElementTag(size.getBlockZ())
            ));
        });

        /* @doc tag
         *
         * @Name paletteCount
         * @RawName <StructureTag.paletteCount>
         * @Object StructureTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the number of block palettes stored in this structure.
         * A structure may contain multiple palettes as random block variants.
         * When placing, passing palette index -1 selects one at random.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "paletteCount", (attr, obj) ->
                obj.structure != null ? new ElementTag(obj.structure.getPaletteCount()) : null
        ).ignoreTest();

        /* @doc tag
         *
         * @Name entities
         * @RawName <StructureTag.entities>
         * @Object StructureTag
         * @ReturnType ListTag(EntityTag)
         * @NoArg
         * @Description
         * Returns a list of entities saved in this structure template.
         * These are template copies; their positions are relative offsets
         * from the structure's origin, not absolute world coordinates.
         * Returns an empty list if the structure contains no entities.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "entities", (attr, obj) -> {
            ListTag result = new ListTag();
            if (obj.structure == null) return result;
            for (var entity : obj.structure.getEntities()) {
                result.addObject(new EntityTag(entity));
            }
            return result;
        });
    }

    public @Nullable Structure getStructure() {
        if (structure != null) return structure;
        if (key == null) return null;
        return Bukkit.getStructureManager().getStructure(key);
    }

    public @Nullable NamespacedKey getKey() {
        return key;
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + key;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<StructureTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "structure@minecraft:igloo";
    }
}
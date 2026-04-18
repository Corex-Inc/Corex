package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/* @doc object
 *
 * @Name MaterialTag
 * @Prefix m
 *
 * @Description
 * A MaterialTag encapsulates a specific material, which can be either a block or an item type.
 * For block materials, it can also hold specific property data, such as the growth stage of a crop or the direction a stair block is facing.
 * The names of materials correspond to Bukkit's official Material enum names.
 *
 * MaterialTags can be matched using several patterns:
 * `block`: Matches if the material is a placeable block.
 * `item`: Matches if the material is an item.
 * `material_flagged:<flag_name>`: Matches if the material has the specified flag.
 * `vanilla_tagged:<tag_name>`: Matches if the material is part of a specific vanilla Minecraft tag group (e.g., `wool`, `planks`).
 * If no specific matcher is used, it performs a wildcard match on the material's name (e.g., `*_log`).
 *
 * @Usage
 * // Places a stone block beneath the player.
 * - setblock <player.location.below> <material[stone]>
 *
 * @Usage
 * // Checks if the player is holding any type of sword.
 * - if <player.itemInHand.material> matches "*_sword":
 *   - narrate "You are holding a sword!"
 */
public class MaterialTag implements AbstractTag {

    private static final String prefix = "m";
    private final Material material;
    private final BlockData blockData;

    public static final TagProcessor<MaterialTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("material", attr -> new MaterialTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, MaterialTag::new);

        /* @doc tag
         *
         * @Name name
         * @RawName <MaterialTag.name>
         * @Object MaterialTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Retrieves the common name of this material as a text string.
         * For example, "STONE" or "DIRT".
         *
         * @Implements MaterialTag.name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) -> new ElementTag(obj.material.name().toLowerCase()));

        /* @doc tag
         *
         * @Name isBlock
         * @RawName <MaterialTag.isBlock>
         * @Object MaterialTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Checks if this material represents a placeable block within the game world.
         * Returns 'true' if it can be placed as a block, otherwise 'false'.
         *
         * @Implements MaterialTag.is_block
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isBlock", (attr, obj) -> new ElementTag(obj.material.isBlock()));

        /* @doc tag
         *
         * @Name isItem
         * @RawName <MaterialTag.isItem>
         * @Object MaterialTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Determines if this material can be held as an item.
         * Most block materials are also holdable items.
         * This tag returns 'false' only for specific non-holdable block types, such as "Fire".
         *
         * @Implements MaterialTag.is_item
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isItem", (attr, obj) -> new ElementTag(obj.material.isItem()));

        /* @doc tag
         *
         * @Name isEdible
         * @RawName <MaterialTag.isEdible>
         * @Object MaterialTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Checks if this material is an edible item that can be consumed by players.
         * Returns 'true' if the material is edible, otherwise 'false'.
         *
         * @Implements MaterialTag.is_edible
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isEdible", (attr, obj) -> new ElementTag(obj.material.isEdible()));

        /* @doc tag
         *
         * @Name maxStackSize
         * @RawName <MaterialTag.maxStackSize>
         * @Object MaterialTag
         * @ReturnType ElementTag(Number)
         * @Mechanism MaterialTag.maxStackSize
         * @NoArg
         * @Description
         * Retrieves the maximum quantity of this material that can be present in a single inventory stack.
         *
         * @Implements MaterialTag.max_stack_size
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxStackSize", (attr, obj) -> new ElementTag(obj.material.getMaxStackSize()));
    }

    public MaterialTag(Material material) {
        this.material = material;
        this.blockData = material.isBlock() ? material.createBlockData() : null;
    }

    public MaterialTag(Block block) {
        this.material = block.getType();
        this.blockData = block.getBlockData();
    }

    public MaterialTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.material = Material.AIR;
            this.blockData = material.createBlockData();
            return;
        }

        if (raw.toLowerCase().startsWith(prefix + "@")) {
            raw = raw.substring(2);
        }

        if (raw.equalsIgnoreCase("RANDOM")) {
            Material[] values = Material.values();
            this.material = values[ThreadLocalRandom.current().nextInt(values.length)];
            this.blockData = this.material.isBlock() ? this.material.createBlockData() : null;
            return;
        }

        Material match = Material.matchMaterial(raw.toUpperCase());

        this.material = Objects.requireNonNullElse(match, Material.AIR);

        this.blockData = this.material.isBlock() ? this.material.createBlockData() : null;
    }

    public Material getMaterial() { return material; }
    public BlockData getBlockData() { return blockData; }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + material.name().toLowerCase();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<MaterialTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "m@stone";
    }
}
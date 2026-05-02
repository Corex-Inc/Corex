package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

/* @doc object
 *
 * @Name MaterialTag
 * @Prefix m
 *
 * @Description
 * A MaterialTag encapsulates a specific material, which can be either a block or an item type.
 * For block materials, it also holds specific property data (Block State), such as the growth stage of a crop, the level of a cauldron, or the direction a stair block is facing.
 *
 * Properties can be defined dynamically using brackets:
 * `m@oak_stairs[facing=east;waterlogged=true;half=top]`
 *
 * The engine dynamically parses ANY vanilla block state, meaning it automatically supports new block properties added in future Minecraft updates without requiring engine updates.
 *
 * @Usage
 * // Places an upside-down, eastward-facing stair block.
 * - setblock <player.location> <material[oak_stairs].with[facing=east|half=top]>
 */
public class MaterialTag implements AbstractTag, Adjustable {

    private static final String prefix = "m";
    private final Material material;
    private BlockData blockData;

    public static final TagProcessor<MaterialTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<MaterialTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

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
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isEdible", (attr, obj) -> new ElementTag(obj.material.isEdible()));

        /* @doc tag
         *
         * @Name maxStackSize
         * @RawName <MaterialTag.maxStackSize>
         * @Object MaterialTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Retrieves the maximum quantity of this material that can be present in a single inventory stack.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxStackSize", (attr, obj) -> new ElementTag(obj.material.getMaxStackSize()));

        /* @doc tag
         *
         * @Name properties
         * @RawName <MaterialTag.properties>
         * @Object MaterialTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a MapTag containing all block state properties currently applied to this material.
         * For example, returns a map like: map@[facing=east;waterlogged=false;half=bottom]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "properties", (attr, obj) -> {
            if (obj.getBlockData() == null) return null;

            String dataStr = obj.getBlockData().getAsString(false);
            int bracket = dataStr.indexOf('[');
            if (bracket == -1) return new MapTag();

            String props = dataStr.substring(bracket + 1, dataStr.length() - 1);
            MapTag map = new MapTag();

            for (String pair : props.split(",")) {
                String[] split = pair.split("=");
                if (split.length == 2) {
                    map.putObject(split[0], new ElementTag(split[1]));
                }
            }
            return map;
        });

        /* @doc tag
         *
         * @Name property[]
         * @RawName <MaterialTag.property[<name>]>
         * @Object MaterialTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Description
         * Returns the value of a specific block state property (like 'facing', 'level', 'waterlogged', 'age').
         * Returns null if the property does not exist on this material.
         *
         * @Usage
         * - narrate "Cauldron water level is: <context.material.property[level]>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "property", (attr, obj) -> {
            if (!attr.hasParam() || obj.getBlockData() == null) return null;
            String key = attr.getParam().toLowerCase();

            String dataStr = obj.getBlockData().getAsString(false);
            int start = dataStr.indexOf(key + "=");
            if (start == -1) return null;

            start += key.length() + 1;
            int end = dataStr.indexOf(',', start);
            if (end == -1) end = dataStr.indexOf(']', start);
            if (end == -1) return null;

            return new ElementTag(dataStr.substring(start, end));
        }).ignoreTest();

        /* @doc tag
         *
         * @Name availableProperties
         * @RawName <MaterialTag.availableProperties>
         * @Object MaterialTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Description
         * Returns a ListTag containing all valid block state property names that can be applied to this material.
         * Returns null if the material is an item with no block properties.
         *
         * @Usage
         * - narrate "Properties for wildflowers: <material[wildflowers].availableProperties>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "availableProperties", (attr, obj) -> {
            if (obj.getBlockData() == null) return null;

            String dataStr = obj.getBlockData().getAsString(false);
            int bracket = dataStr.indexOf('[');

            if (bracket == -1) return new ListTag();

            String props = dataStr.substring(bracket + 1, dataStr.length() - 1);
            StringJoiner keys = new StringJoiner("|");

            for (String pair : props.split(",")) {
                String[] split = pair.split("=");
                if (split.length == 2) {
                    keys.add(split[0]);
                }
            }
            return new ListTag(keys.toString());
        });
    }

    public MaterialTag(Material material) {
        this.material = material;
        this.blockData = material.isBlock() ? material.createBlockData() : null;
    }

    public MaterialTag(Block block) {
        this.material = block.getType();
        this.blockData = block.getBlockData().clone();
    }

    private MaterialTag(Material material, BlockData blockData) {
        this.material = material;
        this.blockData = blockData != null ? blockData.clone() : null;
    }

    public MaterialTag(@NotNull BlockData blockData) {
        this.material = blockData.getMaterial();
        this.blockData = blockData.clone();
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

        if (raw.equalsIgnoreCase("random")) {
            Material[] values = Material.values();
            this.material = values[ThreadLocalRandom.current().nextInt(values.length)];
            this.blockData = this.material.isBlock() ? this.material.createBlockData() : null;
            return;
        }

        int bracketStart = raw.indexOf('[');
        if (bracketStart > 0 && raw.endsWith("]")) {
            String matName = raw.substring(0, bracketStart);
            String properties = raw.substring(bracketStart + 1, raw.length() - 1);

            Material match = Material.matchMaterial(matName.toUpperCase());
            this.material = Objects.requireNonNullElse(match, Material.AIR);

            if (this.material.isBlock()) {
                String bukkitProps = properties.replace(';', ',');
                try {
                    this.blockData = Bukkit.createBlockData(this.material, "[" + bukkitProps + "]");
                } catch (Exception e) {
                    this.blockData = this.material.createBlockData();
                }
            } else {
                this.blockData = null;
            }
        } else {
            Material match = Material.matchMaterial(raw.toUpperCase());
            this.material = Objects.requireNonNullElse(match, Material.AIR);
            this.blockData = this.material.isBlock() ? this.material.createBlockData() : null;
        }
    }

    @Override
    public @NonNull Adjustable duplicate() {
        return new MaterialTag(this.material, this.blockData);
    }

    public Material getMaterial() { return material; }
    public BlockData getBlockData() { return blockData; }

    @Override
    public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        StringBuilder sb = new StringBuilder(prefix).append("@").append(material.name().toLowerCase());

        if (blockData != null) {
            String dataStr = blockData.getAsString(true);
            int bracket = dataStr.indexOf('[');
            if (bracket != -1) {
                String props = dataStr.substring(bracket).replace(',', ';');
                sb.append(props);
            }
        }

        return sb.toString();
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
    public @NonNull MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        AbstractTag processed = MECHANISM_PROCESSOR.process(this, mechanism, value);

        if (processed != this) {
            return processed;
        }

        if (this.blockData != null) {
            try {
                BlockData mergeData = Bukkit.createBlockData(this.material, "[" + mechanism + "=" + value.identify() + "]");
                MaterialTag copy = (MaterialTag) this.duplicate();

                copy.blockData = copy.blockData.merge(mergeData);

                return copy;
            } catch (Exception ignored) {
            }
        }

        return this;
    }

    @Override
    public @NonNull String getTestValue() {
        return "m@cauldron";
    }
}
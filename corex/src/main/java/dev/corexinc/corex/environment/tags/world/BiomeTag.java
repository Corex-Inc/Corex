package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ColorTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.adapters.BiomeAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc object
 *
 * @Name BiomeTag
 * @Prefix biome
 *
 * @Description
 * A BiomeTag identifies and encapsulates a specific biome within a game world.
 * Its flexible format allows specifying a world name followed by a biome key (e.g., `biome@myworld,minecraft:desert`).
 * If the world name is omitted, it defaults to the primary world. If the namespace for the biome key is not provided, `minecraft:` is assumed.
 * This tag dynamically interacts with the server's internal biome registries, ensuring full compatibility with custom biomes from datapacks or procedural world generation.
 *
 * @Usage
 * // Defines a BiomeTag for the 'desert' biome in the default world.
 * - define myBiome biome@minecraft:desert
 *
 * @Usage
 * // Defines a BiomeTag for a custom biome 'terra:warm_river' in the world 'space'.
 * - define customBiome biome@space,terra:warm_river
 */
public class BiomeTag implements AbstractTag, Adjustable {

    private static final String prefix = "biome";
    private final NamespacedKey biomeKey;
    private final World world;

    public static final TagProcessor<BiomeTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<BiomeTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private static final BiomeAdapter nms = NMSHandler.get().get(BiomeAdapter.class);

    public static void register() {
        BaseTagProcessor.registerBaseTag("biome", attr -> new BiomeTag(attr.getParam()));
        ObjectFetcher.registerFetcher(prefix, BiomeTag::new);

        /* @doc tag
         *
         * @Name name
         * @RawName <BiomeTag.name>
         * @Returns ElementTag
         * @Object BiomeTag
         * @NoArg
         *
         * @Implements BiomeTag.name
         *
         * @Description
         * Retrieves the full namespaced key of this biome, which uniquely identifies it across the server.
         * For example, it might return "minecraft:plains" for a vanilla biome or "myplugin:custom_forest" for a custom one.
         *
         * @Usage
         * // Displays the namespaced key of the biome at the player's current location.
         * - narrate "You are in the biome: <player.location.biome.name>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.biomeKey.toString()));

        /* @doc tag
         *
         * @Name world
         * @Syntax <BiomeTag.world>
         * @Returns ElementTag
         * @Object BiomeTag
         * @NoArg
         *
         * @Description
         * Returns the name of the world in which this biome is located.
         * This allows distinguishing between biomes with the same key across different worlds.
         *
         * @Usage
         * // Displays the name of the world where the biome is found.
         * - narrate "This biome is in world: <player.location.biome.world>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "world", (attr, obj) ->
                new ElementTag(obj.world.getName()));

        /* @doc tag
         *
         * @Name humidity
         * @Syntax <BiomeTag.humidity>
         * @Returns ElementTag(Decimal)
         * @Object BiomeTag
         * @NoArg
         *
         * @Implements BiomeTag.humidity
         *
         * @Description
         * Retrieves the base humidity level of the biome, which influences precipitation.
         * This value is a decimal number representing the biome's innate "downfall" characteristic.
         *
         * @Usage
         * // Displays the humidity level of the biome the player is currently in.
         * - narrate "Biome humidity: <player.location.biome.humidity>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "humidity", (attr, obj) ->
                new ElementTag(nms.getDownfall(obj.world, obj.biomeKey))).ignoreTest();

        /* @doc tag
         *
         * @Name baseTemperature
         * @Syntax <BiomeTag.baseTemperature>
         * @Returns ElementTag(Decimal)
         * @NoArg
         * @Object BiomeTag
         *
         * @Implements BiomeTag.base_temperature
         *
         * @Description
         * Provides the fundamental temperature value of this biome.
         * This is the default temperature before considering altitude or other environmental factors.
         *
         * @Usage
         * // Shows the base temperature of the biome at the player's location.
         * - narrate "Base biome temperature: <player.location.biome.baseTemperature>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "baseTemperature", (attr, obj) ->
                new ElementTag(nms.getTemperature(obj.world, obj.biomeKey))).ignoreTest();

        /* @doc tag
         *
         * @Name temperatureAt
         * @Syntax <BiomeTag.temperatureAt[<location>]>
         * @Returns ElementTag(Decimal)
         * @Object BiomeTag
         * @ArgRequired
         *
         * @Implements BiomeTag.temperature_at
         *
         * @Description
         * Calculates and returns the precise temperature at a given location within this biome.
         * This calculation takes into account factors like the Y-level (altitude) which can affect temperature.
         *
         * @Usage
         * // Displays the temperature at the player's feet.
         * - narrate "Temperature at your feet: <player.location.biome.temperatureAt[<player.location.below>]>"
         *
         * @Usage
         * // Checks if a specific location is cold enough for snow.
         * - if <biome[minecraft:forest].temperatureAt[<player.location>]> < 0.15:
         *   - narrate "It feels like snow could fall here."
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "temperatureAt", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            if (fetched instanceof LocationTag locTag) {
                return new ElementTag(nms.getTemperatureAt(locTag.getLocation()));
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name hasDownfall
         * @Syntax <BiomeTag.hasDownfall>
         * @Returns ElementTag(Boolean)
         * @Object BiomeTag
         * @NoArg
         *
         * @Implements BiomeTag.has_downfall
         *
         * @Description
         * Indicates whether this biome is capable of experiencing any form of precipitation, such as rain or snow.
         * Returns `true` if precipitation can occur, `false` otherwise.
         *
         * @Usage
         * // Checks if the current biome has any precipitation.
         * - if <player.location.biome.hasDownfall>:
         *   - narrate "This biome gets rain or snow."
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasDownfall", (attr, obj) ->
                new ElementTag(nms.hasDownfall(obj.world, obj.biomeKey))).ignoreTest();

        /* @doc tag
         *
         * @Name downfallAt[]
         * @Syntax <BiomeTag.downfallAt[<location>]>
         * @Returns ElementTag
         * @ArgRequired
         *
         * @Implements BiomeTag.downfallAt
         *
         * @Description
         * Determines the type of precipitation (RAIN, SNOW, or NONE) that would occur at a specified location within this biome.
         * This decision is based on the location's exact temperature and whether the biome generally supports precipitation.
         *
         * @Usage
         * // Reports the precipitation type at the player's current position.
         * - narrate "Current precipitation type: <player.location.biome.downfallAt[<player.location>]>"
         *
         * @Usage
         * // Spawns a snowball if the location is snowy.
         * - if <player.location.biome.downfallAt[<player.location>]> == SNOW:
         *   - spawn snowball <player.location>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "downfallAt", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            if (fetched instanceof LocationTag locTag) {
                if (!nms.hasDownfall(obj.world, obj.biomeKey)) return new ElementTag("NONE");
                float tempAtLoc = nms.getTemperatureAt(locTag.getLocation());
                return new ElementTag(tempAtLoc < 0.15f ? "SNOW" : "RAIN");
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name fogColor
         * @Syntax <BiomeTag.fogColor>
         * @Returns ColorTag
         * @NoArg
         * @Object BiomeTag
         *
         * @Implements BiomeTag.fog_color
         *
         * @Description
         * Retrieves the ColorTag representing the sky fog color specific to this biome.
         * This color influences the visual appearance of the sky and distant terrain.
         *
         * @Usage
         * // Displays the RGB components of the biome's fog color.
         * - narrate "Biome fog color: <player.location.biome.fogColor.rgb>"
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "fogColor", (attr, obj) -> nms.getFogColor(obj.world, obj.biomeKey)
                .map(colorInt -> new ColorTag((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, colorInt & 0xFF))
                .orElse(null)).ignoreTest();

        /* @doc tag
         *
         * @Name waterColor
         * @Syntax <BiomeTag.waterColor>
         * @Returns ColorTag
         * @NoArg
         * @Object BiomeTag
         *
         * @Description
         * Returns the ColorTag defining the visual color of water within this biome.
         * This can vary significantly between different biomes, affecting aesthetics.
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "waterColor", (attr, obj) -> nms.getWaterColor(obj.world, obj.biomeKey)
                .map(colorInt -> new ColorTag((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, colorInt & 0xFF))
                .orElse(null)).ignoreTest();

        /* @doc tag
         *
         * @Name waterFogColor
         * @Syntax <BiomeTag.waterFogColor>
         * @Returns ColorTag
         * @NoArg
         * @Object BiomeTag
         *
         * @Implements BiomeTag.water_fog_color
         *
         * @Description
         * Retrieves the ColorTag that specifies the color of the fog experienced when submerged underwater in this biome.
         * This visual effect contributes to the unique feel of different aquatic environments.
         *
         * @Usage
         * // Logs the underwater fog color of the biome.
         * - narrate "Underwater fog color: <player.location.biome.waterFogColor.rgb>"
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "waterFogColor", (attr, obj) -> nms.getWaterFogColor(obj.world, obj.biomeKey)
                .map(colorInt -> new ColorTag((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, colorInt & 0xFF))
                .orElse(null)).ignoreTest();

        /* @doc tag
         *
         * @Name foliageColor
         * @Syntax <BiomeTag.foliageColor>
         * @Returns ColorTag
         * @Nullable
         * @NoArg
         * @Object BiomeTag
         *
         * @Implements BiomeTag.foliage_color
         *
         * @Description
         * Provides the explicit foliage color override for this biome, if one is defined.
         * If no hardcoded override exists, the client calculates the color dynamically, and this tag will return `null`.
         *
         * @Usage
         * // Checks if the biome has a custom foliage color.
         * - if <player.location.biome.foliageColor||null> == null:
         *   - narrate "Foliage color is dynamically calculated."
         * - else:
         *   - narrate "Custom foliage color: <player.location.biome.foliageColor.rgb>"
         */
        TAG_PROCESSOR.registerTag(ColorTag.class, "foliageColor", (attr, obj) -> nms.getFoliageColor(obj.world, obj.biomeKey)
                .map(colorInt -> new ColorTag((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, colorInt & 0xFF))
                .orElse(null)).ignoreTest();

        /* @doc tag
         *
         * @Name spawnableEntities
         * @Syntax <BiomeTag.spawnableEntities[(<category>)]>
         * @Returns ListTag(ElementTag)
         * @Object BiomeTag
         *
         * @Implements BiomeTag.spawnable_entities
         *
         * @Description
         * Provides a list of entity types that are configured to spawn within this biome.
         * An optional category parameter can be specified to filter entities by their spawn category (e.g., MONSTER, CREATURE, AMBIENT, WATER_CREATURE, WATER_AMBIENT).
         * If no category is provided, it defaults to MONSTER.
         *
         * @Usage
         * // Lists all monster entities that can spawn in the player's current biome.
         * - narrate "Monsters that can spawn here: <player.location.biome.spawnableEntities[monster].join[, ]||None>"
         *
         * @Usage
         * // Checks if 'ZOMBIE' can spawn in the current biome.
         * - if <player.location.biome.spawnableEntities[monster].contains[ZOMBIE]>:
         *   - narrate "Watch out for zombies!"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "spawnableEntities", (attr, obj) -> {
            String categoryStr = attr.hasParam() ? attr.getParam().toUpperCase() : "MONSTER";
            try {
                BiomeAdapter.SpawnCategory category = BiomeAdapter.SpawnCategory.valueOf(categoryStr);
                List<EntityType> entities = nms.getSpawns(obj.world, obj.biomeKey, category);

                ListTag result = new ListTag();
                for (EntityType type : entities) {
                    result.addString(type.name());
                }
                return result;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).ignoreTest();

        /* @doc tag
         *
         * @Name attribute[]
         * @Syntax <BiomeTag.attribute[<attribute>]>
         * @Returns ObjectTag
         * @AvailableSince 1.21.11
         * @ArgRequired
         * @Object BiomeTag
         *
         * @Description
         * Retrieves the value of a dynamic attribute associated with this biome.
         * These attributes can be custom data points defined for a biome, and their type can vary (e.g., ElementTag, ColorTag).
         *
         * @Usage
         * // Checks if a biome has a 'gameplay/fast_lava' attribute set to true.
         * - if <biome[myplugin:magical_forest].attribute[gameplay/fast_lava]>:
         *   - narrate "This forest has a fast lava!"
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "attribute", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String attrName = attr.getParam();

            Object value = nms.getDynamicAttribute(obj.world, obj.biomeKey, attrName);

            if (value instanceof AbstractTag tag) {
                return tag;
            }

            return null;
        }).setAvailableSince("1.21.11");

        /* @doc tag
         *
         * @Name attributeNames
         * @Syntax <BiomeTag.attributeNames>
         * @Returns ListTag(ElementTag)
         * @AvailableSince 1.21.11
         * @Object BiomeTag
         * @NoArgs
         *
         * @Description
         * Provides a list of all dynamic attribute names that are registered and available for biomes.
         * This is useful for discovering what custom data can be queried or set on biomes.
         *
         * @Usage
         * // Displays all available dynamic attribute names for biomes.
         * - narrate "Available biome attributes: <biome[minecraft:plains].attributeNames.join[, ]>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "attributeNames", (attr, obj) ->
                new ListTag(nms.getAttributes())).setAvailableSince("1.21.11");

        /* @doc mechanism
         *
         * @Name setAttribute
         * @Input MapTag
         * @AvailableSince 1.21.11
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Description
         * Sets multiple dynamic attributes on the biome using a MapTag.
         * The keys of the MapTag represent the attribute names, and the values are the corresponding attribute values.
         * Supported value types include ColorTag (converted to RGB integer), ElementTag (converted to float, boolean, or string), or other suitable AbstractTag types.
         *
         * @Usage
         * // Sets the fast lava in the current biome
         * - adjust <player.location.biome> setAttribute:<map[gameplay/fast_lava=true]>
         */
        MECHANISM_PROCESSOR.registerMechanism("setAttribute", (biomeTag, value) -> {
            if (value instanceof MapTag map) {
                map.keySet().forEach(attrName -> {
                    AbstractTag val = map.getObject(attrName);
                    Object nmsValue = null;

                    if (val instanceof ColorTag c) nmsValue = c.asRGB();
                    else if (val instanceof ElementTag e) {
                        if (e.isDouble()) nmsValue = (float) e.asDouble();
                        else if (e.isBoolean()) nmsValue = e.asBoolean();
                        else nmsValue = e.asString();
                    }

                    if (nmsValue != null) {
                        nms.setDynamicAttribute(biomeTag.world, biomeTag.biomeKey, attrName, nmsValue);
                    }
                });
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name foliageColor
         * @Input ColorTag
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Implements BiomeTag.foliage_color
         *
         * @Description
         * Sets the hardcoded foliage color override for this biome.
         * The input should be a ColorTag, which will be converted to an RGB integer value.
         * Setting this will override any client-side dynamic foliage color calculations.
         *
         * @Usage
         * // Sets the foliage color of the biome at the player's location to green.
         * - adjust <player.location.biome> foliageColor:<color[#00ff00]>
         */
        MECHANISM_PROCESSOR.registerMechanism("foliageColor", (biomeTag, value) -> {
            if (value instanceof ColorTag color) {
                nms.setFoliageColor(biomeTag.world, biomeTag.biomeKey, color.asRGB());
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name fogColor
         * @Input ColorTag
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Implements BiomeTag.fog_color
         *
         * @Description
         * Modifies the sky fog color for this biome.
         * Provide a ColorTag as input to set the new color for the biome's atmospheric fog effect.
         *
         * @Usage
         * // Changes the fog color of the current biome to a blue.
         * - adjust <player.location.biome> fogColor:<color[#0000ff]>
         */
        MECHANISM_PROCESSOR.registerMechanism("fogColor", (biomeTag, value) -> {
            if (value instanceof ColorTag color) {
                nms.setFogColor(biomeTag.world, biomeTag.biomeKey, color.asRGB());
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name baseTemperature
         * @Input ElementTag(Decimal)
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Description
         * Adjusts the base temperature of this biome.
         * The input should be an ElementTag representing a decimal number, which will be used as the new default temperature.
         *
         * @Usage
         * // Sets the base temperature of the biome at the player's location to a colder value (0.0).
         * - adjust <player.location.biome> baseTemperature:0.0
         */
        MECHANISM_PROCESSOR.registerMechanism("baseTemperature", (biomeTag, value) -> {
            if (value instanceof ElementTag temp) {
                nms.setBaseTemperature(biomeTag.world, biomeTag.biomeKey, Float.parseFloat(temp.asString()));
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name humidity
         * @Input ElementTag(Decimal)
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Description
         * Modifies the base humidity level of this biome.
         * The input is an ElementTag (decimal) defining the new humidity value, which affects precipitation behavior.
         *
         * @Usage
         * // Increases the humidity of the current biome to a very wet level (0.9).
         * - adjust <player.location.biome> humidity:0.9
         */
        MECHANISM_PROCESSOR.registerMechanism("humidity", (biomeTag, value) -> {
            if (value instanceof ElementTag humidity) {
                nms.setHumidity(biomeTag.world, biomeTag.biomeKey, Float.parseFloat(humidity.asString()));
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name hasDownfall
         * @Input ElementTag(Boolean)
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Description
         * Toggles whether this biome experiences precipitation (rain or snow).
         * Provide a boolean ElementTag (true or false) to enable or disable downfall for the biome.
         *
         * @Usage
         * // Disables all precipitation for the biome where the player is located.
         * - adjust <player.location.biome> hasDownfall:false
         */
        MECHANISM_PROCESSOR.registerMechanism("hasDownfall", (biomeTag, value) -> {
            if (value instanceof ElementTag hasDownfall) {
                nms.setHasDownfall(biomeTag.world, biomeTag.biomeKey, hasDownfall.asBoolean());
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name waterColor
         * @Input ColorTag
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Description
         * Sets the visual color of water within this biome.
         * The input should be a ColorTag to specify the new desired water color.
         *
         * @Usage
         * // Changes the water color of the biome at the player's location to bright blue.
         * - adjust <player.location.biome> waterColor:<color[#00FFFF]>
         */
        MECHANISM_PROCESSOR.registerMechanism("waterColor", (biomeTag, value) -> {
            if (value instanceof ColorTag color) {
                nms.setWaterColor(biomeTag.world, biomeTag.biomeKey, color.asRGB());
            }
            return biomeTag;
        });

        /* @doc mechanism
         *
         * @Name waterFogColor
         * @Input ColorTag
         * @Tip Use {@link mechanism PlayerTag.reconfigure} to reload biomes for player
         *
         * @Implements BiomeTag.water_fog_color
         *
         * @Description
         * Modifies the color of the underwater fog effect for this biome.
         * A ColorTag input determines the new fog color visible when submerged in water.
         *
         * @Usage
         * // Sets the underwater fog color to #ff00ff.
         * - adjust <player.location.biome> waterFogColor:<color[#ff00ff]>
         */
        MECHANISM_PROCESSOR.registerMechanism("waterFogColor", (biomeTag, value) -> {
            if (value instanceof ColorTag color) {
                nms.setWaterFogColor(biomeTag.world, biomeTag.biomeKey, color.asRGB());
            }
            return biomeTag;
        });
    }

    public BiomeTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.world = null;
            this.biomeKey = null;
            return;
        }

        if (raw.toLowerCase().startsWith(prefix + "@")) {
            raw = raw.substring(6);
        }

        World parsedWorld = Bukkit.getWorlds().getFirst();

        int commaIndex = raw.indexOf(',');
        if (commaIndex != -1) {
            String worldStr = raw.substring(0, commaIndex);
            World tempWorld = Bukkit.getWorld(worldStr);
            if (tempWorld != null) parsedWorld = tempWorld;

            raw = raw.substring(commaIndex + 1);
        }

        if (!raw.contains(":")) {
            raw = "minecraft:" + raw;
        }

        String[] parts = raw.split(":");
        this.world = parsedWorld;
        this.biomeKey = new NamespacedKey(parts[0], parts[1]);
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + world.getName() + "," + biomeKey.toString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<BiomeTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "biome@minecraft:plains";
    }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @Nullable MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }

    @Override
    public Adjustable duplicate() {
        return this;
    }

    public World getWorld() {
        return world;
    }

    public NamespacedKey getBiomeKey() {
        return biomeKey;
    }
}
package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.BiomeTag;
import dev.corexinc.corex.environment.tags.world.RegionTag;
import dev.corexinc.corex.environment.utils.adapters.BiomeAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import dev.corexinc.corex.environment.tags.world.StructureTag;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.StructureType;
import org.jspecify.annotations.NonNull;

import java.io.File;

/* @doc object
 *
 * @Name ServerTag
 * @Prefix server
 * @IdentifyFormat server@
 * @Implements Flaggable
 *
 * @Description
 * Represents the Minecraft server itself.
 * Access it via the base tag "server" from anywhere in a script.
 *
 * ServerTag implements Flaggable, so you store persistent server-wide
 * data using the flag command with "server@" as the target.
 * Server flags persist to disk across restarts (SQLite).
 *
 * @Tags
 * <server.onlinePlayers>       - returns a ListTag(PlayerTag) of all currently online players.
 * <server.regions>             - returns a ListTag(RegionTag) of all active tick-regions on the server.
 * <server.loadedStructures>    - returns a ListTag(StructureTag) of all structures registered with the StructureManager.
 * <server.structureTypes>      - returns a ListTag(ElementTag) of all structure type keys known to the server.
 * <server.availableBiomes>     - returns a ListTag(BiomeTag) of all biome types available on the server.
 * <server.flag[name]>          - returns the value of a server flag.
 * <server.hasFlag[name]>       - returns an ElementTag(Boolean) indicating if the flag exists.
 * <server.flagExpiry[name]>    - returns a DurationTag of remaining time before the flag expires.
 *
 * @Usage
 * // Send a message to every online player.
 * - narrate targets:<server.onlinePlayers> "Server is restarting soon." per_player
 *
 * @Usage
 * // Store a server-wide value that persists across restarts.
 * - flag <server> maintenanceMode true
 *
 * @Usage
 * // Get average TPS across all active regions.
 * - narrate "Average TPS: <server.regions.parse[tps].average>"
 */
public class ServerTag implements AbstractTag, Flaggable {

    private static final String PREFIX = "server";
    public static final TagProcessor<ServerTag> PROCESSOR = new TagProcessor<>();

    private static final ServerTag INSTANCE = new ServerTag();

    private static SqlFlagTracker serverTracker;

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", attr -> INSTANCE);
        ObjectFetcher.registerFetcher(PREFIX, s -> INSTANCE);

        File dbFile = new File(Corex.getInstance().getDataFolder(), "serverFlags.db");
        serverTracker = new SqlFlagTracker(dbFile, "serverGlobal");

        PROCESSOR.registerTag(ElementTag.class, "ram", (attribute, serverTag) -> new ElementTag(Runtime.getRuntime().maxMemory()));

        /* @doc tag
         *
         * @Name regions
         * @RawName <server.regions>
         * @Object ServerTag
         * @ReturnType ListTag(RegionTag)
         * @NoArg
         * @Description
         * Returns a list of all unique tick-regions currently active across the entire server.
         * This includes all individual world threads and the global region pool.
         *
         * @Usage
         * // Calculate the average TPS across all active threads
         * - narrate "Average Server TPS: <server.regions.parse[tps].average>"
         */
        PROCESSOR.registerTag(ListTag.class, "regions", (attr, obj) -> {
            ListTag list = new ListTag();
            list.addObject(new RegionTag("global"));

            for (World w : Bukkit.getWorlds()) {
                if (!Corex.isFolia()) {
                    list.addObject(new RegionTag(w, 0, 0));
                } else {
                    for (RegionTag rt : RegionTag.FoliaSupport.getAllRegions(w)) {
                        list.addObject(rt);
                    }
                }
            }
            return list;
        });

        /* @doc tag
         *
         * @Name loadedStructures
         * @RawName <server.loadedStructures>
         * @Object ServerTag
         * @ReturnType ListTag(StructureTag)
         * @NoArg
         * @Description
         * Returns a list of all structures currently registered with the server's StructureManager.
         * This includes vanilla structures, DataPack structures, and any structures registered at runtime.
         *
         * @Usage
         * // List all registered structure keys
         * - narrate <server.loadedStructures.parse[key]>
         */
        PROCESSOR.registerTag(ListTag.class, "loadedStructures", (attr, obj) -> {
            ListTag list = new ListTag();
            for (var entry : Bukkit.getStructureManager().getStructures().entrySet()) {
                list.addObject(new StructureTag(entry.getValue(), entry.getKey()));
            }
            return list;
        });

        /* @doc tag
         *
         * @Name structureTypes
         * @RawName <server.structureTypes>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Description
         * Returns a list of all structure type keys known to the server, including
         * custom structures added by DataPacks.
         * These correspond to game-level structure types (e.g. `minecraft:village`,
         * `minecraft:stronghold`) from the structure registry — not NBT templates.
         *
         * @Usage
         * // Check if a custom datapack structure exists
         * - if <server.structureTypes.contains[myplugin:my_dungeon]>:
         *   - narrate "Dungeon structure type is registered!"
         */
        PROCESSOR.registerTag(ListTag.class, "structureTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            for (StructureType type : Registry.STRUCTURE_TYPE) {
                list.addObject(new ElementTag(type.getKey().toString()));
            }
            return list;
        });

        /* @doc tag
         *
         * @Name onlinePlayers
         * @RawName <server.onlinePlayers>
         * @Object ServerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Description
         * Returns a list of online players
         *
         * @Usage
         * // Sends a message to all online players on the server
         * - narrate targets:<server.onlinePlayers> "Hello <player.name>!" per_player
         */
        PROCESSOR.registerTag(ListTag.class, "onlinePlayers", (attr, obj) -> {
            ListTag listTag = new ListTag();
            for (Player player : Bukkit.getOnlinePlayers()) {
                listTag.addObject(new PlayerTag(player));
            }
            return listTag;
        });

        /* @doc tag
         *
         * @Name availableBiomes
         * @Syntax <server.availableBiomes>
         * @Returns ListTag(BiomeTag)
         *
         * @Implements WorldTag.biomes
         *
         * @Description
         * This tag returns a list of all recognized biome types currently available on the server.
         * Each item in the list is a BiomeTag, representing a distinct biome definition in the game world.
         * This is useful for iterating through all possible biomes or checking for specific biome types.
         *
         * @Usage
         * // Displays a list of all biomes registered on the server.
         * - narrate "All available biomes: <server.availableBiomes.parse[name].join[, ]>."
         *
         * @Usage
         * // Iterates through all available biomes and prints each biome's name.
         * - foreach <server.availableBiomes> as:biome:
         *   - narrate "Biome name: <[biome].name>"
         */
        PROCESSOR.registerTag(ListTag.class, "availableBiomes", (attr, obj) -> {
            BiomeAdapter adapter = NMSHandler.get().get(BiomeAdapter.class);
            ListTag list = new ListTag();

            World world = Bukkit.getWorlds().getFirst();

            for (NamespacedKey key : adapter.getAllBiomeKeys(world)) {
                list.addObject(new BiomeTag(world.getName() + "," + key.toString()));
            }
            return list;
        });
    }

    public ServerTag() {}

    @Override
    public SqlFlagTracker getFlagTracker() {
        return serverTracker;
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }
    @Override public @NonNull String identify() { return PREFIX + "@"; }
    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<ServerTag> getProcessor() { return PROCESSOR; }
    @Override public @NonNull String getTestValue() { return "server@"; }
}
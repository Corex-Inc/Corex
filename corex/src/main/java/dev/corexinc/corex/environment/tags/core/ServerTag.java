package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.BiomeTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import dev.corexinc.corex.environment.tags.world.RegionTag;
import dev.corexinc.corex.environment.utils.adapters.BiomeAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import dev.corexinc.corex.environment.tags.world.StructureTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.StructureType;
import org.jspecify.annotations.NonNull;

import java.io.File;

/* @doc object
 *
 * @Name ServerTag
 * @Prefix server
 * @IdentifyFormat server@
 * @Implements server
 *
 * @Description
 * Represents the Minecraft server itself.
 * Access it via the base tag "server" from anywhere in a script.
 *
 * ServerTag implements Flaggable, so you store persistent server-wide
 * data using the flag command with "server@" as the target.
 * Server flags persist to disk across restarts (SQLite).
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
 *
 * @Implements ServerTag
 */
public class ServerTag implements AbstractTag, Flaggable, Adjustable {

    private static final String PREFIX = "server";
    public static final TagProcessor<ServerTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<ServerTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private static final ServerTag INSTANCE = new ServerTag();

    private static SqlFlagTracker serverTracker;

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", attr -> INSTANCE);
        ObjectFetcher.registerFetcher(PREFIX, s -> INSTANCE);

        File dbFile = new File(Corex.getInstance().getDataFolder(), "serverFlags.db");
        serverTracker = new SqlFlagTracker(dbFile, "serverGlobal");

        TAG_PROCESSOR.registerTag(ElementTag.class, "ram", (attribute, serverTag) -> new ElementTag(Runtime.getRuntime().maxMemory()));

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
        TAG_PROCESSOR.registerTag(ListTag.class, "regions", (attr, obj) -> {
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
        TAG_PROCESSOR.registerTag(ListTag.class, "loadedStructures", (attr, obj) -> {
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
        TAG_PROCESSOR.registerTag(ListTag.class, "structureTypes", (attr, obj) -> {
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
        TAG_PROCESSOR.registerTag(ListTag.class, "onlinePlayers", (attr, obj) -> {
            ListTag listTag = new ListTag();
            for (Player player : Bukkit.getOnlinePlayers()) {
                listTag.addObject(new PlayerTag(player));
            }
            return listTag;
        });

        /* @doc tag
         *
         * @Name onlineOps
         * @RawName <server.onlineOps>
         * @Object ServerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Implements server.online_ops
         * @Description
         * Returns a list of all currently online players that are server operators.
         *
         * @Usage
         * // Message every online operator
         * - narrate targets:<server.onlineOps> "Staff meeting now." per_player
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "onlineOps", (attr, obj) -> {
            ListTag list = new ListTag();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp()) list.addObject(new PlayerTag(player));
            }
            return list;
        });

        /* @doc tag
         *
         * @Name offlinePlayers
         * @RawName <server.offlinePlayers>
         * @Object ServerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Implements server.offline_players
         * @Description
         * Returns a list of all players that have ever joined the server, including those
         * currently offline.
         *
         * @Usage
         * // Count everyone who has ever played
         * - narrate "Total known players: <server.offlinePlayers.size>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "offlinePlayers", (attr, obj) -> {
            ListTag list = new ListTag();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                list.addObject(new PlayerTag(player.getUniqueId()));
            }
            return list;
        });

        /* @doc tag
         *
         * @Name offlineOps
         * @RawName <server.offlineOps>
         * @Object ServerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Implements server.offline_ops
         * @Description
         * Returns a list of all players that are server operators, including those currently offline.
         *
         * @Usage
         * // List every operator's name
         * - narrate <server.offlineOps.parse[name].join[, ]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "offlineOps", (attr, obj) -> {
            ListTag list = new ListTag();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player.isOp()) list.addObject(new PlayerTag(player.getUniqueId()));
            }
            return list;
        });

        /* @doc tag
         *
         * @Name matchPlayer
         * @RawName <server.matchPlayer[<name>]>
         * @Object ServerTag
         * @ReturnType PlayerTag
         * @Implements server.match_player
         * @Description
         * Returns the online player that best matches the input name.
         * For example, in a group of 'bo', 'bob', and 'bobby': input 'bob' returns 'bob',
         * input 'bobb' returns 'bobby', and input 'b' returns 'bo'.
         *
         * @Usage
         * // Resolve a partial name to an online player
         * - narrate "Best match: <server.matchPlayer[bob].name>"
         */
        TAG_PROCESSOR.registerTag(PlayerTag.class, "matchPlayer", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String input = attr.getParam().toLowerCase();
            Player best = null;
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName().toLowerCase();
                if (name.equals(input)) { best = player; break; }
                if (name.contains(input) && (best == null || name.length() < best.getName().length())) {
                    best = player;
                }
            }
            return best == null ? null : new PlayerTag(best);
        });

        /* @doc tag
         *
         * @Name motd
         * @RawName <server.motd>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Implements server.motd
         * @Description
         * Returns the server's message of the day.
         *
         * @Usage
         * // Show the configured MOTD
         * - narrate "MOTD: <server.motd>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "motd", (attr, obj) -> new ElementTag(Bukkit.motd()));

        /* @doc tag
         *
         * @Name maxPlayers
         * @RawName <server.maxPlayers>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements server.max_players
         * @Description
         * Returns the maximum number of players the server allows online at once.
         *
         * @Usage
         * // Show current slots usage
         * - narrate "<server.onlinePlayers.size>/<server.maxPlayers>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxPlayers", (attr, obj) -> new ElementTag(Bukkit.getMaxPlayers()));

        /* @doc tag
         *
         * @Name materialTypes
         * @RawName <server.materialTypes>
         * @Object ServerTag
         * @ReturnType ListTag(MaterialTag)
         * @NoArg
         * @Implements server.material_types
         * @Description
         * Returns a list of all material types known to the server.
         *
         * @Usage
         * // Check whether a material exists
         * - if <server.materialTypes.contains[m@diamond_block]>:
         *   - narrate "Diamond blocks exist!"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "materialTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            for (Material material : Material.values()) list.addObject(new MaterialTag(material));
            return list;
        });

        /* @doc tag
         *
         * @Name particleTypes
         * @RawName <server.particleTypes>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.particle_types
         * @Description
         * Returns a list of all particle type names available on the server.
         *
         * @Usage
         * // List all particles
         * - narrate <server.particleTypes.join[, ]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "particleTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            for (Particle particle : Particle.values()) list.addString(particle.name().toLowerCase());
            return list;
        });

        /* @doc tag
         *
         * @Name entityTypes
         * @RawName <server.entityTypes>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.entity_types
         * @Description
         * Returns a list of all entity type names known to the server.
         *
         * @Usage
         * // Check whether an entity type exists
         * - if <server.entityTypes.contains[zombie]>:
         *   - narrate "Zombies exist!"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "entityTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            for (EntityType type : EntityType.values()) list.addString(type.name());
            return list;
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
        TAG_PROCESSOR.registerTag(ListTag.class, "availableBiomes", (attr, obj) -> {
            BiomeAdapter adapter = NMSHandler.get().get(BiomeAdapter.class);
            ListTag list = new ListTag();

            World world = Bukkit.getWorlds().getFirst();

            for (NamespacedKey key : adapter.getAllBiomeKeys(world)) {
                list.addObject(new BiomeTag(world.getName() + "," + key.toString()));
            }
            return list;
        });

        /* @doc mechanism
         *
         * @Name forceGarbageCollection
         * @Object ServerTag
         * @Description
         * Requests the Java Virtual Machine to run the Garbage Collector.
         *
         * @Warning
         * DO NOT USE THIS UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING!
         * IT IS HIGHLY DISCOURAGED TO EVER TOUCH THIS.
         * Manual garbage collection calls can cause severe lag spikes ("stop-the-world" pauses)
         * and interfere with Java's highly optimized automated memory management.
         * Usually, if you feel you need this, you have a memory leak that needs fixing instead.
         *
         * @Usage
         * // Forces a garbage collection cycle. (NOT RECOMMENDED)
         * - adjust <server> forceGarbageCollection
         */
        MECHANISM_PROCESSOR.registerMechanism("forceGarbageCollection", (obj, val) -> {
            System.gc();
            return obj;
        });

        /* @doc mechanism
         *
         * @Name haltJVM
         * @Object ServerTag
         * @Input ElementTag(Number)
         * @Description
         * Instantly terminates the Java Virtual Machine with the given exit code.
         *
         * @Warning
         * EXTREME DANGER! DATA CORRUPTION IMMINENT!
         * This bypasses Bukkit.shutdown() entirely. Worlds will NOT be saved, player data
         * will NOT be synchronized, and database connections will be abruptly severed.
         * Using this will almost certainly result in corrupted chunks or data loss.
         *
         * @Usage
         * // Instantly kills the server process with exit code 0
         * - adjust <server> haltJVM:0
         */
        MECHANISM_PROCESSOR.registerMechanism("haltJVM", (obj, val) -> {
            int exitCode = 0;
            if (val instanceof ElementTag el && el.isDouble()) {
                exitCode = (int) el.asDouble();
            }
            Runtime.getRuntime().halt(exitCode);
            return obj.duplicate();
        });

        /* @doc mechanism
         *
         * @Name freezeThread
         * @Object ServerTag
         * @Input ElementTag(Number)
         * @Description
         * Forces the current executing thread to sleep (freeze) for the specified number of milliseconds.
         *
         * @Warning
         * SERVER FREEZE / WATCHDOG CRASH!
         * If executed on the main server thread or a Folia region thread, this will completely
         * halt all game logic (TPS drops to 0) for the duration. If the duration is too long,
         * the Server Watchdog will assume the server has crashed and will forcibly terminate it.
         *
         * @Usage
         * // Freezes the current thread for 5 seconds (5000 ms)
         * - adjust <server> freezeThread:5000
         */
        MECHANISM_PROCESSOR.registerMechanism("freezeThread", (obj, val) -> {
            if (val instanceof ElementTag el && el.isDouble()) {
                long ms = (long) el.asDouble();
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return obj.duplicate();
        });

        /* @doc mechanism
         *
         * @Name forceCrash
         * @Object ServerTag
         * @Input ElementTag (String message)
         * @Description
         * Throws a fatal java.lang.Error on the current thread, intentionally simulating a catastrophic JVM failure.
         *
         * @Warning
         * THIS WILL CRASH THE SERVER OR THREAD!
         * This skips standard exception handling and throws an Error, which will likely
         * trigger an emergency shutdown or crash report depending on the server software.
         *
         * @Usage
         * - adjust <server> forceCrash:"Intentional script crash"
         */
        MECHANISM_PROCESSOR.registerMechanism("forceCrash", (obj, val) -> {
            String reason = val != null ? val.identify() : "Forced crash via CorexVM";
            throw new Error("Corex Forced JVM Crash: " + reason);
        });
    }

    public ServerTag() {}

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return serverTracker;
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<ServerTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "server@";
    }

    @Override
    public @NonNull Adjustable duplicate() {
        return INSTANCE;
    }

    @Override
    public @NonNull AbstractTag applyMechanism(@NonNull String mechanism, @NonNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NonNull MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }
}
package dev.corexinc.corex.environment.tags.core;

import com.destroystokyo.paper.profile.PlayerProfile;
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
import dev.corexinc.corex.environment.tags.world.*;
import dev.corexinc.corex.environment.utils.adapters.BiomeAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import io.papermc.paper.ban.BanListType;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

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
@SuppressWarnings("deprecation")
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

        /* @doc tag
         *
         * @Name availableProcessors
         * @RawName <server.availableProcessors>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.available_processors
         * @Description
         * Returns the number of logical CPU processors available to the JVM.
         * This is the value reported by Runtime.availableProcessors() and reflects
         * the current CPU affinity mask of the process, not the raw hardware count.
         *
         * @Usage
         * // Check CPU count for diagnostics
         * - narrate "CPU threads available: <server.availableProcessors>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "availableProcessors", (attr, obj) ->
                new ElementTag(Runtime.getRuntime().availableProcessors()));

        /* @doc tag
         *
         * @Name javaVersion
         * @RawName <server.javaVersion>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Implements utils.java_version
         * @Description
         * Returns the Java version string the server is currently running on,
         * as reported by the "java.version" system property (e.g. "21.0.3").
         *
         * @Usage
         * // Log which Java version is in use
         * - narrate "Running Java: <server.javaVersion>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "javaVersion", (attr, obj) ->
                new ElementTag(System.getProperty("java.version", "unknown")));

        /* @doc tag
         *
         * @Name ramMax
         * @RawName <server.ramMax>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.ram_max
         * @Description
         * Returns the maximum heap memory in bytes the JVM is allowed to claim from the OS (-Xmx).
         * This is a hard ceiling — the JVM will throw OutOfMemoryError before exceeding this.
         * Use this to understand the configured memory budget, not how much is actually in use.
         *
         * @Usage
         * // Show configured memory limit in MB
         * - narrate "Max heap: <server.ramMax.div[1048576]> MB"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ramMax", (attr, obj) ->
                new ElementTag(Runtime.getRuntime().maxMemory()));

        /* @doc tag
         *
         * @Name ramAllocated
         * @RawName <server.ramAllocated>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.ram_allocated
         * @Description
         * Returns the amount of heap memory in bytes currently committed (allocated) from the OS
         * to the JVM. This is the live working set, bounded between the initial heap size (-Xms)
         * and the max heap (-Xmx). Not all of this memory is actively used — see ramUsage.
         *
         * @Usage
         * // Show committed heap in MB
         * - narrate "Committed heap: <server.ramAllocated.div[1048576]> MB"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ramAllocated", (attr, obj) ->
                new ElementTag(Runtime.getRuntime().totalMemory()));

        /* @doc tag
         *
         * @Name ramUsage
         * @RawName <server.ramUsage>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.ram_usage
         * @Description
         * Returns the amount of heap memory in bytes that is actively occupied by live objects
         * (committed heap minus the unused portion within it). This is the most accurate measure
         * of how much memory the server is actually consuming right now.
         * Formula: totalMemory() - freeMemory()
         *
         * @Usage
         * // Show live heap usage in MB
         * - narrate "Used heap: <server.ramUsage.div[1048576]> MB"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ramUsage", (attr, obj) -> {
            Runtime r = Runtime.getRuntime();
            return new ElementTag(r.totalMemory() - r.freeMemory());
        });

        /* @doc tag
         *
         * @Name ramFree
         * @RawName <server.ramFree>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.ram_free
         * @Description
         * Returns the total free heap headroom in bytes — the amount of memory that can still be
         * allocated before hitting the -Xmx limit. This accounts for both the unused portion of
         * the currently committed heap and the uncommitted heap space the JVM can still claim.
         * Formula: maxMemory() - totalMemory() + freeMemory()
         *
         * @Usage
         * // Warn if free headroom drops below 512 MB
         * - if <server.ramFree.div[1048576].is[less_than].to[512]>:
         *   - narrate "Warning: low memory headroom!"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ramFree", (attr, obj) -> {
            Runtime r = Runtime.getRuntime();
            return new ElementTag(r.maxMemory() - r.totalMemory() + r.freeMemory());
        });

        /* @doc tag
         *
         * @Name diskTotal
         * @RawName <server.diskTotal>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.disk_total
         * @Description
         * Returns the total capacity of the partition that holds the server's working directory,
         * in bytes.
         *
         * @Usage
         * // Show partition size in GB
         * - narrate "Disk total: <server.diskTotal.div[1073741824]> GB"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "diskTotal", (attr, obj) ->
                new ElementTag(new File(".").getTotalSpace()));

        /* @doc tag
         *
         * @Name diskFree
         * @RawName <server.diskFree>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Implements utils.disk_free
         * @Description
         * Returns the usable free space in bytes on the partition holding the server's working
         * directory. Uses getUsableSpace() rather than getFreeSpace() so that space reserved
         * for the OS root is excluded — this is the actual amount the server process can write.
         *
         * @Usage
         * // Warn if disk space is critically low
         * - if <server.diskFree.div[1073741824].is[less_than].to[5]>:
         *   - narrate "Critical: less than 5 GB disk space remaining!"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "diskFree", (attr, obj) ->
                new ElementTag(new File(".").getUsableSpace()));

        /* @doc tag
         *
         * @Name diskUsage
         * @RawName <server.diskUsage>
         * @Object ServerTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Implements utils.disk_usage
         * @Description
         * Returns the disk usage ratio of the server's partition as a decimal between 0.0 and 1.0.
         * Calculated as (total - usable) / total. Multiply by 100 for a percentage.
         * Returns 0.0 if the partition total cannot be determined.
         *
         * @Usage
         * // Show disk usage as a percentage
         * - narrate "Disk usage: <server.diskUsage.mul[100].round>%"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "diskUsage", (attr, obj) -> {
            File root = new File(".");
            long total = root.getTotalSpace();
            if (total == 0) return new ElementTag(0.0);
            long used = total - root.getUsableSpace();
            return new ElementTag((double) used / total);
        });

        /* @doc tag
         *
         * @Name hasFile
         * @RawName <server.hasFile[<path>]>
         * @Object ServerTag
         * @ReturnType ElementTag(Boolean)
         * @Implements utils.has_file
         * @Description
         * Returns true if the given path exists on the filesystem (file or directory).
         * Relative paths are resolved from the server's working directory.
         *
         * @Usage
         * // Check if a config file exists before reading it
         * - if <server.hasFile[plugins/MyPlugin/config.yml]>:
         *   - narrate "Config file found."
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasFile", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ElementTag(new File(attr.getParam()).exists());
        });

        /* @doc tag
         *
         * @Name listFiles
         * @RawName <server.listFiles[<path>]>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @Implements utils.list_files
         * @Description
         * Returns a list of file and directory names directly inside the given path.
         * Only lists the immediate children (non-recursive). Relative paths are resolved
         * from the server's working directory. Returns an empty list if the path does not
         * exist, is not a directory, or cannot be read.
         *
         * @Usage
         * // List all files in the plugins folder
         * - foreach <server.listFiles[plugins]> as:entry:
         *   - narrate "  <[entry]>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "listFiles", (attr, obj) -> {
            ListTag list = new ListTag();
            if (!attr.hasParam()) return list;
            File dir = new File(attr.getParam());
            File[] entries = dir.listFiles();
            if (entries == null) return list;
            for (File entry : entries) list.addString(entry.getName());
            return list;
        });

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

        /* @doc tag
         *
         * @Name advancementTypes
         * @RawName <server.advancementTypes>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.advancement_types
         * @Description
         * Returns a list of all advancement keys registered on the server, including
         * vanilla advancements and those added by datapacks.
         * Each entry is the full namespaced key (e.g. "minecraft:story/mine_stone").
         *
         * @Usage
         * // List every advancement key
         * - narrate <server.advancementTypes.join[, ]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "advancementTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) list.addString(it.next().getKey().toString());
            return list;
        });

        /* @doc tag
         *
         * @Name artTypes
         * @RawName <server.artTypes>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.art_types
         * @Description
         * Returns a list of all painting art variant keys registered on the server.
         * Each entry is the full namespaced key (e.g. "minecraft:kebab").
         *
         * @Usage
         * // Pick a random painting type
         * - narrate <server.artTypes.random>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "artTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            for (Art art : Registry.ART) list.addString(art.assetId().toString());
            return list;
        });

        /* @doc tag
         *
         * @Name bannedAddresses
         * @RawName <server.bannedAddresses>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.banned_addresses
         * @Description
         * Returns a list of all IP addresses currently on the IP ban list.
         *
         * @Usage
         * // Check how many IPs are banned
         * - narrate "Banned IPs: <server.bannedAddresses.size>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "bannedAddresses", (attr, obj) -> {
            ListTag list = new ListTag();
            IpBanList banList = Bukkit.getBanList(BanListType.IP);

            for (BanEntry<? super InetAddress> entry : banList.getEntries()) {
                InetAddress ip = (InetAddress) entry.getBanTarget();
                list.addString(ip.getHostAddress());
            }

            return list;
        });

        /* @doc tag
         *
         * @Name bannedPlayers
         * @RawName <server.bannedPlayers>
         * @Object ServerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Implements server.banned_players
         * @Description
         * Returns a list of all players currently on the profile ban list.
         *
         * @Usage
         * // List every banned player's name
         * - narrate <server.bannedPlayers.parse[name].join[, ]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "bannedPlayers", (attr, obj) -> {
            ListTag list = new ListTag();
            ProfileBanList banList = Bukkit.getBanList(BanListType.PROFILE);
            for (BanEntry<? super PlayerProfile> entry : banList.getEntries()) {
                if (entry.getBanTarget() instanceof PlayerProfile profile) {
                    UUID uuid = profile.getId();
                    if (uuid != null) {
                        list.addObject(new PlayerTag(uuid));
                    }
                }
            }
            return list;
        });

        /* @doc tag
         *
         * @Name banInfo
         * @RawName <server.banInfo[<address>]>
         * @Object ServerTag
         * @ReturnType MapTag
         * @Implements server.ban_info
         * @Description
         * Returns a MapTag describing the IP ban entry for the given address, or null if the
         * address is not banned. The map contains the following keys:
         *   reason       - the ban reason string (empty string if none was given)
         *   source       - the name of the admin or system that issued the ban
         *   createdTime  - ISO-8601 timestamp of when the ban was created
         *   expirationTime - ISO-8601 timestamp of when the ban expires, or "never"
         *
         * @Usage
         * // Show why an address was banned
         * - narrate "Banned by <server.banInfo[1.2.3.4].get[source]>: <server.banInfo[1.2.3.4].get[reason]>"
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "banInfo", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            IpBanList banList = Bukkit.getBanList(BanListType.IP);
            BanEntry<InetAddress> entry;
            try {
                entry = banList.getBanEntry(InetAddress.getByName(attr.getParam()));
            }
            catch (UnknownHostException ex) {
                return null;
            }
            if (entry == null) return null;
            MapTag map = new MapTag();
            map.putObject("reason", new ElementTag(entry.getReason() != null ? entry.getReason() : ""));
            map.putObject("source", new ElementTag(entry.getSource()));
            map.putObject("createdTime", new ElementTag(entry.getCreated().toInstant().toString()));
            Date expiration = entry.getExpiration();
            map.putObject("expirationTime", new ElementTag(expiration != null ? expiration.toInstant().toString() : "never"));
            return map;
        });

        /* @doc tag
         *
         * @Name platform
         * @RawName <server.platform>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Implements server.bukkit_name
         * @Description
         * Returns the name of the server platform (e.g. "Paper", "Folia", "Spigot").
         *
         * @Usage
         * - narrate "Running on: <server.platform>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "platform", (attr, obj) -> new ElementTag(Bukkit.getName()));

        /* @doc tag
         *
         * @Name platformVersion
         * @RawName <server.platformVersion>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Implements server.bukkit_version
         * @Description
         * Returns the Bukkit API version string of the current platform
         * (e.g. "1.21.1-R0.1-SNAPSHOT").
         *
         * @Usage
         * - narrate "Bukkit API: <server.platformVersion>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "platformVersion", (attr, obj) -> new ElementTag(Bukkit.getBukkitVersion()));

        /* @doc tag
         *
         * @Name version
         * @RawName <server.version>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Implements server.version
         * @Description
         * Returns the Minecraft protocol version string the server is running
         * (e.g. "1.21.1").
         *
         * @Usage
         * - narrate "MC version: <server.version>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "version", (attr, obj) -> new ElementTag(Bukkit.getMinecraftVersion()));

        /* @doc tag
         *
         * @Name corexVersion
         * @RawName <server.corexVersion>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Implements server.denizen_version
         * @Description
         * Returns the currently loaded CoreX plugin version string.
         *
         * @Usage
         * - narrate "CoreX v<server.corexVersion>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "corexVersion", (attr, obj) ->
                new ElementTag(Corex.getInstance().getPluginMeta().getVersion()));

        /* @doc tag
         *
         * @Name damageCauses
         * @RawName <server.damageCauses>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.damage_causes
         * @Description
         * Returns a list of all damage cause names known to the server
         * (e.g. "fall", "fire", "projectile"). Names are lower-cased.
         *
         * @Usage
         * // Check whether a damage cause exists
         * - if <server.damageCauses.contains[magic]>:
         *   - narrate "Magic damage is supported."
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "damageCauses", (attr, obj) -> {
            ListTag list = new ListTag();
            for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values())
                list.addString(cause.name().toLowerCase());
            return list;
        });

        /* @doc tag
         *
         * @Name effectTypes
         * @RawName <server.effectTypes>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.effect_types
         * @Description
         * Returns a list of all potion effect type keys registered on the server
         * (e.g. "minecraft:speed", "minecraft:strength").
         *
         * @Usage
         * // Check whether a custom effect is registered
         * - if <server.effectTypes.contains[minecraft:speed]>:
         *   - narrate "Speed effect is available."
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "effectTypes", (attr, obj) -> {
            ListTag list = new ListTag();
            for (PotionEffectType type : Registry.EFFECT) list.addString(type.getKey().toString());
            return list;
        });

        /* @doc tag
         *
         * @Name enchantments
         * @RawName <server.enchantments>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.enchantments
         * @Description
         * Returns a list of all enchantment keys registered on the server,
         * including any added by datapacks (e.g. "minecraft:sharpness").
         *
         * @Usage
         * // List every enchantment
         * - narrate <server.enchantments.join[, ]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "enchantments", (attr, obj) -> {
            ListTag list = new ListTag();
            for (Enchantment ench : Registry.ENCHANTMENT) list.addString(ench.getKey().toString());
            return list;
        });

        /* @doc tag
         *
         * @Name gamerules
         * @RawName <server.gamerules>
         * @Object ServerTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Implements server.gamerules
         * @Description
         * Returns a list of all gamerule names known to the server, derived from the first
         * loaded world (gamerule keys are identical across all worlds on the same server version).
         * Includes vanilla gamerules and any added by datapacks.
         *
         * @Usage
         * // Check if a gamerule exists
         * - if <server.gamerules.contains[doFireTick]>:
         *   - narrate "Fire spreading is a gamerule."
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "gamerules", (attr, obj) -> {
            ListTag list = new ListTag();
            java.util.List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) return list;
            for (String rule : worlds.getFirst().getGameRules()) list.addString(rule);
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
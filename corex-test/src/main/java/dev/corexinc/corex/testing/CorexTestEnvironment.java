package dev.corexinc.corex.testing;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.ItemDisplayMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.UUID;

/**
 * Boots a MockBukkit server with Corex loaded on top, ready for tag and formatter tests.
 * <p>
 * The fixtures below are the ones Corex's own tags declare as their {@code testValue} — a world
 * named {@code world}, a player, an item display and a running queue. An addon testing its tags
 * needs them too, because its tags will chain into built-in ones.
 *
 * <pre>{@code
 * @BeforeAll
 * static void setup() {
 *     registry = CorexTestEnvironment.bootstrap();
 * }
 * }</pre>
 */
public final class CorexTestEnvironment {

    /** UUID of the player added by {@link #bootstrap()}. */
    public static final UUID PLAYER_UUID = UUID.fromString("465876c1-2a15-4fc0-9f0b-97de13aa46f1");

    /** UUID of the item display entity added by {@link #bootstrap()}. */
    public static final UUID ITEM_DISPLAY_UUID = UUID.fromString("cf5d1e35-fb92-476e-9c96-bc932ca0b0cb");

    private static Corex plugin;
    private static ServerMock server;

    private CorexTestEnvironment() {}

    /**
     * Starts the mock server (or reuses a running one) and returns Corex's registry.
     * Safe to call from several test classes in the same JVM.
     */
    public static CorexRegistry bootstrap() {
        if (plugin != null && MockBukkit.isMocked()) {
            return plugin.getRegistry();
        }

        // A previous class may have unmocked; drop the stale handle before starting over.
        plugin = null;
        server = MockBukkit.isMocked() ? MockBukkit.getMock() : MockBukkit.mock();

        server.addSimpleWorld("world");
        World world = server.getWorld("world");
        if (world != null) {
            world.getBlockAt(1, 1, 1).setType(Material.STONE);
        }

        PlayerMock player = new PlayerMock(server, "TestPlayer", PLAYER_UUID);
        server.addPlayer(player);
        player.setLocation(new Location(world, 10.5, 64.0, 10.5, 90f, 0f));

        server.registerEntity(new ItemDisplayMock(server, ITEM_DISPLAY_UUID));

        ScriptQueue queue = new ScriptQueue("test_queue", new Instruction[0], false, null, null);
        queue.setKeepAlive(true);
        queue.define("testDef", new ElementTag("Yeah!"));
        queue.start();

        plugin = MockBukkit.load(Corex.class);
        return plugin.getRegistry();
    }

    /** The loaded Corex instance. Call {@link #bootstrap()} first. */
    public static Corex getPlugin() {
        if (plugin == null) throw new IllegalStateException("bootstrap() has not been called");
        return plugin;
    }

    /** The running mock server. Call {@link #bootstrap()} first. */
    public static ServerMock getServer() {
        if (server == null) throw new IllegalStateException("bootstrap() has not been called");
        return server;
    }

    /** Tears the mock server down. Optional — leaving it running lets later test classes reuse it. */
    public static void shutdown() {
        if (MockBukkit.isMocked()) MockBukkit.unmock();
        plugin = null;
        server = null;
    }
}

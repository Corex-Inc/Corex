package dev.corexinc.corex.velocity.environment.commands.player;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/* @doc command
 *
 * @Name ResourcePack
 * @Syntax resourcepack [set/add/remove] (id:<id>) (url:<url>) (hash:<hash>) (prompt:<text>) (targets:<player>|...) (forced)
 * @RequiredArgs 1
 * @MaxArgs 7
 * @Waitable
 * @Modules VELOCITY
 * @ShortDescription Offers a resource pack to players from the proxy.
 *
 * @Implements ResourcePack
 *
 * @Description
 * Offers a resource pack to players, or takes one back. 'add' stacks another pack on top of what
 * the player already has, 'set' clears their packs first, and 'remove' takes one away by id, or
 * all of them when no id is given.
 *
 * Only 'url:' is required, and it has to be a link the client can reach on its own, the proxy does
 * not serve the file. 'hash:' is the 40 character SHA-1 of the archive: without it the client
 * downloads the pack again every single time, so leave it out only while testing. 'id:' is a UUID
 * used to remove that pack later, and defaults to one derived from the url.
 *
 * With no 'targets:' the pack goes to the queue's linked player. Run it waitable with '~' to hold
 * the queue until every target has accepted, declined or failed.
 *
 * A backend server can offer packs of its own. On 1.20.3 and up those stack with these instead of
 * replacing them.
 *
 * @Usage
 * // Offer a pack to the linked player.
 * - resourcepack add url:https://example.com/pack.zip hash:5d41402abc4b2a76b9719d911017c592aa1b2c3d
 *
 * @Usage
 * // Force a pack on everyone entering the arena, then wait until they are done with it.
 * - ~resourcepack set url:<[arenaPackUrl]> hash:<[arenaPackHash]> prompt:"Required for the arena" forced targets:<[players]>
 * - narrate "Everyone is ready." targets:<[players]>
 *
 * @Usage
 * // Take every pack back off the player.
 * - resourcepack remove
 */
public class ResourcePackCommand implements AbstractCommand {

    private static final Map<UUID, List<Runnable>> WAIT_CALLBACKS = new ConcurrentHashMap<>();
    private static boolean listenerRegistered = false;

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ElementTag.class)
            .optionalPrefix("id", ElementTag.class)
            .optionalPrefix("url", ElementTag.class)
            .optionalPrefix("hash", ElementTag.class)
            .optionalPrefix("prompt", AbstractTag.class)
            .optionalPrefix("targets", ListTag.class)
            .optionalFlag("forced")
            .build();

    public ResourcePackCommand() {
        if (!listenerRegistered) {
            CorexVelocity plugin = CorexVelocity.getInstance();
            plugin.getServer().getEventManager().register(plugin, this);
            listenerRegistered = true;
        }
    }

    @Subscribe
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!event.getStatus().isIntermediate()) {
            triggerCallbacks(event.getPlayer().getUniqueId());
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        triggerCallbacks(event.getPlayer().getUniqueId());
    }

    private void triggerCallbacks(UUID uuid) {
        List<Runnable> callbacks = WAIT_CALLBACKS.remove(uuid);
        if (callbacks != null) {
            callbacks.forEach(Runnable::run);
        }
    }

    @Override
    public @NonNull String getName() {
        return "resourcepack";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[set/add/remove] (id:<id>) (url:<url>) (hash:<hash>) (prompt:<text>) (targets:<player>|...) (forced)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 7;
    }

    @Override
    public boolean setCanBeWaitable() {
        return true;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        String action = args.linear(0).identify();

        ElementTag rawId = args.prefix("id");
        String id = rawId == null ? null : rawId.identify();

        ElementTag rawUrl = args.prefix("url");
        String url = rawUrl == null ? null : rawUrl.identify();

        ElementTag rawHash = args.prefix("hash");
        String hash = rawHash == null ? null : rawHash.identify();

        AbstractTag promptTag = args.prefix("prompt");
        boolean forced = args.flag("forced");

        List<Player> targetPlayers = getTargets(queue, args.prefix("targets"));
        if (targetPlayers.isEmpty()) return;

        if (action.equalsIgnoreCase("remove")) {
            UUID packUUID = id == null ? null : parseUUID(id);

            Debugger.report(queue, instruction,
                    "Action", action,
                    "ID", packUUID,
                    "Targets", targetPlayers.size()
            );

            for (Player player : targetPlayers) {
                if (packUUID == null) player.clearResourcePacks();
                else player.removeResourcePacks(packUUID);
            }
            return;
        }

        if (url == null || hash == null) {
            Debugger.echoError(queue, "Both 'url:' and 'hash:' are required for add/set.");
            return;
        }

        byte[] hashBytes = parseHash(hash);
        if (hashBytes == null) {
            Debugger.echoError(queue, "Invalid resource pack hash (must be a 40-character hex string): " + hash);
            return;
        }

        UUID packUUID = id != null ? parseUUID(id) : UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));

        ResourcePackInfo.Builder builder = CorexVelocity.getInstance().getServer()
                .createResourcePackBuilder(url)
                .setId(packUUID)
                .setShouldForce(forced);

        builder.setHash(hashBytes);
        if (promptTag != null) builder.setPrompt(promptTag.asComponent());

        ResourcePackInfo packInfo = builder.build();

        Debugger.report(queue, instruction,
                "Action", action,
                "ID", packUUID,
                "URL", url,
                "Waitable", instruction.isWaitable
        );

        if (instruction.isWaitable) {
            queue.pause();
            AtomicInteger pending = new AtomicInteger(targetPlayers.size());
            Runnable onComplete = () -> {
                if (pending.decrementAndGet() == 0) SchedulerAdapter.get().run(queue::resume);
            };

            for (Player player : targetPlayers) {
                WAIT_CALLBACKS.computeIfAbsent(player.getUniqueId(), key -> new CopyOnWriteArrayList<>()).add(onComplete);
            }
        }

        for (Player player : targetPlayers) {
            if (action.equalsIgnoreCase("set")) player.clearResourcePacks();
            player.sendResourcePackOffer(packInfo);
        }
    }

    private List<Player> getTargets(ScriptQueue queue, ListTag targets) {
        List<Player> targetPlayers = new ArrayList<>();

        if (targets != null && !targets.isEmpty()) {
            targets.filter(PlayerTag.class, queue).forEach(p -> {
                Player player = p.getPlayer().orElse(null);
                if (player != null && player.isActive()) targetPlayers.add(player);
            });
        } else {
            PlayerTag queuePlayer = (PlayerTag) queue.getPlayer();
            if (queuePlayer != null && queuePlayer.isOnline()) {
                queuePlayer.getPlayer().ifPresent(targetPlayers::add);
            }
        }

        if (targetPlayers.isEmpty()) Debugger.echoError(queue, "No online targets found.");
        return targetPlayers;
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        }
        catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] parseHash(String hash) {
        if (hash.length() != 40) return null;
        byte[] bytes = new byte[20];
        for (int index = 0; index < 20; index++) {
            int high = Character.digit(hash.charAt(index * 2), 16);
            int low = Character.digit(hash.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) return null;
            bytes[index] = (byte) ((high << 4) | low);
        }
        return bytes;
    }
}

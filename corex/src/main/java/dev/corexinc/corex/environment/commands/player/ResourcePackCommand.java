package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.Corex;
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
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourcePackCommand implements AbstractCommand, Listener {

    private static final Map<UUID, List<Runnable>> WAIT_CALLBACKS = new ConcurrentHashMap<>();
    private static boolean listenerRegistered = false;

    public ResourcePackCommand() {
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
            listenerRegistered = true;
        }
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED ||
                status == PlayerResourcePackStatusEvent.Status.DECLINED ||
                status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD ||
                status == PlayerResourcePackStatusEvent.Status.DISCARDED) {

            triggerCallbacks(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
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

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ElementTag.class)
            .optionalPrefix("id", ElementTag.class)
            .optionalPrefix("hash", ElementTag.class)
            .optionalPrefix("prompt", ElementTag.class)
            .optionalPrefix("targets", ListTag.class)
            .optionalFlag("forced")
            .build();

    @Override
    public @NonNull String getSyntax() {
        return "[set/add/remove] (id:<id>) (url:<url>) (hash:<hash>) (prompt:<text>) (targets:<player>|...) (forced)";
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
            UUID packUUID = id != null ? parseUUID(id) : null;
            for (Player player : targetPlayers) {
                ((BukkitSchedulerAdapter) SchedulerAdapter.get()).runEntity(player, () -> {
                    if (packUUID == null) player.removeResourcePacks();
                    else player.removeResourcePack(packUUID);
                });
            }
            return;
        }

        if (url == null || hash == null) {
            Debugger.echoError(queue, "Both 'url:' and 'hash:' are required for add/set.");
            return;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            Debugger.echoError(queue, "Invalid URL format: " + url);
            return;
        }

        UUID packUUID = id != null ? parseUUID(id) : UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
        Component promptComp = promptTag != null ? promptTag.asComponent() : Component.empty();

        ResourcePackInfo packInfo;
        try {
            packInfo = ResourcePackInfo.resourcePackInfo(packUUID, uri, hash);
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid resource pack hash (must be a 40-character hex string): " + hash);
            return;
        }

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .required(forced)
                .prompt(promptComp)
                .build();

        Debugger.report(queue, instruction,
                "Action", action,
                "ID", packUUID,
                "URL", rawUrl,
                "Waitable", instruction.isWaitable
        );

        if (instruction.isWaitable) {
            queue.pause();
            AtomicInteger pending = new AtomicInteger(targetPlayers.size());
            Runnable onComplete = () -> {
                if (pending.decrementAndGet() == 0) SchedulerAdapter.get().run(queue::resume);
            };

            for (Player player : targetPlayers) {
                WAIT_CALLBACKS.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArrayList<>()).add(onComplete);
            }
        }

        for (Player player : targetPlayers) {
            ((BukkitSchedulerAdapter) SchedulerAdapter.get()).runEntity(player, () -> {
                if (action.equalsIgnoreCase("set")) player.removeResourcePacks();
                player.sendResourcePacks(request);
            });
        }
    }

    private List<Player> getTargets(ScriptQueue queue, ListTag targets) {
        List<Player> targetPlayers = new ArrayList<>();

        if (targets != null && targets.isEmpty()) {
            targets.filter(PlayerTag.class, queue).forEach(p -> {
                Player player = p.getPlayer();
                if (player != null && player.isOnline()) targetPlayers.add(player);
            });
        } else {
            PlayerTag queuePlayer = (PlayerTag) queue.getPlayer();
            if (queuePlayer != null && queuePlayer.getPlayer() != null && queuePlayer.getPlayer().isOnline()) {
                targetPlayers.add(queuePlayer.getPlayer());
            }
        }

        if (targetPlayers.isEmpty()) Debugger.echoError(queue, "No online targets found.");
        return targetPlayers;
    }

    private UUID parseUUID(String id) {
        try { return UUID.fromString(id); }
        catch (IllegalArgumentException ex) { return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)); }
    }
}
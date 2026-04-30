package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
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
import java.util.concurrent.atomic.AtomicInteger;

public class ResourcePackCommand implements AbstractCommand, Listener {

    private static final Map<UUID, List<Runnable>> WAIT_CALLBACKS = new ConcurrentHashMap<>();

    public ResourcePackCommand() {
        Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
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

    @Override public @NonNull String getName() { return "resourcepack"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 7; }
    @Override public boolean setCanBeWaitable() { return true; }

    @Override
    public @NonNull String getSyntax() {
        return "[set/add/remove] (id:<id>) (url:<url>) (hash:<hash>) (prompt:<text>) (targets:<player>|...) (forced)";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String actionRaw = instruction.getLinear(0, queue);
        if (actionRaw == null) {
            Debugger.echoError(queue, "Action must be specified: set, add, or remove.");
            return;
        }
        String action = actionRaw.toLowerCase();

        String idRaw = instruction.getPrefix("id", queue);
        String urlRaw = instruction.getPrefix("url", queue);
        String hashRaw = instruction.getPrefix("hash", queue);
        AbstractTag promptTag = instruction.getPrefixObject("prompt", queue);
        boolean forced = instruction.hasFlag("forced");

        List<Player> targetPlayers = getTargets(queue, instruction);
        if (targetPlayers.isEmpty()) return;

        if (action.equals("remove")) {
            UUID packUUID = idRaw != null ? parseUUID(idRaw) : null;
            for (Player player : targetPlayers) {
                SchedulerAdapter.runEntity(player, () -> {
                    if (packUUID == null) player.removeResourcePacks();
                    else player.removeResourcePack(packUUID);
                });
            }
            return;
        }

        if (urlRaw == null || hashRaw == null) {
            Debugger.echoError(queue, "Both 'url:' and 'hash:' are required for add/set.");
            return;
        }

        URI uri;
        try {
            uri = new URI(urlRaw);
        } catch (URISyntaxException e) {
            Debugger.echoError(queue, "Invalid URL format: " + urlRaw);
            return;
        }

        UUID packUUID = idRaw != null ? parseUUID(idRaw) : UUID.nameUUIDFromBytes(urlRaw.getBytes(StandardCharsets.UTF_8));
        Component promptComp = promptTag != null ? promptTag.asComponent() : Component.empty();

        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo(packUUID, uri, hashRaw);

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .required(forced)
                .prompt(promptComp)
                .build();

        Debugger.report(queue, instruction,
                "Action", action,
                "ID", packUUID,
                "URL", urlRaw,
                "Waitable", instruction.isWaitable
        );

        if (instruction.isWaitable) {
            queue.pause();
            AtomicInteger pending = new AtomicInteger(targetPlayers.size());
            Runnable onComplete = () -> {
                if (pending.decrementAndGet() == 0) SchedulerAdapter.run(queue::resume);
            };
            for (Player player : targetPlayers) {
                WAIT_CALLBACKS.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(onComplete);
            }
        }

        for (Player player : targetPlayers) {
            SchedulerAdapter.runEntity(player, () -> {
                if (action.equals("set")) player.removeResourcePacks();
                player.sendResourcePacks(request);
            });
        }
    }

    private List<Player> getTargets(ScriptQueue queue, Instruction instruction) {
        String targetsRaw = instruction.getPrefix("targets", queue);
        List<Player> targetPlayers = new ArrayList<>();
        if (targetsRaw != null) {
            new ListTag(targetsRaw).filter(PlayerTag.class, queue).forEach(p -> {
                if (p.getPlayer() != null && p.getPlayer().isOnline()) targetPlayers.add(p.getPlayer());
            });
        } else if (queue.getPlayer() != null && queue.getPlayer().getOfflinePlayer().isOnline()) {
            targetPlayers.add(queue.getPlayer().getPlayer());
        }
        if (targetPlayers.isEmpty()) Debugger.echoError(queue, "No online targets found.");
        return targetPlayers;
    }

    private UUID parseUUID(String id) {
        try { return UUID.fromString(id); }
        catch (IllegalArgumentException ex) { return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)); }
    }
}
package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "teleport";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>|...] [<location>] (cause:<TeleportCause>)";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction entry) {
        int argCount = entry.linearArgs.length;

        List<Location> targets = new ArrayList<>();
        String rawLocation;

        if (argCount >= 2) {
            String rawTarget = entry.getLinear(0, queue);
            Object resolved = ObjectFetcher.pickObject(rawTarget);

            switch (resolved) {
                case PlayerTag playerTag -> targets.add(playerTag.getPlayer().getLocation());
                case EntityTag entityTag -> targets.add(entityTag.getEntity().getLocation());
                case ListTag listTag -> {
                    for (var tag : listTag.getList()) {
                        if (tag instanceof EntityTag entityTag) {
                            targets.add(entityTag.getEntity().getLocation());
                        } else if (tag instanceof PlayerTag playerTag) {
                            targets.add(playerTag.getPlayer().getLocation());
                        }
                    }
                }
                case null, default -> targets.add(queue.getPlayer().getPlayer().getLocation());
            }

            rawLocation = entry.getLinear(1, queue);
        } else {
            targets.add(queue.getPlayer().getPlayer().getLocation());
            rawLocation = entry.getLinear(0, queue);
        }

        if (targets.isEmpty()) {
            Debugger.echoError("TeleportCommand: could not resolve any target entities.");
            return;
        }

        LocationTag location = new LocationTag(rawLocation);
        if (location.getLocation().getWorld() == null) {
            Debugger.echoError("TeleportCommand: invalid or world-less location: " + rawLocation);
            return;
        }

        TeleportCause cause = TeleportCause.PLUGIN;
        String rawCause = entry.getPrefix("cause", queue);
        if (rawCause != null) {
            try {
                cause = TeleportCause.valueOf(rawCause.toUpperCase());
            } catch (IllegalArgumentException e) {
                Debugger.echoError("TeleportCommand: unknown TeleportCause '" + rawCause + "', defaulting to PLUGIN.");
            }
        }

        final TeleportCause finalCause = cause;
        final Location destination = location.getLocation();

        for (Location target : targets) {
            target.getWorld().getEntities().stream()
                    .filter(e -> e.getLocation().equals(target))
                    .forEach(e -> e.teleportAsync(destination, finalCause));
        }
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }
}
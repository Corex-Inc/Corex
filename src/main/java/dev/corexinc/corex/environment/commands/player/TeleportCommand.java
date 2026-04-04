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
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 3;
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
            Debugger.error(queue, getName() + " could not resolve any target entities.", 0);
            return;
        }

        LocationTag location = new LocationTag(rawLocation);
        if (location.getLocation().getWorld() == null) {
            Debugger.error(queue, getName() + " invalid or world-less location: " + rawLocation, 0);
            return;
        }

        TeleportCause cause = TeleportCause.PLUGIN;
        String rawCause = entry.getPrefix("cause", queue);
        if (rawCause != null) {
            try {
                cause = TeleportCause.valueOf(rawCause.toUpperCase());
            } catch (IllegalArgumentException e) {
                Debugger.error(queue, getName() + " unknown TeleportCause '" + rawCause + "', defaulting to PLUGIN.", e, 0);
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
}
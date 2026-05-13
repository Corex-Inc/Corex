package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import it.unimi.dsi.fastutil.objects.ObjectHeaps;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @[command]
 *
 * @Name Teleport
 * @Syntax teleport [<entity>|...] [<location>] (cause:<TeleportCause>)
 * @RequiredArgs 1
 * @MaxArgs 3
 * @ShortDescription Teleports the entity(s) to a new location.
 *
 * @Implements Teleport
 *
 * @Description
 * Teleports the entity or entities to the new location.
 * Entities can be teleported between worlds using this command.
 * You may optionally specify a teleport cause for player entities, allowing proper teleport event handling. When not specified, this is "PLUGIN". See {@link javadoc https://jd.papermc.io/paper/org/bukkit/event/player/PlayerTeleportEvent.TeleportCause.html} for causes.
 *
 * Instead of a valid entity, an offline player may also be used.
 *
 *
 * @Tags
 * <EntityTag.location>
 *
 * @Usage
 * // Use to teleport a player to the location their cursor is pointing at.
 * - teleport <player> <player.cursorOn>
 *
 * @Usage
 * // Use to teleport a player high above.
 * - teleport <player> <player.location.above[200]>
 *
 * @Usage
 * // Use to teleport to a random online player.
 * - teleport <player> <server.onlinePlayers.random.location>
 *
 * @Usage
 * // Use to teleport all players to your location.
 * - teleport <server.onlinePlayers> <player.location>
 *
 * @Usage
 * // Use to teleport a player to some location, and inform events that it was caused by a nether portal.
 * - teleport <player> <server.flag[netherHubLocation]> cause:nether_portal
 */
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
        return 2;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        int argCount = instruction.linearArgs.length;

        List<Entity> entitiesToTeleport = new ArrayList<>();
        String rawLocationInput;

        if (argCount >= 2) {
            AbstractTag targetTag = instruction.getLinearObject(0, queue);
            rawLocationInput = instruction.getLinear(1, queue);

            if (targetTag instanceof ListTag list) {
                entitiesToTeleport.addAll(list.filter(PlayerTag.class, queue).stream().map(PlayerTag::getPlayer).toList());
                entitiesToTeleport.addAll(list.filter(EntityTag.class, queue).stream().map(EntityTag::getEntity).toList());
            } else if (targetTag instanceof PlayerTag p) {
                entitiesToTeleport.add(p.getPlayer());
            } else if (targetTag instanceof EntityTag e) {
                entitiesToTeleport.add(e.getEntity());
            }
        } else {
            PlayerTag pt = (PlayerTag) queue.getPlayer();
            if (pt != null) {
                entitiesToTeleport.add(pt.getPlayer());
            }
            rawLocationInput = instruction.getLinear(0, queue);
        }

        if (entitiesToTeleport.isEmpty()) {
            Debugger.echoError(queue, getName() + " could not resolve any target entities.");
            return;
        }

        LocationTag locationTag = new LocationTag(rawLocationInput);
        Location destination = locationTag.getLocation();

        if (destination.getWorld() == null) {
            Debugger.echoError(queue, getName() + " invalid or world-less location: " + rawLocationInput);
            return;
        }

        TeleportCause cause = TeleportCause.PLUGIN;
        String rawCause = instruction.getPrefix("cause", queue);
        if (rawCause != null) {
            try {
                cause = TeleportCause.valueOf(rawCause.toUpperCase());
            } catch (IllegalArgumentException e) {
                Debugger.echoError(queue, getName() + " unknown TeleportCause '" + rawCause + "', using PLUGIN.");
            }
        }

        Debugger.report(queue, instruction,
                "Destination", locationTag.identify(),
                "Cause", cause.name(),
                "Targets_Count", entitiesToTeleport.size()
        );

        for (Entity entity : entitiesToTeleport) {
            if (entity != null && entity.isValid()) {
                entity.teleportAsync(destination, cause);
            }
        }
    }
}
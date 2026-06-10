package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import dev.corexinc.corex.environment.utils.scripts.CommandHelper;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Spawn
 * @Syntax spawn [<entity>|...] (<location>) (persistent) (reason:<reason>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Spawns one or more entities into the world.
 *
 * @Implements Spawn
 *
 * @Description
 * Spawns one or more entities at the given location, or at the linked player's location when none is given.
 *
 * Each input EntityTag is spawned as a brand new entity.
 * A blueprint (e.g. 'e@item_display' or 'e@zombie[maxHealth=100;name=okak]') is spawned by its type and mechanisms.
 * An already spawned entity is cloned - its current data is copied onto a fresh entity.
 *
 * Specify (persistent) to force the spawned entities to be saved with the world.
 * Specify (reason:<reason>) to control the CreatureSpawnEvent spawn reason. When not specified, this is "CUSTOM".
 *
 * The spawn command is ~waitable, and supports the 'save:' argument to store the spawned entities.
 * The saved result is a MapTag with 'entity' (when a single entity is spawned) or 'entities' (a list, when several are spawned).
 *
 * @Usage
 * // Spawn a zombie at the player's location.
 * - spawn zombie
 *
 * @Usage
 * // Spawn a named, high-health zombie blueprint at a location.
 * - spawn <entity[zombie].with[maxHealth=100;name=okak]> <player.location>
 *
 * @Usage
 * // Spawn an entity and keep a reference to it.
 * - spawn creeper <player.location> save:mob
 * - narrate <[mob].get[entity].name>
 */
public class SpawnCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "spawn";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>|...] (<location>) (persistent) (reason:<reason>)";
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
    public boolean setCanBeWaitable() {
        return true;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        List<EntityTag> blueprints = resolveBlueprints(instruction.getLinearObject(0, queue));
        if (blueprints.isEmpty()) {
            Debugger.echoError(queue, "Spawn command could not resolve any entities to spawn.");
            return;
        }

        Location location;
        if (instruction.linearArgs.length >= 2) {
            location = new LocationTag(instruction.getLinear(1, queue)).getLocation();
        } else {
            PlayerTag player = (PlayerTag) queue.getPlayer();
            location = player != null ? player.getPlayer().getLocation() : null;
        }

        if (location == null || location.getWorld() == null) {
            Debugger.echoError(queue, "Spawn command requires a valid location, or a linked player.");
            return;
        }

        CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.CUSTOM;
        String rawReason = instruction.getPrefix("reason", queue);
        if (rawReason != null) {
            try {
                reason = CreatureSpawnEvent.SpawnReason.valueOf(rawReason.toUpperCase());
            } catch (IllegalArgumentException e) {
                Debugger.echoError(queue, "Spawn command unknown reason '" + rawReason + "', using CUSTOM.");
            }
        }

        boolean persistent = instruction.getPrefix("persistent", queue) != null;

        Debugger.report(queue, instruction,
                "Entities", new ListTag(blueprints).identify(),
                "Location", new LocationTag(location).identify(),
                "Reason", reason.name(),
                "Persistent", String.valueOf(persistent)
        );

        CreatureSpawnEvent.SpawnReason spawnReason = reason;
        if (instruction.isWaitable) queue.pause();

        SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(location), () -> {
            try {
                ListTag spawned = new ListTag();
                for (EntityTag blueprint : blueprints) {
                    EntityTag result = blueprint.spawn(location, spawnReason, persistent);
                    if (result != null) {
                        spawned.addObject(result);
                    } else {
                        Debugger.echoError(queue, "Spawn command failed to spawn '" + blueprint.identify() + "'.");
                    }
                }

                MapTag result = new MapTag();
                List<AbstractTag> list = spawned.getList();
                if (list.size() == 1) {
                    result.putObject("entity", list.getFirst());
                } else {
                    result.putObject("entities", spawned);
                }
                CommandHelper.saveResult(queue, instruction, result);
            } finally {
                if (instruction.isWaitable) queue.resume();
            }
        });
    }

    private List<EntityTag> resolveBlueprints(AbstractTag argument) {
        List<EntityTag> blueprints = new ArrayList<>();
        switch (argument) {
            case EntityTag entity -> blueprints.add(entity);
            case ListTag list -> list.getList().forEach(tag ->
                    blueprints.add(tag instanceof EntityTag entity ? entity : new EntityTag(tag.identify())));
            case null -> {}
            default -> blueprints.add(new EntityTag(argument.identify()));
        }
        return blueprints;
    }
}

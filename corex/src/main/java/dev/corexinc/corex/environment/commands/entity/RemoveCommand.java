package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.WorldTag;
import dev.corexinc.corex.environment.tags.world.area.AbstractAreaObject;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/* @doc command
 *
 * @Name Remove
 * @Syntax remove [<entity>|...] (world:<world>) (in:<area>)
 * @RequiredArgs 1
 * @MaxArgs 1
 * @ShortDescription Removes entities from the world.
 *
 * @Description
 * Removes one or more entities. Players are never removed.
 *
 * Spawned entities (e.g. 'e@<uuid>' or '<player.target>') are removed directly.
 * A type or matcher (e.g. 'zombie', 'item', '*') is mass-removal. With no scope it removes matching entities across ALL worlds.
 * Matchers follow the entity advanced-matcher rules (type name, uuid, '*'/'any', wildcards, custom name contains).
 *
 * Specify (world:<world>) to limit mass-removal to that world.
 * Specify (in:<area>) to limit mass-removal to an area object (CuboidTag, EllipsoidTag, PolygonTag).
 * 'in:' covers radius-style removal too - build the area from a location, e.g. '<player.location.toEllipsoid[20]>'.
 *
 * Removal is region-safe (each entity is removed on its own region thread).
 *
 * @Usage
 * // Remove a specific entity.
 * - remove <player.target>
 *
 * @Usage
 * // Remove every dropped item and zombie in a world.
 * - remove item|zombie world:myworld
 *
 * @Usage
 * // Clear all mobs inside an arena cuboid.
 * - remove * in:<[arenaCuboid]>
 */
public class RemoveCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "remove";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>|...] (world:<world>) (in:<area>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        AbstractTag arg = instruction.getLinearObject(0, queue);
        if (arg == null) {
            Debugger.echoError(queue, "Remove requires an entity, a list, or a type matcher!");
            return;
        }

        List<Entity> specific = new ArrayList<>();
        List<String> matchers = new ArrayList<>();
        collect(arg, specific, matchers);

        AbstractAreaObject area = null;
        String inRaw = instruction.getPrefix("in", queue);
        if (inRaw != null) {
            if (ObjectFetcher.pickObject(inRaw) instanceof AbstractAreaObject resolved) {
                area = resolved;
            } else {
                Debugger.echoError(queue, "Remove: 'in:" + inRaw + "' is not a valid area object!");
                return;
            }
        }

        World world = null;
        String worldRaw = instruction.getPrefix("world", queue);
        if (worldRaw != null) {
            world = new WorldTag(worldRaw).getWorld();
            if (world == null) {
                Debugger.echoError(queue, "Remove: unknown world '" + worldRaw + "'!");
                return;
            }
        }

        Set<Entity> targets = new LinkedHashSet<>(specific);

        if (!matchers.isEmpty()) {
            List<World> worlds;
            if (area != null) {
                World areaWorld = area.getCenter().getLocation().getWorld();
                worlds = areaWorld != null ? List.of(areaWorld) : List.of();
            } else if (world != null) {
                worlds = List.of(world);
            } else {
                worlds = Bukkit.getWorlds();
            }

            for (World scope : worlds) {
                for (Entity entity : scope.getEntities()) {
                    if (area != null && !area.contains(entity.getLocation())) continue;
                    if (matchesAny(entity, matchers)) targets.add(entity);
                }
            }
        }

        Debugger.report(queue, instruction,
                "Targets", String.valueOf(targets.size()),
                "Matchers", matchers.isEmpty() ? "None" : String.join(",", matchers),
                "Scope", matchers.isEmpty() ? "direct" : (area != null ? "area" : (world != null ? world.getName() : "all worlds"))
        );

        for (Entity entity : targets) {
            if (entity == null || entity.isDead() || entity instanceof Player) continue;
            ((BukkitSchedulerAdapter) SchedulerAdapter.get()).runEntity(entity, entity::remove);
        }
    }

    private void collect(AbstractTag arg, List<Entity> specific, List<String> matchers) {
        switch (arg) {
            case EntityTag entity -> addEntity(entity, specific, matchers);
            case ListTag list -> list.getList().forEach(tag -> {
                if (tag instanceof EntityTag entity) addEntity(entity, specific, matchers);
                else matchers.add(tag.identify());
            });
            default -> matchers.add(arg.identify());
        }
    }

    private void addEntity(EntityTag tag, List<Entity> specific, List<String> matchers) {
        if (tag.getEntity() != null) {
            specific.add(tag.getEntity());
        } else {
            matchers.add(tag.getEntityType() != null ? tag.getEntityType().name() : "*");
        }
    }

    private boolean matchesAny(Entity entity, List<String> matchers) {
        EntityTag wrapper = new EntityTag(entity);
        for (String matcher : matchers) {
            if (wrapper.tryAdvancedMatcher(matcher)) return true;
        }
        return false;
    }
}

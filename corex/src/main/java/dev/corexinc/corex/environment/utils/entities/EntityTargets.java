package dev.corexinc.corex.environment.utils.entities;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns command arguments into entity targets, keeping real and fake entities apart.
 *
 * <p>Commands that only work through the Bukkit API take {@link Resolved#real()} and
 * report the fakes via {@link #warnUnsupported}. Commands backed by
 * {@link LiveEntityView} take {@link Resolved#views()} and treat both the same.</p>
 */
public final class EntityTargets {

    /**
     * @param real  entities the server actually tracks, already filtered for validity
     * @param views a write-side view per target, real and fake alike, in input order
     * @param fakes fake entities among the targets, for reporting when unsupported
     */
    public record Resolved(List<Entity> real, List<LiveEntityView> views, List<EntityTag> fakes) {

        public boolean isEmpty() {
            return views.isEmpty();
        }

        public boolean hasFakes() {
            return !fakes.isEmpty();
        }
    }

    private EntityTargets() {}

    /**
     * Collects targets from a tag that may be a single entity, a player, or a list of either.
     */
    public static Resolved resolve(AbstractTag argument, ScriptQueue queue) {
        List<Entity> real = new ArrayList<>();
        List<LiveEntityView> views = new ArrayList<>();
        List<EntityTag> fakes = new ArrayList<>();

        collect(argument, queue, real, views, fakes);
        return new Resolved(real, views, fakes);
    }

    private static void collect(AbstractTag argument, ScriptQueue queue,
                                List<Entity> real, List<LiveEntityView> views, List<EntityTag> fakes) {
        switch (argument) {
            case null -> {}
            case ListTag list -> {
                for (AbstractTag item : list.getList()) collect(item, queue, real, views, fakes);
            }
            case PlayerTag playerTag -> {
                Entity player = playerTag.getPlayer();
                if (player != null && player.isValid()) {
                    real.add(player);
                    views.add(new BukkitEntityView(player));
                }
            }
            case EntityTag entityTag -> {
                if (entityTag.isFake()) {
                    fakes.add(entityTag);
                    views.add(entityTag.getView());
                    return;
                }
                Entity entity = entityTag.getEntity();
                if (entity != null && entity.isValid()) {
                    real.add(entity);
                    views.add(entityTag.getView());
                }
            }
            default -> {}
        }
    }

    /**
     * Reports fake targets handed to a command that cannot act on them.
     */
    public static void warnUnsupported(ScriptQueue queue, String commandName, Resolved resolved) {
        warn(queue, commandName, resolved.fakes().size());
    }

    /**
     * Scans a raw command argument for fake entities and reports them as unsupported.
     * <p>
     * For commands that resolve targets their own way and would otherwise drop fakes
     * without a word.
     */
    public static void warnIfFake(ScriptQueue queue, String commandName, AbstractTag argument) {
        warn(queue, commandName, countFakes(argument));
    }

    private static void warn(ScriptQueue queue, String commandName, int fakes) {
        if (fakes == 0) return;
        Debugger.echoError(queue, "'<yellow>" + commandName + "</yellow>' does not support fake entities - skipped "
                + fakes + " of them.");
        Debugger.echoError(queue, "Fake entities exist only as packets, so they have no server-side state to change.");
    }

    private static int countFakes(AbstractTag argument) {
        return switch (argument) {
            case null -> 0;
            case EntityTag entityTag -> entityTag.isFake() ? 1 : 0;
            case ListTag list -> {
                int total = 0;
                for (AbstractTag item : list.getList()) total += countFakes(item);
                yield total;
            }
            default -> 0;
        };
    }
}

package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Mount
 * @Syntax mount [<entity>|...] (cancel)
 * @RequiredArgs 1
 * @MaxArgs 1
 * @Aliases ride
 * @ShortDescription Chains a list of entities on top of each other, or dismounts them.
 *
 * @Description
 * Takes a list of entities and mounts each one on top of the next, forming a chain
 * (an "entity train"). The first entity in the list is the topmost passenger, the second
 * rides it on top of the third, and so on — the last entity in the list is the bottommost vehicle.
 *
 * Requires at least 2 valid entities in the list to form a chain.
 *
 * Use "cancel" to instead dismount every entity in the list from whatever it's currently riding,
 * rather than mounting them.
 *
 * @Usage
 * // Player mounts (rides) the entity.
 * - mount <player>|<[mount.entity]>
 *
 * @Usage
 * // Chain three entities: player rides skeleton, skeleton rides zombie.
 * - mount <player>|<[skeleton]>|<[zombie]>
 *
 * @Usage
 * // Dismount every entity in the list.
 * - mount <player>|<[mount.entity]> cancel
 */
public class MountCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "mount";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("ride");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>|...] (cancel)";
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

        AbstractTag listArg = instruction.getLinearObject(0, queue);
        if (listArg == null) {
            Debugger.echoError(queue, "Mount: entity list cannot be null.");
            return;
        }

        ListTag list = listArg instanceof ListTag lt ? lt : new ListTag(listArg.identify());

        List<Entity> entities = new ArrayList<>();
        for (AbstractTag item : list.getList()) {
            Entity entity = resolveEntity(item);
            if (entity == null || entity.isDead()) {
                Debugger.echoError(queue, "Mount: '" + item.identify() + "' is not a valid, living entity (got: "
                        + item.getPrefix() + "), skipping.");
                continue;
            }
            entities.add(entity);
        }

        boolean cancel = instruction.hasFlag("cancel");

        if (cancel) {
            for (Entity entity : entities) entity.leaveVehicle();
            Debugger.report(queue, instruction,
                    "List",    list.identify(),
                    "Entities", String.valueOf(entities.size()),
                    "Action",  "cancel"
            );
            return;
        }

        if (entities.size() < 2) {
            Debugger.echoError(queue, "Mount: need at least 2 valid entities to chain, only "
                    + entities.size() + " resolved from " + list.size() + " list entries.");
            return;
        }

        int mounted = 0;
        for (int i = 0; i < entities.size() - 1; i++) {
            Entity passenger = entities.get(i);
            Entity vehicle = entities.get(i + 1);
            if (vehicle.addPassenger(passenger)) {
                mounted++;
            } else {
                Debugger.echoError(queue, "Mount: Paper rejected mounting '" + passenger.getUniqueId()
                        + "' onto '" + vehicle.getUniqueId() + "' (dead entity or incompatible type?).");
            }
        }

        Debugger.report(queue, instruction,
                "List",    list.identify(),
                "Chain",   String.valueOf(entities.size()),
                "Mounted", mounted + "/" + (entities.size() - 1),
                "Action",  "mount"
        );
    }

    private static Entity resolveEntity(AbstractTag tag) {
        if (tag instanceof EntityTag entityTag) {
            return entityTag.getEntity();
        }
        if (tag instanceof PlayerTag playerTag) {
            return playerTag.getPlayer();
        }
        return null;
    }
}
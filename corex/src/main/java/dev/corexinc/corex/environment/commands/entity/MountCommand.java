package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.CommandExecutionException;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import me.tofaa.entitylib.wrapper.WrapperEntity;
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
        return 2;
    }

    public void run(ScriptQueue queue, ListTag list, boolean cancel) {
        List<Rider> riders = new ArrayList<>();
        for (AbstractTag item : list.getList()) {
            Rider rider = resolveRider(item);
            if (rider == null) {
                Debugger.echoError(queue, "Mount: '" + item.identify() + "' is not a valid, living entity (got: "
                        + item.getPrefix() + "), skipping.");
                continue;
            }
            riders.add(rider);
        }

        if (cancel) {
            for (Rider rider : riders) rider.dismount();
            Debugger.detail(queue, "Dismounted", riders.size());
            return;
        }

        if (riders.size() < 2) {
            throw new CommandExecutionException("Mount: need at least 2 valid entities to chain, only "
                    + riders.size() + " resolved from " + list.size() + " list entries.");
        }

        int mounted = 0;
        for (int i = 0; i < riders.size() - 1; i++) {
            Rider passenger = riders.get(i);
            Rider vehicle = riders.get(i + 1);

            if (vehicle.fake() != null) {
                vehicle.fake().addPassenger(passenger.entityId());
                mounted++;
                continue;
            }

            if (passenger.fake() != null) {
                Debugger.echoError(queue, "Mount: a real entity cannot carry the fake entity '"
                        + passenger.fake().getUuid() + "' - only a fake vehicle can, since the server "
                        + "has no entity to attach. Skipped.");
                continue;
            }

            if (vehicle.real().addPassenger(passenger.real())) {
                mounted++;
            } else {
                Debugger.echoError(queue, "Mount: Paper rejected mounting '" + passenger.real().getUniqueId()
                        + "' onto '" + vehicle.real().getUniqueId() + "' (dead entity or incompatible type?).");
            }
        }

        Debugger.detail(queue, "Mounted", mounted + "/" + (riders.size() - 1));
    }

    /** One link of a mount chain: either a server entity or a packet-only one. */
    private record Rider(Entity real, WrapperEntity fake) {

        int entityId() {
            return real != null ? real.getEntityId() : fake.getEntityId();
        }

        void dismount() {
            if (real != null) real.leaveVehicle();
            else {
                WrapperEntity riding = fake.getRiding();
                if (riding != null) riding.removePassenger(fake);
            }
        }
    }

    private static Rider resolveRider(AbstractTag tag) {
        if (tag instanceof EntityTag entityTag && entityTag.isFake()) {
            return new Rider(null, entityTag.getFakeEntity());
        }
        Entity entity = resolveEntity(tag);
        if (entity == null || entity.isDead()) return null;
        return new Rider(entity, null);
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
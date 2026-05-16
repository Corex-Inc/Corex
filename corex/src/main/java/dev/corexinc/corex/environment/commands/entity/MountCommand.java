package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Mount
 * @Syntax mount [<vehicle>] (remove|eject) (passenger:<entity>)
 * @RequiredArgs 1
 * @MaxArgs 3
 * @Aliases ride
 * @ShortDescription Mounts one entity onto another, or ejects passengers.
 *
 * @Description
 * Makes a passenger entity ride on top of a vehicle entity.
 * The first argument is always the vehicle.
 *
 * Use "passenger:" to specify who rides.
 * Use "remove" to dismount a specific passenger from the vehicle.
 * Use "eject" to eject every passenger from the vehicle at once.
 *
 * @Usage
 * // Mount a player onto an entity.
 * - mount <[horse]> passenger:<[player]>
 *
 * @Usage
 * // Dismount a specific passenger.
 * - mount <[horse]> remove passenger:<[player]>
 *
 * @Usage
 * // Eject all passengers from a vehicle.
 * - mount <[horse]> eject
 */
public class MountCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "mount";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("sit");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<vehicle>] (remove|eject) (passenger:<entity>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override public int getMaxArgs() {
        return -1;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        Entity vehicle = resolveEntityArg(instruction.getLinearObject(0, queue), queue, "vehicle");
        if (vehicle == null) return;

        if (instruction.hasFlag("eject")) {
            int count = vehicle.getPassengers().size();
            Debugger.report(queue, instruction,
                    "Vehicle", instruction.getLinear(0, queue),
                    "Ejected", String.valueOf(count),
                    "Action",  "eject"
            );
            if (count == 0) {
                Debugger.echoError(queue, "Mount: vehicle has no passengers to eject.");
                return;
            }
            vehicle.eject();
            return;
        }

        Entity passenger = resolveEntityArg(instruction.getPrefixObject("passenger", queue), queue, "passenger");
        if (passenger == null) return;

        String vehicleId   = instruction.getLinear(0, queue);
        String passengerId = instruction.getPrefix("passenger", queue);

        if (instruction.hasFlag("remove")) {
            Debugger.report(queue, instruction,
                    "Vehicle",   vehicleId,
                    "Passenger", passengerId,
                    "Action",    "remove"
            );
            if (!vehicle.getPassengers().contains(passenger)) {
                Debugger.echoError(queue, "Mount: passenger is not riding this vehicle.");
                return;
            }
            passenger.leaveVehicle();
            return;
        }

        Debugger.report(queue, instruction,
                "Vehicle",   vehicleId,
                "Passenger", passengerId,
                "Action",    "mount"
        );
        if (!vehicle.addPassenger(passenger)) {
            Debugger.echoError(queue, "Mount: Paper rejected the mount (dead entity or incompatible type?).");
        }
    }

    private static Entity resolveEntityArg(
            dev.corexinc.corex.api.tags.AbstractTag tag,
            @NonNull ScriptQueue queue,
            @NonNull String argName
    ) {
        if (!(tag instanceof EntityTag entityTag)) {
            Debugger.echoError(queue, "Mount: '" + argName + "' must be an EntityTag, got: "
                    + (tag != null ? tag.identify() : "null") + ".");
            return null;
        }
        Entity entity = entityTag.getEntity();
        if (entity == null || entity.isDead()) {
            Debugger.echoError(queue, "Mount: '" + argName + "' entity is null or dead.");
            return null;
        }
        return entity;
    }
}
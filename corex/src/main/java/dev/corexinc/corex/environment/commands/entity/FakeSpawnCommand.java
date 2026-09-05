package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.scripts.CommandHelper;
import org.bukkit.Location;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

/* @doc command
 *
 * @Name FakeSpawn
 * @Syntax fakespawn [<entity>|...] (<location>) (targets:<player>|...) (duration:<duration>)
 * @RequiredArgs 1
 * @MaxArgs 4
 * @Waitable
 * @ShortDescription Shows packet-only entities to chosen players, without spawning anything in the world.
 *
 * @Implements FakeSpawn
 *
 * @Description
 * Spawns entities that exist only on the client: they are sent as packets to the target players,
 * never enter the world, never tick and cannot be hit or interact with anything. Use them for
 * decorations, previews and per-player displays.
 *
 * The first argument is one or more entity blueprints, an entity type name or an EntityTag with
 * mechanisms applied. The location defaults to the linked player's position. "targets:" picks who
 * sees them and defaults to the linked player. "duration:" removes them again after that time,
 * and defaults to 10 seconds; the waitable form '~fakespawn' pauses the queue until then.
 *
 * The spawned entities are saved through save: as a list of EntityTags, so they can be moved,
 * animated or removed early with adjust and the usual entity commands.
 *
 * @Usage
 * // Show a floating item to the linked player for ten seconds.
 * - fakespawn item_display <player.location.add[0,2,0]>
 *
 * @Usage
 * // Show a zombie to two players for a minute and keep a handle on it.
 * - fakespawn zombie <[arenaSpawn]> targets:<[attacker]>|<[defender]> duration:1m save:decoy
 */
public class FakeSpawnCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "fakespawn";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>|...] (<location>) (targets:<player>|...) (duration:<duration>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public boolean setCanBeWaitable() {
        return true;
    }

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinearRaw(0, ListTag.class, EntityTag::resolveBlueprintList)
            .optionalLinear(1, LocationTag.class, null)
            .optionalPrefix("targets", ListTag.class, null)
            .optionalPrefix("duration", DurationTag.class, "10")
            .build();

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        List<EntityTag> blueprints = EntityTag.resolveBlueprints(args.linear(0));

        if (blueprints.isEmpty()) {
            Debugger.echoError(queue, "Could not resolve any entities to spawn.");
            return;
        }

        Location location;
        if (args.linear(1) != null) {
            location = ((LocationTag) args.linear(1)).getLocation();
        } else {
            PlayerTag player = (PlayerTag) queue.getPlayer();
            location = player != null ? player.getPlayer().getLocation() : null;
        }

        if (location == null || location.getWorld() == null) {
            Debugger.echoError(queue, "Requires a valid location, or a linked player.");
            return;
        }

        ListTag targetsRaw = args.prefix("targets");
        List<UUID> targets;
        if (targetsRaw == null) {
            targets = List.of(queue.getPlayer().getUniqueId());
        } else {
            targets = targetsRaw.filter(PlayerTag.class, queue).stream().map(PlayerTag::getUniqueId).toList();
        }

        int durationTicks = ((int) ((DurationTag) args.prefix("duration")).getTicks());

        ListTag spawned = new ListTag();
        for (EntityTag blueprint : blueprints) {
            EntityTag result = blueprint.fakeSpawn(location, targets, durationTicks);
            if (result != null) spawned.addObject(result);
        }

        MapTag result = new MapTag();

        if (spawned.getList().size() == 1) {
            result.putObject("entity", spawned.getList().getFirst());
        } else {
            result.putObject("entities", spawned);
        }
        CommandHelper.saveResult(queue, instruction, result);
    }
}
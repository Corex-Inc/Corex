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
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.adapters.PlayerAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/* @doc command
 *
 * @Name Look
 * @Syntax look [<entity>|...] (<location>) (yaw:<#>) (pitch:<#>) (offthread_repeat:<#>)
 * @RequiredArgs 1
 * @MaxArgs 4
 * @ShortDescription Makes one or more entities face a target location or rotation.
 *
 * @Implements Look
 *
 * @Description
 * Forces the given entity or entities to face a target immediately (single tick).
 *
 * Accepts a target location as the second linear argument, OR a specific
 * rotation via (yaw:<#>) and/or (pitch:<#>) prefix arguments.
 * If neither is provided the command errors.
 *
 * Location mode uses Paper's lookAt API (EYES anchor) and works for any
 * LivingEntity. For Players a direction-computed yaw/pitch is applied via
 * setRotation(), which sends a position packet to the client.
 * Non-living entities support yaw/pitch mode only.
 *
 * Specify (offthread_repeat:<#>) to send extra relative-rotation packets
 * to a player's client within a single tick using async sleeps. This smooths
 * out sudden large rotations caused by client-side interpolation lag.
 * Only effective for Player entities; silently ignored for all others.
 * Requires NMS PlayerAdapter to be registered.
 *
 * The look command is ~waitable when offthread_repeat is specified.
 * Refer to Language:~waitable.
 *
 * @Usage
 * // Make the linked entity look at a fixed position.
 * - look <player> l@world,0,64,0
 *
 * @Usage
 * // Set an exact yaw and pitch for a player.
 * - look <player> yaw:90 pitch:-30
 *
 * @Usage
 * // Look with 3 extra async packets and wait for completion.
 * - ~look <player> <npc.location> offthread_repeat:3
 */
public class LookCommand implements AbstractCommand {

    private static final float UNSET = Float.NaN;

    @Override
    public @NonNull String getName() { return "look"; }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>|...] (<location>) (yaw:<#>) (pitch:<#>) (offthread_repeat:<#>)";
    }

    @Override
    public int getMinArgs() { return 1; }

    @Override
    public int getMaxArgs() { return 4; }

    @Override
    public boolean isAsyncSafe() { return false; }

    @Override
    public boolean setCanBeWaitable() { return true; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String entitiesRaw  = instruction.getLinear(0, queue);
        String targetRaw    = instruction.getLinear(1, queue);
        String yawRaw       = instruction.getPrefix("yaw",               queue);
        String pitchRaw     = instruction.getPrefix("pitch",             queue);
        String offthreadRaw = instruction.getPrefix("offthread_repeat",  queue);

        if (entitiesRaw == null) {
            Debugger.echoError(queue, "Entities argument cannot be null!");
            return;
        }

        List<AbstractTag> rawEntities = new ListTag(entitiesRaw).filter(queue, PlayerTag.class, EntityTag.class);
        if (rawEntities.isEmpty()) {
            Debugger.echoError(queue, "No valid entities resolved for look command.");
            return;
        }

        Debugger.report(queue, instruction,
                "Entities",        entitiesRaw,
                "Target",          targetRaw    != null ? targetRaw    : "none",
                "Yaw",             yawRaw       != null ? yawRaw       : "none",
                "Pitch",           pitchRaw     != null ? pitchRaw     : "none",
                "OffthreadRepeat", offthreadRaw != null ? offthreadRaw : "none",
                "IsWaitable",      instruction.isWaitable
        );

        LookTarget target = resolveLookTarget(targetRaw, yawRaw, pitchRaw, queue);
        if (target == null) return;

        int offthreadRepeats = 0;
        if (offthreadRaw != null) {
            try {
                offthreadRepeats = Math.max(0, Integer.parseInt(offthreadRaw));
            } catch (NumberFormatException e) {
                Debugger.echoError(queue, "Invalid offthread_repeat value: '" + offthreadRaw + "'.");
            }
        }

        List<Entity> targets = rawEntities.stream()
                .map(tag -> switch (tag) {
                    case PlayerTag pt -> pt.getPlayer();
                    case EntityTag et -> et.getEntity();
                    default           -> null;
                })
                .filter(e -> e != null && e.isValid() && !e.isDead())
                .toList();

        if (targets.isEmpty()) return;

        if (offthreadRepeats > 0 && instruction.isWaitable) queue.pause();

        PlayerAdapter adapter  = NMSHandler.get().get(PlayerAdapter.class);
        int           repeats  = offthreadRepeats;
        AtomicInteger pending  = new AtomicInteger(
                (int) targets.stream().filter(e -> e instanceof Player).count()
        );

        for (Entity entity : targets) {
            applyLook(entity, target, repeats, adapter, () -> {
                if (pending.decrementAndGet() == 0 && instruction.isWaitable) {
                    queue.resume();
                }
            });
        }

        if (offthreadRepeats == 0 && instruction.isWaitable) queue.resume();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void applyLook(Entity entity, LookTarget target, int offthreadRepeats,
                                  @Nullable PlayerAdapter adapter, Runnable onComplete) {
        switch (target) {
            case LookTarget.AtLocation(LocationTag locationTag) -> {
                Location location = locationTag.getLocation();
                if (entity instanceof Player player) {
                    float[] angles = directionAngles(player.getEyeLocation(), location);
                    applyPlayerRotation(player, angles[0], angles[1], offthreadRepeats, adapter, onComplete);
                } else if (entity instanceof LivingEntity living) {
                    living.lookAt(location, LookAnchor.EYES);
                }
            }
            case LookTarget.AtRotation(float yaw, float pitch) -> {
                float resolvedYaw   = Float.isNaN(yaw)   ? entity.getLocation().getYaw()   : yaw;
                float resolvedPitch = Float.isNaN(pitch) ? entity.getLocation().getPitch() : pitch;
                if (entity instanceof Player player) {
                    applyPlayerRotation(player, resolvedYaw, resolvedPitch, offthreadRepeats, adapter, onComplete);
                } else {
                    entity.setRotation(resolvedYaw, resolvedPitch);
                }
            }
        }
    }

    private static void applyPlayerRotation(Player player, float targetYaw, float targetPitch,
                                            int offthreadRepeats, @Nullable PlayerAdapter adapter,
                                            Runnable onComplete) {
        float relYaw   = normaliseAngle(targetYaw - player.getLocation().getYaw());
        float relPitch = targetPitch - player.getLocation().getPitch();

        player.setRotation(targetYaw, targetPitch);

        if (offthreadRepeats <= 0 || adapter == null) return;

        long intervalMs = Math.max(1L, 50L / (offthreadRepeats + 1));

        SchedulerAdapter.runAsync(() -> {
            for (int i = 0; i < offthreadRepeats; i++) {
                try {
                    Thread.sleep(intervalMs * (i + 1));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!player.isOnline()) break;
                adapter.sendRelativeLookPacket(player, relYaw, relPitch);
            }
            onComplete.run();
        });
    }

    private static @Nullable LookTarget resolveLookTarget(@Nullable String targetRaw,
                                                          @Nullable String yawRaw,
                                                          @Nullable String pitchRaw,
                                                          ScriptQueue queue) {
        if (targetRaw != null) {
            Object fetched = ObjectFetcher.pickObject(targetRaw);
            if (fetched instanceof LocationTag location && location.getLocation() != null) {
                return new LookTarget.AtLocation(location);
            }
            if (yawRaw == null && pitchRaw == null) {
                Debugger.echoError(queue, "'" + targetRaw + "' is not a valid LocationTag. "
                        + "Provide a valid location or use the yaw/pitch prefix arguments.");
                return null;
            }
        }

        if (yawRaw == null && pitchRaw == null) {
            Debugger.echoError(queue, "No look target supplied. "
                    + "Provide a location or use the yaw and/or pitch prefix arguments.");
            return null;
        }

        return new LookTarget.AtRotation(
                parseAngle(yawRaw,   "yaw",   queue),
                parseAngle(pitchRaw, "pitch", queue)
        );
    }

    private static float[] directionAngles(Location from, Location to) {
        double dx   = to.getX() - from.getX();
        double dy   = to.getY() - from.getY();
        double dz   = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float  yaw   = (float) -Math.toDegrees(Math.atan2(dx, dz));
        float  pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    private static float normaliseAngle(float delta) {
        delta %= 360f;
        if (delta >  180f) delta -= 360f;
        if (delta < -180f) delta += 360f;
        return delta;
    }

    private static float parseAngle(@Nullable String raw, String label, ScriptQueue queue) {
        if (raw == null) return UNSET;
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            Debugger.echoError(queue, "Invalid " + label + " value: '" + raw + "', defaulting to 0.");
            return 0f;
        }
    }

    private sealed interface LookTarget {
        record AtLocation(LocationTag location) implements LookTarget {}
        record AtRotation(float yaw, float pitch) implements LookTarget {}
    }
}
package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.WorldTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/* @doc command
 *
 * @Name Time
 * @Syntax time ({global}/player) [<time-duration>/reset] (<world>) (reset:<duration>) (freeze)
 * @RequiredArgs 1
 * @MaxArgs 5
 * @ShortDescription Changes the current time in the minecraft world.
 *
 * @Implements Time
 *
 * @Description
 * Changes the current time in a world, or the time that a single player sees the world in.
 *
 * If no world is specified, defaults to the linked player's world. If no player is
 * available, an error will be thrown.
 *
 * If 'player' is specified, this changes only that player's personal time. This is
 * separate from the global world time and does not affect any other player. When that
 * player logs off, their time resets back to global time automatically.
 *
 * Instead of a time value, you may specify 'reset' to immediately return the player's
 * time back to global time.
 *
 * If you specify a custom time, optionally specify 'reset:<duration>' to have the
 * player's time automatically reset after that duration, unless overwritten by another
 * 'time player' call before then.
 *
 * Optionally specify 'freeze' to lock the player's time exactly at the given value,
 * instead of letting it keep advancing alongside the real day/night cycle.
 *
 * @Usage
 * // Set the global time in the linked player's world.
 * - time 500t
 *
 * @Usage
 * // Make the player see a different time than everyone else.
 * - time player 500t
 *
 * @Usage
 * // Make the player see a different time than everyone else, with the sun no longer moving.
 * - time player 500t freeze
 *
 * @Usage
 * // Make the player see a different time than everyone else for the next 5 minutes.
 * - time player 500t reset:5m
 *
 * @Usage
 * // Make the player see the global time again.
 * - time player reset
 *
 * @Usage
 * // Set the time in a specific world.
 * - time 500t myworld
 */
public class TimeCommand implements AbstractCommand {
    private final Map<UUID, Integer> resetGenerations = new ConcurrentHashMap<>();

    @Override
    public @NotNull String getName() {
        return "time";
    }

    @Override
    public @NotNull String getSyntax() {
        return "({global}/player) [<time-duration>/reset] (<world>) (reset:<duration>) (freeze)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 5;
    }

    @Override
    public boolean isAsyncSafe() {
        return false;
    }

    @Override
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
        List<String> linearArgs = new ArrayList<>();
        for (int i = 0; ; i++) {
            String arg = instruction.getLinear(i, queue);
            if (arg == null) {
                break;
            }
            linearArgs.add(arg);
        }

        boolean isPlayerScoped = false;
        boolean wantsReset = false;
        boolean freeze = false;
        DurationTag value = null;
        World world = null;

        for (String raw : linearArgs) {
            if (raw.equalsIgnoreCase("global")) {
                isPlayerScoped = false;
            }
            else if (raw.equalsIgnoreCase("player")) {
                isPlayerScoped = true;
            }
            else if (raw.equalsIgnoreCase("freeze")) {
                freeze = true;
            }
            else if (raw.equalsIgnoreCase("reset") && value == null && !wantsReset) {
                wantsReset = true;
            }
            else if (value == null && !wantsReset && DurationTag.matches(raw)) {
                value = new DurationTag(raw);
            }
            else if (world == null) {
                World parsedWorld = new WorldTag(raw).getWorld();
                if (parsedWorld == null) {
                    Debugger.echoError(queue, "Unknown world: " + raw);
                    return;
                }
                world = parsedWorld;
            }
            else {
                Debugger.echoError(queue, "Unrecognized argument: " + raw);
                return;
            }
        }

        if (value == null && !wantsReset) {
            Debugger.echoError(queue, "Must specify a time value or 'reset'!");
            return;
        }

        String resetAfterRaw = instruction.getPrefix("reset", queue);
        DurationTag resetAfter = null;
        if (resetAfterRaw != null) {
            if (!DurationTag.matches(resetAfterRaw)) {
                Debugger.echoError(queue, "Invalid duration for 'reset:': " + resetAfterRaw);
                return;
            }
            resetAfter = new DurationTag(resetAfterRaw);
        }

        if (Debugger.shouldReport(queue)) {
            Debugger.report(queue, instruction,
                    "Scope", isPlayerScoped ? "player" : "global",
                    "Value", value != null ? value.getTicksLong() + "t" : "reset",
                    "Freeze", freeze,
                    "ResetAfter", resetAfter != null ? resetAfter.getTicksLong() + "t" : "none",
                    "World", world != null ? world.getName() : "(linked player's world)"
            );
        }

        if (!isPlayerScoped) {
            if (value == null) {
                Debugger.echoError(queue, "Cannot 'reset' the global world time!");
                return;
            }
            if (world == null) {
                PlayerTag fallbackPlayer = (PlayerTag) queue.getPlayer();
                if (fallbackPlayer != null && fallbackPlayer.getPlayer() != null) {
                    world = fallbackPlayer.getPlayer().getWorld();
                }
            }
            if (world == null) {
                Debugger.echoError(queue, "No world available to set the time in!");
                return;
            }

            World targetWorld = world;
            long ticks = value.getTicksLong();
            SchedulerAdapter.get().run(() -> targetWorld.setTime(ticks));
            return;
        }

        PlayerTag playerTag = (PlayerTag) queue.getPlayer();
        if (playerTag == null || playerTag.getPlayer() == null) {
            Debugger.echoError(queue, "Must have a valid player link to use 'time player'!");
            return;
        }
        Player player = playerTag.getPlayer();
        UUID uuid = player.getUniqueId();

        int generation = resetGenerations.merge(uuid, 1, Integer::sum);

        BukkitSchedulerAdapter scheduler = (BukkitSchedulerAdapter) SchedulerAdapter.get();

        if (wantsReset) {
            resetGenerations.remove(uuid);
            scheduler.runEntity(player, player::resetPlayerTime);
            return;
        }

        long timeTicks = value.getTicksLong();
        boolean relative = !freeze;
        scheduler.runEntity(player, () -> {
            if (relative) {
                player.setPlayerTime(timeTicks - player.getWorld().getTime(), true);
            }
            else {
                player.setPlayerTime(timeTicks, false);
            }
        });

        if (resetAfter != null) {
            scheduler.runEntityLater(player, () -> {
                Integer current = resetGenerations.get(uuid);
                if (current != null && current == generation && player.isOnline()) {
                    resetGenerations.remove(uuid, current);
                    player.resetPlayerTime();
                }
            }, Math.min(Integer.MAX_VALUE, resetAfter.getTicksLong()));
        }
    }
}
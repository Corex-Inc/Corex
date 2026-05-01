package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name PlaySound
 * @Syntax playsound [<sound>] (at:<location>|...) (targets:<player>|...) (volume:<#.#>) (pitch:<#.#>) (category:<category>)
 * @RequiredArgs 1
 * @MaxArgs 6
 * @ShortDescription Plays a vanilla or custom resource-pack sound.
 *
 * @Implements PlaySound
 * @Aliases sound
 *
 * @Description
 * Plays a sound either to specific players or at a specific world location.
 *
 * The command uses Kyori Adventure API. You can provide the modern Minecraft sound key
 * (like `entity.experience_orb.pickup`) OR a custom namespaced key from a resource pack
 * (like `my_pack:custom.click`).
 * For backwards compatibility, old Bukkit uppercase names (`ENTITY_PLAYER_LEVELUP`) are automatically converted.
 *
 * - `volume:` The volume of the sound (default 1.0). Values above 1.0 increase the audible distance (approx. 1 chunk per 1.0).
 * - `pitch:` The pitch/speed of the sound (default 1.0). Ranges from 0.0 (deep) to 2.0 (high-pitched).
 * - `category:` The audio channel to play the sound on (MASTER, MUSIC, RECORD, WEATHER, BLOCK, HOSTILE, NEUTRAL, PLAYER, AMBIENT, VOICE). Default is MASTER.
 *
 * Targeting logic:
 * - If `targets` are provided and `at` is NOT provided, plays the sound to the targets at their own locations.
 * - If `at` is provided and `targets` are NOT provided, plays the sound in the world at those locations (heard by anyone nearby).
 * - If BOTH `targets` and `at` are provided, plays the sound only to those targets, but originating from the specified locations (like a fake sound source).
 * - If NEITHER are provided, defaults to the player attached to the queue.
 *
 * @Usage
 * // Play a level-up sound to the attached player.
 * - playsound entity.player.levelup pitch:1.5
 *
 * @Usage
 * // Play a custom resource pack sound at a specific location for everyone nearby.
 * - playsound my_pack:magic_spell at:<player.location> volume:2.0
 *
 * @Usage
 * // Play a scary sound specifically to online players, coming from behind them.
 * - foreach <server.onlinePlayers> as:target:
 *     - playsound entity.enderman.stare targets:<[target]> at:<[target].location.backward[3]>
 */
public class PlaySoundCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "playsound";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("sound");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<sound>] (at:<location>|...) (targets:<player>|...) (volume:<#.#>) (pitch:<#.#>) (category:<category>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 6;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String soundRaw = instruction.getLinear(0, queue);
        if (soundRaw == null) {
            Debugger.echoError(queue, "Sound argument cannot be null!");
            return;
        }

        Key soundKey;
        try {
            String normalized = soundRaw.toLowerCase();

            if (!soundRaw.contains(":") && !soundRaw.contains(".") && soundRaw.equals(soundRaw.toUpperCase())) {
                normalized = normalized.replace('_', '.');
            }

            if (normalized.contains(":")) {
                soundKey = Key.key(normalized);
            } else {
                soundKey = Key.key(Key.MINECRAFT_NAMESPACE, normalized);
            }
        } catch (Exception ex) {
            Debugger.echoError(queue, "Invalid sound format: " + soundRaw + ". Expected 'namespace:value' or 'entity.player.levelup'.");
            return;
        }

        float volume = parseNumberSafely(queue, instruction, "volume", 1.0f);
        float pitch = parseNumberSafely(queue, instruction, "pitch", 1.0f);

        String categoryRaw = instruction.getPrefix("category", queue);
        Sound.Source category = Sound.Source.MASTER;
        if (categoryRaw != null) {
            try {
                category = Sound.Source.valueOf(categoryRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                Debugger.echoError(queue, "Invalid sound category: " + categoryRaw + ". Falling back to MASTER.");
            }
        }

        final Sound adventureSound = Sound.sound(soundKey, category, volume, pitch);

        String atRaw = instruction.getPrefix("at", queue);
        List<LocationTag> locations = new ArrayList<>();
        if (atRaw != null) {
            locations = new ListTag(atRaw).filter(LocationTag.class, queue);
        }

        String targetsRaw = instruction.getPrefix("targets", queue);
        List<Player> targetPlayers = new ArrayList<>();

        if (targetsRaw != null) {
            for (PlayerTag pTag : new ListTag(targetsRaw).filter(PlayerTag.class, queue)) {
                Player player = pTag.getPlayer();
                if (player != null && player.isOnline()) targetPlayers.add(player);
            }
        } else if (atRaw == null) {
            PlayerTag queuePlayer = queue.getPlayer();
            if (queuePlayer != null && queuePlayer.getPlayer() != null && queuePlayer.getPlayer().isOnline()) {
                targetPlayers.add(queuePlayer.getPlayer());
            }
        }

        if (locations.isEmpty() && targetPlayers.isEmpty()) {
            Debugger.echoError(queue, "No valid locations or targets found to play the sound.");
            return;
        }

        Debugger.report(queue, instruction,
                "Sound", soundKey.asString(),
                "Volume", volume,
                "Pitch", pitch,
                "Category", category.name(),
                "Locations", locations.size(),
                "Targets", targetPlayers.size()
        );

        final List<LocationTag> finalLocations = locations;

        if (!targetPlayers.isEmpty()) {
            for (Player player : targetPlayers) {
                SchedulerAdapter.runEntity(player, () -> {
                    if (finalLocations.isEmpty()) {
                        player.playSound(adventureSound);
                    } else {
                        for (LocationTag locTag : finalLocations) {
                            Location loc = locTag.getLocation();
                            if (loc != null) {
                                player.playSound(adventureSound, loc.getX(), loc.getY(), loc.getZ());
                            }
                        }
                    }
                });
            }
        }
        else {
            for (LocationTag locTag : finalLocations) {
                Location loc = locTag.getLocation();
                if (loc == null || loc.getWorld() == null) continue;

                SchedulerAdapter.runAt(loc, () -> {
                    loc.getWorld().playSound(adventureSound, loc.getX(), loc.getY(), loc.getZ());
                });
            }
        }
    }

    private float parseNumberSafely(ScriptQueue queue, Instruction instruction, String prefix, float defaultValue) {
        AbstractTag tag = instruction.getPrefixObject(prefix, queue);
        if (tag == null) return defaultValue;

        try {
            return Float.parseFloat(tag.identify());
        } catch (NumberFormatException e) {
            Debugger.echoError(queue, "Invalid number format for '" + prefix + "': " + tag.identify());
            return defaultValue;
        }
    }
}
package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name PlaySound
 * @Syntax playsound [<sound>] (at:<location>|...) (targets:<player>|...) (volume:<#.#>) (pitch:<#.#>) (category:<category>)
 * @RequiredArgs 1
 * @MaxArgs 6
 * @Aliases sound
 * @ShortDescription Plays a sound to players or at world locations.
 *
 * @Implements PlaySound
 *
 * @Description
 * Plays a minecraft sound by its key. Both spellings work: "entity.player.levelup"
 * and "ENTITY_PLAYER_LEVELUP". Custom sounds from resource packs work too, written
 * as a namespaced key like "mypack:my.sound".
 *
 * With targets: the sound plays only for those players. With at: it plays at the
 * given world locations for anyone in range. Give both and each target hears the
 * sound coming from those positions. Give neither and it plays for the linked player.
 *
 * volume: defaults to 1.0; values above 1.0 don't get louder but carry further.
 * pitch: defaults to 1.0, clients accept 0.5 to 2.0.
 * category: picks which client sound slider applies (master, music, record, weather,
 * block, hostile, neutral, player, ambient, voice). Defaults to master.
 *
 * @Usage
 * // Level-up chime for the linked player.
 * - playsound entity.player.levelup
 *
 * @Usage
 * // An explosion at a stored location, audible to anyone nearby.
 * - playsound entity.generic.explode at:<[explosionSpot]>
 *
 * @Usage
 * // A quiet, low cave noise for two specific players.
 * - playsound ambient.cave targets:<[p1]>|<[p2]> volume:0.4 pitch:0.8
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
    public boolean isAsyncSafe() {
        return true;
    }

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ElementTag.class)
            .optionalPrefix("volume", ElementTag.class, "1")
            .optionalPrefix("pitch", ElementTag.class, "1")
            .optionalPrefix("category", ElementTag.class)
            .optionalPrefix("at", ListTag.class)
            .optionalPrefix("targets", ListTag.class)
            .build();

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        ElementTag soundTag = args.linear(0);
        String soundRaw = soundTag.asString();

        Key soundKey;
        try {
            soundKey = parseKey(soundRaw);
        } catch (Exception e) {
            Debugger.echoError(queue, "Invalid sound format: " + soundRaw + ". Expected 'namespace:value' or 'entity.player.levelup'.");
            return;
        }

        float volume = parseFloat(queue, args.prefix("volume"), "volume");
        float pitch = parseFloat(queue, args.prefix("pitch"), "pitch");
        Sound.Source category = parseCategory(queue, args.prefix("category"));
        Sound sound = Sound.sound(soundKey, category, volume, pitch);

        ListTag atList = args.prefix("at");
        List<LocationTag> locations = atList != null ? atList.filter(LocationTag.class, queue) : List.of();
        List<Player> targets = parseTargets(queue, args.prefix("targets"), locations);

        if (locations.isEmpty() && targets.isEmpty()) {
            Debugger.echoError(queue, "No valid locations or targets found to play the sound.");
            return;
        }

        Debugger.report(queue, instruction,
                "Sound", soundKey.asString(),
                "Volume", volume,
                "Pitch", pitch,
                "Category", category.name(),
                "Locations", locations.size(),
                "Targets", targets.size()
        );

        if (!targets.isEmpty()) {
            playToTargets(targets, locations, sound);
        } else {
            playAtLocations(locations, sound);
        }
    }

    private Key parseKey(String raw) {
        String normalized = raw.toLowerCase();

        if (!raw.contains(":") && !raw.contains(".") && raw.equals(raw.toUpperCase())) {
            normalized = normalized.replace('_', '.');
        }

        return normalized.contains(":")
                ? Key.key(normalized)
                : Key.key(Key.MINECRAFT_NAMESPACE, normalized);
    }

    private Sound.Source parseCategory(ScriptQueue queue, ElementTag categoryTag) {
        if (categoryTag == null) return Sound.Source.MASTER;

        try {
            return Sound.Source.valueOf(categoryTag.asString().toUpperCase());
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid sound category: " + categoryTag.asString() + ". Falling back to MASTER.");
            return Sound.Source.MASTER;
        }
    }

    private List<Player> parseTargets(ScriptQueue queue, ListTag targetsList, List<LocationTag> locations) {
        if (targetsList != null) {
            return targetsList.filter(PlayerTag.class, queue).stream()
                    .map(PlayerTag::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .toList();
        }

        if (locations.isEmpty()) {
            PlayerTag queuePlayer = (PlayerTag) queue.getPlayer();
            if (queuePlayer != null) {
                Player player = queuePlayer.getPlayer();
                if (player != null && player.isOnline()) return List.of(player);
            }
        }

        return List.of();
    }

    private void playToTargets(List<Player> targets, List<LocationTag> locations, Sound sound) {
        for (Player player : targets) {
            ((BukkitSchedulerAdapter) SchedulerAdapter.get()).runEntity(player, () -> {
                if (locations.isEmpty()) {
                    player.playSound(sound);
                } else {
                    for (LocationTag locTag : locations) {
                        Location loc = locTag.getLocation();
                        if (loc != null) player.playSound(sound, loc.getX(), loc.getY(), loc.getZ());
                    }
                }
            });
        }
    }

    private void playAtLocations(List<LocationTag> locations, Sound sound) {
        for (LocationTag locTag : locations) {
            Location loc = locTag.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(loc), () -> loc.getWorld().playSound(sound, loc.getX(), loc.getY(), loc.getZ()));
        }
    }

    private float parseFloat(ScriptQueue queue, ElementTag tag, String prefix) {
        if (tag == null) return 1.0f;

        try {
            return Float.parseFloat(tag.asString());
        } catch (NumberFormatException e) {
            Debugger.echoError(queue, "Invalid number for '" + prefix + "': " + tag.identify());
            return 1.0f;
        }
    }
}
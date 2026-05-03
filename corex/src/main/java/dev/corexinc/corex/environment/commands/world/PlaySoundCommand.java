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

import java.util.List;

public class PlaySoundCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() { return "playsound"; }

    @Override
    public @NonNull List<String> getAlias() { return List.of("sound"); }

    @Override
    public @NonNull String getSyntax() {
        return "[<sound>] (at:<location>|...) (targets:<player>|...) (volume:<#.#>) (pitch:<#.#>) (category:<category>)";
    }

    @Override
    public int getMinArgs() { return 1; }

    @Override
    public int getMaxArgs() { return 6; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String soundRaw = instruction.getLinear(0, queue);
        if (soundRaw == null) {
            Debugger.echoError(queue, "Sound argument cannot be null!");
            return;
        }

        Key soundKey;
        try {
            soundKey = parseKey(soundRaw);
        } catch (Exception e) {
            Debugger.echoError(queue, "Invalid sound format: " + soundRaw + ". Expected 'namespace:value' or 'entity.player.levelup'.");
            return;
        }

        float volume          = parseFloat(queue, instruction, "volume");
        float pitch           = parseFloat(queue, instruction, "pitch");
        Sound.Source category = parseCategory(queue, instruction);
        Sound sound           = Sound.sound(soundKey, category, volume, pitch);

        List<LocationTag> locations = parseLocations(queue, instruction);
        List<Player> targets        = parseTargets(queue, instruction, locations);

        if (locations.isEmpty() && targets.isEmpty()) {
            Debugger.echoError(queue, "No valid locations or targets found to play the sound.");
            return;
        }

        Debugger.report(queue, instruction,
                "Sound",     soundKey.asString(),
                "Volume",    volume,
                "Pitch",     pitch,
                "Category",  category.name(),
                "Locations", locations.size(),
                "Targets",   targets.size()
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

    private Sound.Source parseCategory(ScriptQueue queue, Instruction instruction) {
        String raw = instruction.getPrefix("category", queue);
        if (raw == null) return Sound.Source.MASTER;

        try {
            return Sound.Source.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid sound category: " + raw + ". Falling back to MASTER.");
            return Sound.Source.MASTER;
        }
    }

    private List<LocationTag> parseLocations(ScriptQueue queue, Instruction instruction) {
        String raw = instruction.getPrefix("at", queue);
        return raw != null ? new ListTag(raw).filter(LocationTag.class, queue) : List.of();
    }

    private List<Player> parseTargets(ScriptQueue queue, Instruction instruction, List<LocationTag> locations) {
        String raw = instruction.getPrefix("targets", queue);

        if (raw != null) {
            return new ListTag(raw).filter(PlayerTag.class, queue).stream()
                    .map(PlayerTag::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .toList();
        }

        if (locations.isEmpty()) {
            PlayerTag queuePlayer = queue.getPlayer();
            if (queuePlayer != null) {
                Player player = queuePlayer.getPlayer();
                if (player != null && player.isOnline()) return List.of(player);
            }
        }

        return List.of();
    }

    private void playToTargets(List<Player> targets, List<LocationTag> locations, Sound sound) {
        for (Player player : targets) {
            SchedulerAdapter.runEntity(player, () -> {
                if (locations.isEmpty()) {
                    player.playSound(sound);
                } else {
                    for (LocationTag locTag : locations) {
                        org.bukkit.Location loc = locTag.getLocation();
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
            SchedulerAdapter.runAt(loc, () -> loc.getWorld().playSound(sound, loc.getX(), loc.getY(), loc.getZ()));
        }
    }

    private float parseFloat(ScriptQueue queue, Instruction instruction, String prefix) {
        AbstractTag tag = instruction.getPrefixObject(prefix, queue);
        if (tag == null) return 1.0f;

        try {
            return Float.parseFloat(tag.identify());
        } catch (NumberFormatException e) {
            Debugger.echoError(queue, "Invalid number for '" + prefix + "': " + tag.identify());
            return 1.0f;
        }
    }
}
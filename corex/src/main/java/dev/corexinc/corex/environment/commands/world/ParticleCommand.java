package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Particle
 * @Syntax particle [<particle>] (at:<location>|...) (data:<#.#>) (specialData:<map>) (visibility:<#>) (quantity:<#>) (offset:<location>) (velocity:<location>) (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 9
 * @ShortDescription Plays a visual particle effect at a specific location.
 *
 * @Implements Particle
 * @Aliases playeffect
 *
 * @Description
 * Spawns a particle effect at the specified location(s).
 *
 * Arguments:
 * - `particle`: The Bukkit particle name (e.g., FLAME, DUST, EXPLOSION).
 * - `at:`: A list of locations where the particle should spawn.
 * - `data:`: Additional data for the particle (usually speed). Defaults to 0.
 * - `quantity:`: The number of particles to spawn. Defaults to 1.
 * - `visibility:`: The block radius in which players can see the particle. Defaults to 15.
 * - `offset:`: A location tag used as an XYZ offset for randomizing particle spread.
 * - `velocity:`: A location tag used as a directional vector.
 * - `targets:`: Specific players to show the particle to (overrides visibility).
 * - `specialData:`: A MapTag containing required parameters for specific particles.
 *
 * Special Data formats:
 * - `DUST`: requires `[color=<hex/rgb>;size=<float>]` (e.g. `[color=#FF0000;size=1.5]`)
 * - `DUST_COLOR_TRANSITION`: requires `[from=<color>;to=<color>;size=<float>]`
 * - `BLOCK` / `FALLING_DUST`: requires `[material=<material>]`
 * - `ITEM`: requires `[item=<material>]`
 * - `SCULK_CHARGE`: requires `[radians=<float>]`
 * - `SHRIEK`: requires `[duration=<duration>]`
 *
 * @Usage
 * // Spawn a huge explosion at the player's cursor.
 * - particle EXPLOSION at:<player.cursorOn> visibility:50 quantity:1
 *
 * @Usage
 * // Spawn a red dust trail.
 * - particle DUST at:<player.location.above[2]> quantity:10 offset:l@0.5,0.5,0.5 specialData:[color=#FF0000;size=1.2]
 *
 * @Usage
 * // Shoot a flame particle in a specific direction using velocity.
 * - particle FLAME at:<player.eyeLocation> velocity:<player.location.direction> data:0.5
 */
public class ParticleCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "particle";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("playeffect");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<particle>] (at:<location>|...) (data:<#.#>) (specialData:<map>) (visibility:<#>) (quantity:<#>) (offset:<location>) (velocity:<location>) (targets:<player>|...)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 9;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String particleRaw = instruction.getLinear(0, queue);
        if (particleRaw == null) {
            Debugger.echoError(queue, "Particle name cannot be null!");
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(particleRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Unknown particle type: " + particleRaw);
            return;
        }

        String atRaw = instruction.getPrefix("at", queue);
        if (atRaw == null) {
            Debugger.echoError(queue, "You must specify at least one location using 'at:<location>'");
            return;
        }

        List<LocationTag> locations = new ListTag(atRaw).filter(LocationTag.class, queue);

        ElementTag dataTag = (ElementTag) instruction.getPrefixObject("data", queue);
        double data = dataTag != null ? dataTag.asDouble() : 0.0;

        ElementTag quantityTag = (ElementTag) instruction.getPrefixObject("quantity", queue);
        int quantity = quantityTag != null ? quantityTag.asInt() : 1;

        ElementTag visibilityTag = (ElementTag) instruction.getPrefixObject("visibility", queue);
        int visibility = visibilityTag != null ? visibilityTag.asInt() : 15;

        LocationTag offsetTag = (LocationTag) instruction.getPrefixObject("offset", queue);
        Location offset = offsetTag != null ? offsetTag.getLocation() : new Location(null, 0, 0, 0);

        LocationTag velocityTag = (LocationTag) instruction.getPrefixObject("velocity", queue);
        Location velocity = velocityTag != null ? velocityTag.getLocation() : null;

        List<Player> targetPlayers = null;
        String targetsRaw = instruction.getPrefix("targets", queue);
        if (targetsRaw != null) {
            targetPlayers = new ArrayList<>();
            for (PlayerTag pTag : new ListTag(targetsRaw).filter(PlayerTag.class, queue)) {
                if (pTag.getPlayer() != null && pTag.getPlayer().isOnline()) targetPlayers.add(pTag.getPlayer());
            }
        }

        Object specialData = null;
        if (particle.getDataType() != Void.class) {
            String specialRaw = instruction.getPrefix("specialData", queue);
            if (specialRaw == null) {
                Debugger.echoError(queue, "Particle " + particle.name() + " requires 'specialData:'!");
                return;
            }
            specialData = parseSpecialData(queue, particle, specialRaw);
            if (specialData == null) return;
        }

        final Object finalData = specialData;
        final List<Player> finalTargets = targetPlayers;

        Debugger.report(queue, instruction,
                "Particle", particle.name(),
                "Quantity", quantity,
                "Data/Speed", data,
                "Visibility", visibility + " blocks",
                "Has Velocity", velocity != null,
                "Special Data", specialData != null ? specialData.getClass().getSimpleName() : "None"
        );

        for (LocationTag locTag : locations) {
            Location loc = locTag.getLocation();
            if (loc.getWorld() == null) continue;

            SchedulerAdapter.runAt(loc, () -> {
                com.destroystokyo.paper.ParticleBuilder builder = new com.destroystokyo.paper.ParticleBuilder(particle)
                        .location(loc)
                        .data(finalData);

                if (finalTargets != null) {
                    builder.receivers(finalTargets);
                } else {
                    builder.receivers(visibility);
                }

                if (velocity != null) {
                    for (int i = 0; i < quantity; i++) {
                        Location randomized = loc.clone().add(
                                (Math.random() - 0.5) * offset.getX() * 2,
                                (Math.random() - 0.5) * offset.getY() * 2,
                                (Math.random() - 0.5) * offset.getZ() * 2
                        );
                        builder.location(randomized)
                                .count(0)
                                .offset(velocity.getX(), velocity.getY(), velocity.getZ())
                                .extra(data)
                                .spawn();
                    }
                } else {
                    builder.count(quantity)
                            .offset(offset.getX(), offset.getY(), offset.getZ())
                            .extra(data)
                            .spawn();
                }
            });
        }
    }

    private Object parseSpecialData(ScriptQueue queue, Particle particle, String specialRaw) {
        Object fetched = ObjectFetcher.pickObject(specialRaw);
        if (!(fetched instanceof MapTag map)) {
            Debugger.echoError(queue, "specialData must be a MapTag (e.g.[color=red;size=1.0])!");
            return null;
        }

        Class<?> type = particle.getDataType();

        try {
            if (type == Particle.DustOptions.class) {
                Color color = new ColorTag(getMapString(map, "color", "#FF0000")).asBukkitColor();
                float size = Float.parseFloat(getMapString(map, "size", "1.0"));
                return new Particle.DustOptions(color, size);
            }
            else if (type == Particle.DustTransition.class) {
                Color from = new ColorTag(getMapString(map, "from", "#FF0000")).asBukkitColor();
                Color to = new ColorTag(getMapString(map, "to", "#0000FF")).asBukkitColor();
                float size = Float.parseFloat(getMapString(map, "size", "1.0"));
                return new Particle.DustTransition(from, to, size);
            }
            else if (type == org.bukkit.block.data.BlockData.class) {
                Material mat = new MaterialTag(getMapString(map, "material", "STONE")).getMaterial();
                return (mat != null && mat.isBlock()) ? mat.createBlockData() : Material.STONE.createBlockData();
            }
            else if (type == ItemStack.class) {
                Material mat = new MaterialTag(getMapString(map, "item", "STICK")).getMaterial();
                return new ItemStack(mat != null ? mat : Material.STICK);
            }
            else if (type == Color.class) {
                return new ColorTag(getMapString(map, "color", "#FF0000")).asBukkitColor();
            }
            else if (type == Float.class) {
                return Float.parseFloat(getMapString(map, "radians", "0.0"));
            }
            else if (type == Integer.class) {
                return (int) new DurationTag(getMapString(map, "duration", "1s")).getTicks();
            }
            else if (type == Vibration.class) {
                String destRaw = getMapString(map, "destination", null);
                if (destRaw == null) {
                    Debugger.echoError(queue, "Vibration particle requires a 'destination' in specialData!");
                    return null;
                }
                Object fetchedDest = ObjectFetcher.pickObject(destRaw);
                Vibration.Destination destination;
                if (fetchedDest instanceof PlayerTag pTag && pTag.getPlayer() != null) {
                    destination = new Vibration.Destination.EntityDestination(pTag.getPlayer());
                } else {
                    LocationTag destLoc = fetchedDest instanceof LocationTag lt ? lt : new LocationTag(destRaw);
                    destination = new Vibration.Destination.BlockDestination(destLoc.getLocation());
                }
                int arrivalTicks = (int) new DurationTag(getMapString(map, "duration", "1s")).getTicks();
                return new Vibration(destination, arrivalTicks);
            }
        } catch (Exception e) {
            Debugger.echoError(queue, "Failed to parse specialData for " + particle.name() + ": " + e.getMessage());
            return null;
        }

        Debugger.echoError(queue, "Unsupported particle data type: " + type.getSimpleName());
        return null;
    }

    private String getMapString(MapTag map, String key, String def) {
        AbstractTag tag = map.getObject(key);
        return tag != null ? tag.identify() : def;
    }
}
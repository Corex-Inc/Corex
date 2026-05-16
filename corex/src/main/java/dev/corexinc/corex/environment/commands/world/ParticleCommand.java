package dev.corexinc.corex.environment.commands.world;

import com.destroystokyo.paper.ParticleBuilder;
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
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
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
 * - particle DUST at:<player.location.above[2]> quantity:10 offset:0.5,0.5,0.5 specialData:[color=#FF0000;size=1.2]
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

        final Particle particle;
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
        if (locations.isEmpty()) {
            Debugger.echoError(queue, "No valid locations found in 'at:' argument.");
            return;
        }

        double parsedData = 0.0;
        String dataRaw = instruction.getPrefix("data", queue);
        if (dataRaw != null) {
            try { parsedData = Double.parseDouble(dataRaw); }
            catch (NumberFormatException e) {
                Debugger.echoError(queue, "Invalid number for 'data': " + dataRaw);
                return;
            }
        }
        final double data = parsedData;

        int parsedQuantity = 1;
        String quantityRaw = instruction.getPrefix("quantity", queue);
        if (quantityRaw != null) {
            try { parsedQuantity = Integer.parseInt(quantityRaw); }
            catch (NumberFormatException e) {
                Debugger.echoError(queue, "Invalid number for 'quantity': " + quantityRaw);
                return;
            }
        }
        final int quantity = parsedQuantity;

        int parsedVisibility = 15;
        String visibilityRaw = instruction.getPrefix("visibility", queue);
        if (visibilityRaw != null) {
            try { parsedVisibility = Integer.parseInt(visibilityRaw); }
            catch (NumberFormatException e) {
                Debugger.echoError(queue, "Invalid number for 'visibility': " + visibilityRaw);
                return;
            }
        }
        final int visibility = parsedVisibility;

        AbstractTag offsetObj = instruction.getPrefixObject("offset", queue);
        Location tempOffsetLoc = null;
        if (offsetObj != null) {
            LocationTag lt = offsetObj instanceof LocationTag ? (LocationTag) offsetObj : new LocationTag(offsetObj.identify());
            tempOffsetLoc = lt.getLocation();
            if (tempOffsetLoc == null) {
                Debugger.echoError(queue, "Invalid location for 'offset': " + offsetObj.identify());
                return;
            }
        }
        final double offsetX = tempOffsetLoc != null ? tempOffsetLoc.getX() : 0.0;
        final double offsetY = tempOffsetLoc != null ? tempOffsetLoc.getY() : 0.0;
        final double offsetZ = tempOffsetLoc != null ? tempOffsetLoc.getZ() : 0.0;

        AbstractTag velocityObj = instruction.getPrefixObject("velocity", queue);
        Location tempVelocity = null;
        if (velocityObj != null) {
            LocationTag lt = velocityObj instanceof LocationTag ? (LocationTag) velocityObj : new LocationTag(velocityObj.identify());
            tempVelocity = lt.getLocation();
            if (tempVelocity == null) {
                Debugger.echoError(queue, "Invalid location for 'velocity': " + velocityObj.identify());
                return;
            }
        }
        final Location velocity = tempVelocity;

        List<Player> targetPlayers = null;
        String targetsRaw = instruction.getPrefix("targets", queue);
        if (targetsRaw != null) {
            targetPlayers = new ArrayList<>();
            for (PlayerTag pTag : new ListTag(targetsRaw).filter(PlayerTag.class, queue)) {
                Player p = pTag.getPlayer();
                if (p != null && p.isOnline()) targetPlayers.add(p);
            }
        }
        final List<Player> finalTargets = targetPlayers;

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
            if (loc == null || loc.getWorld() == null) continue;

            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(loc), () -> {
                ParticleBuilder builder = new ParticleBuilder(particle)
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
                                (Math.random() - 0.5) * offsetX * 2,
                                (Math.random() - 0.5) * offsetY * 2,
                                (Math.random() - 0.5) * offsetZ * 2
                        );
                        builder.location(randomized)
                                .count(0)
                                .offset(velocity.getX(), velocity.getY(), velocity.getZ())
                                .extra(data)
                                .spawn();
                    }
                } else {
                    builder.count(quantity)
                            .offset(offsetX, offsetY, offsetZ)
                            .extra(data)
                            .spawn();
                }
            });
        }
    }

    private Object parseSpecialData(ScriptQueue queue, Particle particle, String specialRaw) {
        AbstractTag fetched = ObjectFetcher.pickObject(specialRaw);

        MapTag map = (fetched instanceof MapTag mt) ? mt : new MapTag(specialRaw);

        if (map.keySet().isEmpty()) {
            Debugger.echoError(queue, "specialData for " + particle.name() + " is empty or invalid: " + specialRaw);
            return null;
        }

        Class<?> type = particle.getDataType();

        try {
            return switch (type.getSimpleName()) {
                case "DustOptions" -> {
                    Color color = toBukkit(new ColorTag(getMapString(map, "color", "#FF0000")));
                    float size = Float.parseFloat(getMapString(map, "size", "1.0"));
                    yield new Particle.DustOptions(color, size);
                }
                case "DustTransition" -> {
                    Color from = toBukkit(new ColorTag(getMapString(map, "from", "#FF0000")));
                    Color to = toBukkit(new ColorTag(getMapString(map, "to", "#0000FF")));
                    float size = Float.parseFloat(getMapString(map, "size", "1.0"));
                    yield new Particle.DustTransition(from, to, size);
                }
                case "BlockData" -> {
                    Material mat = new MaterialTag(getMapString(map, "material", "STONE")).getMaterial();
                    yield (mat != null && mat.isBlock()) ? mat.createBlockData() : Material.STONE.createBlockData();
                }
                case "ItemStack" -> {
                    Material mat = new MaterialTag(getMapString(map, "item", "STICK")).getMaterial();
                    yield new ItemStack(mat != null ? mat : Material.STICK);
                }
                case "Color" -> toBukkit(new ColorTag(getMapString(map, "color", "#FF0000")));
                case "Float" -> Float.parseFloat(getMapString(map, "radians", "0.0"));
                case "Integer" -> (int) new DurationTag(getMapString(map, "duration", "1s")).getTicks();
                case "Vibration" -> {
                    String destRaw = getMapString(map, "destination", null);
                    if (destRaw == null) {
                        Debugger.echoError(queue, "Vibration particle requires a 'destination' in specialData!");
                        yield null;
                    }

                    Object fetchedDest = ObjectFetcher.pickObject(destRaw);
                    Vibration.Destination destination;

                    if (fetchedDest instanceof PlayerTag pTag && pTag.getPlayer() != null) {
                        destination = new Vibration.Destination.EntityDestination(pTag.getPlayer());
                    } else {
                        LocationTag destLoc = fetchedDest instanceof LocationTag lt ? lt : new LocationTag(destRaw);
                        if (destLoc.getLocation() == null) {
                            Debugger.echoError(queue, "Invalid destination location for Vibration: " + destRaw);
                            yield null;
                        }
                        destination = new Vibration.Destination.BlockDestination(destLoc.getLocation());
                    }

                    int arrivalTicks = (int) new DurationTag(getMapString(map, "duration", "1s")).getTicks();
                    yield new Vibration(destination, arrivalTicks);
                }
                default -> {
                    Debugger.echoError(queue, "Unsupported particle data type: " + type.getSimpleName());
                    yield null;
                }
            };
        } catch (Exception e) {
            Debugger.echoError(queue, "Failed to parse specialData for " + particle.name() + ": " + e.getMessage());
            return null;
        }
    }

    private String getMapString(MapTag map, String key, String def) {
        AbstractTag tag = map.getObject(key);
        return tag != null ? tag.identify() : def;
    }

    private static Color toBukkit(ColorTag c) {
        return Color.fromARGB(c.alpha, c.red, c.green, c.blue);
    }
}
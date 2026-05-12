package dev.corexinc.corex.environment.tags.world;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.LocationPdcFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.utils.FindTag;
import dev.corexinc.corex.environment.tags.world.area.CuboidTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;
import org.bukkit.util.*;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.SimplexNoiseGenerator;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

import java.net.URL;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

/* @doc object
 *
 * @Name LocationTag
 * @Prefix l
 *
 * @Format
 * The standard format for a LocationTag is `x,y,z,pitch,yaw,world`.
 * Components like world, pitch/yaw, or even the Z coordinate can be omitted.
 * For example: `l@1,2.15,3,45,90,space` or `l@7.5,99,3.2`.
 * Note that both Z and pitch/yaw cannot be omitted simultaneously.
 *
 * @Description
 * A LocationTag represents a precise point within a game world, including its X, Y, Z coordinates,
 * as well as optional pitch and yaw rotations, and the specific world it resides in.
 *
 * The prefix 'l' (lowercase L) is commonly used to identify LocationTags.
 *
 * This object type supports custom flags, which are persisted within the corresponding chunk file in the world's directory.
 */
public class LocationTag implements AbstractTag, Flaggable {

    private static final String prefix = "l";
    private final Location location;

    public static final TagProcessor<LocationTag> TAG_PROCESSOR = new TagProcessor<>();

    @SuppressWarnings("deprecation")
    public static void register() {
        BaseTagProcessor.registerBaseTag("location", attr -> new LocationTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, LocationTag::new);

        /* @doc tag
         *
         * @Name x
         * @RawName <LocationTag.x>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the X-coordinate component of this location object.
         * This value represents the horizontal position along the X-axis in a 3D space.
         *
         * @Implements LocationTag.x
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "x", (attr, obj) -> new ElementTag(obj.location.getX()));

        /* @doc tag
         *
         * @Name y
         * @RawName <LocationTag.y>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the Y-coordinate component of this location object.
         * This value represents the vertical position in a 3D space.
         *
         * @Implements LocationTag.y
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "y", (attr, obj) -> new ElementTag(obj.location.getY()));

        /* @doc tag
         *
         * @Name z
         * @RawName <LocationTag.z>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the Z-coordinate component of this location object.
         * This value represents the horizontal position along the Z-axis in a 3D space.
         *
         * @Implements LocationTag.z
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "z", (attr, obj) -> new ElementTag(obj.location.getZ()));

        /* @doc tag
         *
         * @Name xyz
         * @RawName <LocationTag.xyz>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Retrieves the core coordinates of this location as a clean string in "x,y,z" format.
         * Excludes other specific data such as world name, pitch, and yaw.
         *
         * @Implements LocationTag.xyz
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "xyz", (attr, obj) -> {
            StringBuilder sb = new StringBuilder(32);
            obj.appendCleanDouble(sb, obj.location.getX()).append(",");
            obj.appendCleanDouble(sb, obj.location.getY()).append(",");
            obj.appendCleanDouble(sb, obj.location.getZ());
            return new ElementTag(sb.toString());
        });

        /* @doc tag
         *
         * @Name find
         * @RawName <LocationTag.find>
         * @Object LocationTag
         * @ReturnType FindTag
         * @NoArg
         * @Description
         * Returns a FindTag instance centered at this location.
         * Used as a root tag to search for nearby entities, players, or blocks.
         */
        TAG_PROCESSOR.registerTag(FindTag.class, "find", (attr, obj) -> new FindTag(obj.getLocation()));

        /* @doc tag
         *
         * @Name yaw
         * @RawName <LocationTag.yaw>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the normalized yaw rotation of this location, bounded between 0 and 360 degrees.
         *
         * @Implements LocationTag.yaw
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "yaw", (attr, obj) -> {
            float yaw = obj.location.getYaw() % 360;
            if (yaw < 0) yaw += 360;
            return new ElementTag(yaw);
        });

        /* @doc tag
         *
         * @Name rawYaw
         * @RawName <LocationTag.rawYaw>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the raw, un-normalized yaw rotation of this location directly from the server.
         *
         * @Implements LocationTag.yaw.raw
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "rawYaw", (attr, obj) -> new ElementTag(obj.location.getYaw()));

        /* @doc tag
         *
         * @Name cardinalYaw
         * @RawName <LocationTag.cardinalYaw>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the simple compass direction the location is facing based on its yaw.
         * Possible returns: North, East, South, West.
         *
         * @Implements LocationTag.yaw.simple
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "cardinalYaw", (attr, obj) -> {
            float yaw = obj.getLocation().getYaw() % 360;
            if (yaw < 0) yaw += 360;
            if (yaw >= 315 || yaw < 45) return new ElementTag("south");
            if (yaw >= 45 && yaw < 135) return new ElementTag("west");
            if (yaw >= 135 && yaw < 225) return new ElementTag("north");
            return new ElementTag("east");
        });

        /* @doc tag
         *
         * @Name pitch
         * @RawName <LocationTag.pitch>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the pitch rotation value of an object positioned at this location.
         * Pitch represents the vertical rotation around the X-axis.
         *
         * @Implements LocationTag.pitch
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "pitch", (attr, obj) -> new ElementTag(obj.location.getPitch()));

        /* @doc tag
         *
         * @Name format[]
         * @RawName <LocationTag.format[<format>]>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Description
         * Formats the location according to a custom string template.
         * Available placeholders: x, y, z, yaw, pitch, world, bx, by, bz.
         *
         * @Implements LocationTag.format[<format>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "format", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String format = attr.getParam();
            Location loc = obj.getLocation();
            format = format.replace("x", String.valueOf(loc.getX()))
                    .replace("y", String.valueOf(loc.getY()))
                    .replace("z", String.valueOf(loc.getZ()))
                    .replace("yaw", String.valueOf(loc.getYaw()))
                    .replace("pitch", String.valueOf(loc.getPitch()))
                    .replace("world", loc.getWorld() != null ? loc.getWorld().getName() : "")
                    .replace("bx", String.valueOf(loc.getBlockX()))
                    .replace("by", String.valueOf(loc.getBlockY()))
                    .replace("bz", String.valueOf(loc.getBlockZ()));
            return new ElementTag(format);
        }).test("X: bx, Y: by, Z: bz");

        /* @doc tag
         *
         * @Name simple
         * @RawName <LocationTag.simple>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns a simplified string representation of the block coordinates and the world.
         * Formatted as: x,y,z,world.
         *
         * @Implements LocationTag.simple
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "simple", (attr, obj) -> {
            Location loc = obj.getLocation();
            String world = loc.getWorld() != null ? "," + loc.getWorld().getName() : "";
            return new ElementTag(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + world);
        });

        /* @doc tag
         *
         * @Name withX[]
         * @RawName <LocationTag.withX[<number>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Creates and returns a copy of this location with the X coordinate replaced by the specified value.
         *
         * @Implements LocationTag.with_x[<number>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withX", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag num = new ElementTag(attr.getParam());
            if (!num.isDouble()) return null;
            Location loc = obj.location.clone();
            loc.setX(num.asDouble());
            return new LocationTag(loc, false);
        }).test("8");

        /* @doc tag
         *
         * @Name withY[]
         * @RawName <LocationTag.withY[<number>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Creates and returns a copy of this location with the Y coordinate replaced by the specified value.
         *
         * @Implements LocationTag.with_y[<number>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withY", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag num = new ElementTag(attr.getParam());
            if (!num.isDouble()) return null;
            Location loc = obj.location.clone();
            loc.setY(num.asDouble());
            return new LocationTag(loc, false);
        }).test("7");

        /* @doc tag
         *
         * @Name withZ[]
         * @RawName <LocationTag.withZ[<number>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Creates and returns a copy of this location with the Z coordinate replaced by the specified value.
         *
         * @Implements LocationTag.with_z[<number>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withZ", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag num = new ElementTag(attr.getParam());
            if (!num.isDouble()) return null;
            Location loc = obj.location.clone();
            loc.setZ(num.asDouble());
            return new LocationTag(loc, false);
        }).test("4");

        /* @doc tag
         *
         * @Name withYaw[]
         * @RawName <LocationTag.withYaw[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of the location with its yaw modified to the specified value.
         *
         * @Implements LocationTag.with_yaw[<number>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withYaw", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Location loc = obj.getLocation().clone();
            loc.setYaw(((float) new ElementTag(attr.getParam()).asDouble()));
            return new LocationTag(loc, false);
        }).test("14");

        /* @doc tag
         *
         * @Name withPitch[]
         * @RawName <LocationTag.withPitch[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of the location with its pitch modified to the specified value.
         *
         * @Implements LocationTag.with_pitch[<number>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withPitch", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Location loc = obj.getLocation().clone();
            loc.setPitch(((float) new ElementTag(attr.getParam()).asDouble()));
            return new LocationTag(loc, false);
        }).test("8");

        /* @doc tag
         *
         * @Name withPose[]
         * @RawName <LocationTag.withPose[<pitch>,<yaw>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of the location with both its pitch and yaw modified.
         *
         * @Implements LocationTag.with_pose[<entity>/<pitch>,<yaw>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withPose", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String[] parts = attr.getParam().split(",");
            if (parts.length != 2) return null;
            Location loc = obj.getLocation().clone();
            loc.setPitch(((float) new ElementTag(parts[0]).asDouble()));
            loc.setYaw(((float) new ElementTag(parts[1]).asDouble()));
            return new LocationTag(loc, false);
        }).test("80,22");

        /* @doc tag
         *
         * @Name toCuboid[]
         * @RawName <LocationTag.toCuboid[<location>]>
         * @Object LocationTag
         * @ReturnType CuboidTag
         * @ArgRequired
         * @Description
         * Constructs a rectangular 3D selection (CuboidTag) by using this location as the first corner
         * and the provided location as the opposite diagonal corner.
         *
         * @Implements LocationTag.to_cuboid[<location>]
         */
        TAG_PROCESSOR.registerTag(CuboidTag.class, "toCuboid", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;
            return new CuboidTag(obj.location, other.getLocation());
        }).test("1,1,1,world");

        /* @doc tag
         *
         * @Name world
         * @RawName <LocationTag.world>
         * @Object LocationTag
         * @ReturnType WorldTag
         * @NoArg
         * @Description
         * Retrieves the WorldTag representing the game world where this location is situated.
         *
         * @Implements LocationTag.world
         */
        TAG_PROCESSOR.registerTag(WorldTag.class, "world", (attr, obj) -> {
            World w = obj.location.getWorld();
            return w != null ? new WorldTag(w) : null;
        });

        /* @doc tag
         *
         * @Name add[]
         * @RawName <LocationTag.add[<location/x,y,z>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Creates and returns a new LocationTag by adding the specified coordinates or another vector/location to this object.
         * This operation does not modify the original LocationTag.
         *
         * @Implements LocationTag.add[<vector/location/x,y,z>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "add", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;

            Location loc = obj.location.clone();
            loc.add(other.getLocation().getX(), other.getLocation().getY(), other.getLocation().getZ());
            return new LocationTag(loc, false);
        }).test("l@1,1,1");

        /* @doc tag
         *
         * @Name sub[]
         * @RawName <LocationTag.sub[<location/x,y,z>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Creates and returns a new LocationTag by subtracting the specified coordinates or another vector/location from this object.
         *
         * @Implements LocationTag.sub[<vector/location/x,y,z>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "sub", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;

            Location loc = obj.location.clone();
            loc.subtract(other.getLocation().getX(), other.getLocation().getY(), other.getLocation().getZ());
            return new LocationTag(loc, false);
        }).test("1,1,1");

        /* @doc tag
         *
         * @Name mul[]
         * @RawName <LocationTag.mul[<number>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a new LocationTag representing this vector scaled up by the specified scalar multiplier.
         * X, Y and Z are multiplied, leaving pitch/yaw intact.
         *
         * @Implements LocationTag.mul[<length>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "mul", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag num = new ElementTag(attr.getParam());
            if (!num.isDouble()) return null;

            Location loc = obj.location.clone();
            loc.multiply(num.asDouble());
            return new LocationTag(loc, false);
        }).test("2");

        /* @doc tag
         *
         * @Name div[]
         * @RawName <LocationTag.div[<length>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a new LocationTag representing this vector scaled down by the specified scalar divisor.
         * X, Y and Z are divided by the provided length.
         *
         * @Implements LocationTag.div[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "div", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag num = new ElementTag(attr.getParam());
            if (!num.isDouble() || num.asDouble() == 0) return null;

            Location loc = obj.location.clone();
            loc.multiply(1.0 / num.asDouble());
            return new LocationTag(loc, false);
        }).test("0.1");

        /* @doc tag
         *
         * @Name normalize
         * @RawName <LocationTag.normalize>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a unit vector (a vector with a length of exactly 1) pointing in the identical direction as this vector.
         *
         * @Implements LocationTag.normalize
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "normalize", (attr, obj) -> {
            Vector vec = obj.location.toVector();
            if (vec.lengthSquared() == 0) return new LocationTag(obj.location);
            vec.normalize();

            Location loc = obj.location.clone();
            loc.setX(vec.getX());
            loc.setY(vec.getY());
            loc.setZ(vec.getZ());
            return new LocationTag(loc, false);
        });

        /* @doc tag
         *
         * @Name length
         * @RawName <LocationTag.length>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the geometric 3D magnitude (length) of this vector from the origin (0,0,0).
         *
         * @Implements LocationTag.vector_length
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "length", (attr, obj) ->
                new ElementTag(obj.location.toVector().length())
        );

        /* @doc tag
         *
         * @Name lengthSquared
         * @RawName <LocationTag.lengthSquared>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the squared 3D magnitude of this vector.
         * Prefer this over standard length calculations when just comparing distances as it is mathematically cheaper.
         *
         * @Implements LocationTag.vector_length_squared
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "lengthSquared", (attr, obj) ->
                new ElementTag(obj.location.toVector().lengthSquared())
        );


        /* @doc tag
         *
         * @Name distance[]
         * @RawName <LocationTag.distance[<location>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the 3D distance between this location and the targeted location.
         *
         * @Implements LocationTag.distance[<location>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "distance", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;

            /* @doc tag
             *
             * @Name distance[].horizontal
             * @RawName <LocationTag.distance[<location>].horizontal>
             * @Object LocationTag
             * @ReturnType ElementTag(Decimal)
             * @ArgRequired 1
             * @NoArg 2
             * @Description
             * Returns the horizontal (2D) distance between this location and the targeted location, ignoring the Y axis.
             *
             * @Implements LocationTag.distance[<location>].horizontal
             */
            if (attr.matchesNext("horizontal")) {
                attr.fulfill(1);
                double dx = obj.getLocation().getX() - other.getLocation().getX();
                double dz = obj.getLocation().getZ() - other.getLocation().getZ();
                return new ElementTag(Math.sqrt(dx * dx + dz * dz));
            }
            /* @doc tag
             *
             * @Name distance[].vertical
             * @RawName <LocationTag.distance[<location>].vertical>
             * @Object LocationTag
             * @ReturnType ElementTag(Decimal)
             * @ArgRequired 1
             * @NoArg 2
             * @Description
             * Returns the vertical (1D) distance between this location and the targeted location on the Y axis.
             *
             * @Implements LocationTag.distance[<location>].vertical
             */
            else if (attr.matchesNext("vertical")) {
                attr.fulfill(1);
                return new ElementTag(Math.abs(obj.getLocation().getY() - other.getLocation().getY()));
            }

            double dx = obj.getLocation().getX() - other.getLocation().getX();
            double dy = obj.getLocation().getY() - other.getLocation().getY();
            double dz = obj.getLocation().getZ() - other.getLocation().getZ();
            return new ElementTag(Math.sqrt(dx * dx + dy * dy + dz * dz));
        }).test("3,4,5");

        /* @doc tag
         *
         * @Name distanceSquared[]
         * @RawName <LocationTag.distanceSquared[<location/x,y,z>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the mathematically cheaper squared distance between this and the targeted location.
         *
         * @Implements LocationTag.distance_squared
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "distanceSquared", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;
            return new ElementTag(obj.location.toVector().distanceSquared(other.getLocation().toVector()));
        }).test("3,4,5");

        /* @doc tag
         *
         * @Name dot[]
         * @RawName <LocationTag.dot[<location/x,y,z>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the scalar dot product between this vector and the provided vector.
         * Often used in calculating viewing angles and directional checks.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "dot", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;
            return new ElementTag(obj.location.toVector().dot(other.getLocation().toVector()));
        }).test("2,3,4");

        /* @doc tag
         *
         * @Name cross[]
         * @RawName <LocationTag.cross[<location/x,y,z>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Computes the mathematical cross product between this and the target vector, resulting in a new vector orthogonal (perpendicular) to both.
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "cross", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;

            Vector cross = obj.location.toVector().crossProduct(other.getLocation().toVector());
            Location loc = obj.location.clone();
            loc.setX(cross.getX());
            loc.setY(cross.getY());
            loc.setZ(cross.getZ());
            return new LocationTag(loc, false);
        }).test("1,1,3");

        /* @doc tag
         *
         * @Name project[]
         * @RawName <LocationTag.project[<location/x,y,z>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns the mathematical projection of this vector onto the given target vector.
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "project", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;

            Vector otherVec = other.getLocation().toVector();
            double lenSq = otherVec.lengthSquared();
            if (lenSq == 0) return new LocationTag(obj.location);

            double scalar = obj.location.toVector().dot(otherVec) / lenSq;
            Location loc = obj.location.clone();
            loc.setX(otherVec.getX() * scalar);
            loc.setY(otherVec.getY() * scalar);
            loc.setZ(otherVec.getZ() * scalar);
            return new LocationTag(loc, false);
        }).test("l@2,2,2");

        /* @doc tag
         *
         * @Name withWorld[]
         * @RawName <LocationTag.withWorld[<world>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Creates and returns a new LocationTag with its world set to the specified WorldTag.
         * The X, Y, Z, yaw, and pitch coordinates of the original location are preserved.
         *
         * @Implements LocationTag.with_world
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withWorld", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Location loc = obj.location.clone();

            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            if (fetched instanceof WorldTag wt) {
                loc.setWorld(wt.getWorld());
            } else {
                loc.setWorld(Bukkit.getWorld(attr.getParam()));
            }

            return new LocationTag(loc, false);
        }).test("world");

        /* @doc tag
         *
         * @Name block
         * @RawName <LocationTag.block>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Retrieves a new LocationTag representing the block coordinates of the current location by rounding down its X, Y, and Z values.
         * This tag effectively removes decimal components, yielding integer block coordinates.
         * It does not represent the actual block entity, but merely its location.
         *
         * @Implements LocationTag.block
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "block", (attr, obj) -> new LocationTag(new Location(
                obj.location.getWorld(),
                obj.location.getBlockX(),
                obj.location.getBlockY(),
                obj.location.getBlockZ()
        )));

        /* @doc tag
         *
         * @Name material
         * @RawName <LocationTag.material>
         * @Object LocationTag
         * @ReturnType MaterialTag
         * @NoArg
         * @Description
         * Retrieves the MaterialTag representing the type of block present at this location in the world.
         *
         * @Implements LocationTag.material
         */
        TAG_PROCESSOR.registerTag(MaterialTag.class, "material", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);
            return new MaterialTag(loc.getBlock());
        });

        /* @doc tag
         *
         * @Name region
         * @RawName <LocationTag.region>
         * @Object LocationTag
         * @ReturnType RegionTag
         * @NoArg
         * @Description
         * Returns the region (tick-thread) that manages this location.
         */
        TAG_PROCESSOR.registerTag(RegionTag.class, "region", (attr, obj) ->
                new RegionTag(
                        obj.getLocation().getWorld(),
                        obj.getLocation().getBlockX() >> 4,
                        obj.getLocation().getBlockZ() >> 4
                )
        );

        /* @doc tag
         *
         * @Name biome
         * @RawName <LocationTag.biome>
         * @Object LocationTag
         * @ReturnType BiomeTag
         * @NoArg
         * @Description
         * Returns a BiomeTag representing the biome at this specific point (X, Y, Z).
         * Takes height into account, which is important for cave or vertical biomes.
         *
         * @Implements LocationTag.biome
         *
         * @Usage
         * - narrate "You are in biome: <player.location.biome.name>"
         */
        TAG_PROCESSOR.registerTag(BiomeTag.class, "biome", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;

            NamespacedKey key = loc.getWorld().getBiome(
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
            ).getKey();

            return new BiomeTag(loc.getWorld().getName() + "," + key);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name toAxisAngleQuaternion[]
         * @RawName <LocationTag.toAxisAngleQuaternion[<angle>]>
         * @Object LocationTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Returns a quaternion that is a rotation around this vector as an axis, by the given angle input (in radians).
         *
         * @Implements LocationTag.to_axis_angle_quaternion
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "toAxisAngleQuaternion", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag angleEl = new ElementTag(attr.getParam());
            if (!angleEl.isDouble()) return null;

            Vector3d axis = new Vector3d(
                    obj.getLocation().getX(),
                    obj.getLocation().getY(),
                    obj.getLocation().getZ()
            ).normalize();

            return new QuaternionTag(new Quaterniond().fromAxisAngleRad(axis, angleEl.asDouble()));
        }).test("45");

        /* @doc tag
         *
         * @Name quaternionBetween[]
         * @RawName <LocationTag.quaternionBetween[<location/vector>]>
         * @Object LocationTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Returns a quaternion that represents the rotation from this vector to another.
         *
         * @Implements LocationTag.quaternion_between_vectors
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "quaternionBetween", (attr, obj) -> {
            LocationTag other = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (other == null) return null;

            Vector3d v1 = new Vector3d(
                    obj.getLocation().getX(),
                    obj.getLocation().getY(),
                    obj.getLocation().getZ()
            ).normalize();

            Vector3d v2 = new Vector3d(
                    other.getLocation().getX(),
                    other.getLocation().getY(),
                    other.getLocation().getZ()
            ).normalize();

            return new QuaternionTag(new Quaterniond().rotationTo(v1, v2));
        }).test("2,3,4");

        /* @doc tag
         *
         * @Name buriedItem
         * @RawName <LocationTag.buriedItem>
         * @Object LocationTag
         * @ReturnType ItemTag
         * @NoArg
         * @Description
         * Returns the item currently buried inside a brushable block (e.g., suspicious sand or gravel).
         * Returns null if the block is empty or not brushable.
         *
         * @Implements LocationTag.buried_item
         */
        TAG_PROCESSOR.registerTag(ItemTag.class, "buriedItem", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);
            if (loc.getBlock().getState() instanceof BrushableBlock brushable) {
                return new ItemTag(brushable.getItem());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name lastInteractedSlot
         * @RawName <LocationTag.lastInteractedSlot>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the slot index (from 1 to 6) that was most recently interacted with in a chiseled bookshelf.
         *
         * @Implements LocationTag.last_interacted_slot
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "lastInteractedSlot", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);
            if (loc.getBlock().getState() instanceof ChiseledBookshelf shelf) {
                return new ElementTag(shelf.getLastInteractedSlot() + 1);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name sherds
         * @RawName <LocationTag.sherds>
         * @Object LocationTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a map containing the sherds applied to a decorated pot.
         * The keys represent the sides (e.g., 'front', 'back', 'left', 'right') and values are the sherd materials.
         *
         * @Implements LocationTag.sherds
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "sherds", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);
            if (loc.getBlock().getState() instanceof DecoratedPot pot) {
                MapTag map = new MapTag();
                for (var entry : pot.getSherds().entrySet()) {
                    map.putObject(entry.getKey().name().toLowerCase(), new MaterialTag(entry.getValue()));
                }
                return map;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name slot[]
         * @RawName <LocationTag.slot[<vector>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @ArgRequired
         * @Description
         * Calculates and returns the specific slot (1-6) of a chiseled bookshelf that is targeted by the provided directional vector.
         *
         * @Implements LocationTag.slot[<vector>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "slot", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag vector = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (vector == null) return null;
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);
            if (loc.getBlock().getState() instanceof ChiseledBookshelf shelf) {
                return new ElementTag(shelf.getSlot(vector.getLocation().toVector()) + 1);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name temperature
         * @RawName <LocationTag.temperature>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the internal temperature of the biome at this exact location coordinate.
         *
         * @Implements LocationTag.temperature
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "temperature", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);
            return new ElementTag(loc.getBlock().getTemperature());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name vectorToFace
         * @RawName <LocationTag.vectorToFace>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Calculates and returns the closest BlockFace name corresponding to the vector representation of this location.
         *
         * @Implements LocationTag.vector_to_face
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "vectorToFace", (attr, obj) -> {
            Vector vec = obj.getLocation().toVector().normalize();
            double x = vec.getX(), y = vec.getY(), z = vec.getZ();
            if (Math.abs(y) > 0.7) return new ElementTag(y > 0 ? "UP" : "DOWN");

            float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
            yaw = yaw % 360;
            if (yaw < 0) yaw += 360;
            if (yaw >= 337.5 || yaw < 22.5) return new ElementTag("SOUTH");
            if (yaw >= 22.5 && yaw < 67.5) return new ElementTag("SOUTH_WEST");
            if (yaw >= 67.5 && yaw < 112.5) return new ElementTag("WEST");
            if (yaw >= 112.5 && yaw < 157.5) return new ElementTag("NORTH_WEST");
            if (yaw >= 157.5 && yaw < 202.5) return new ElementTag("NORTH");
            if (yaw >= 202.5 && yaw < 247.5) return new ElementTag("NORTH_EAST");
            if (yaw >= 247.5 && yaw < 292.5) return new ElementTag("EAST");
            return new ElementTag("SOUTH_EAST");
        });

        /* @doc tag
         *
         * @Name isIn[]
         * @RawName <LocationTag.isIn[<area>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Checks whether this location is contained within the specified area object, such as a CuboidTag.
         *
         * @Implements LocationTag.is_in[<area>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isIn", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            CuboidTag cuboid = attr.getParamObject(CuboidTag.class, CuboidTag::new);
            if (cuboid != null) {
                return new ElementTag(cuboid.contains(obj.getLocation()));
            }
            return new ElementTag(false);
        }).test("world,2,2,2,2,2,2");

        /* @doc tag
         *
         * @Name above[]
         * @RawName <LocationTag.above[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved upwards along the Y-axis by the specified distance (defaults to 1 block).
         *
         * @Implements LocationTag.above[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "above", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            return new LocationTag(obj.getLocation().clone().add(0, distance, 0));
        });

        /* @doc tag
         *
         * @Name below[]
         * @RawName <LocationTag.below[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved downwards along the Y-axis by the specified distance (defaults to 1 block).
         *
         * @Implements LocationTag.below[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "below", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            return new LocationTag(obj.getLocation().clone().subtract(0, distance, 0));
        });

        /* @doc tag
         *
         * @Name forward[]
         * @RawName <LocationTag.forward[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved forward in the direction this location is facing by the specified distance.
         *
         * @Implements LocationTag.forward[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "forward", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            return new LocationTag(obj.getLocation().clone().add(obj.getLocation().getDirection().multiply(distance)));
        });

        /* @doc tag
         *
         * @Name backward[]
         * @RawName <LocationTag.backward[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved backward relative to the direction this location is facing by the specified distance.
         *
         * @Implements LocationTag.backward[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "backward", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            return new LocationTag(obj.getLocation().clone().subtract(obj.getLocation().getDirection().multiply(distance)));
        });

        /* @doc tag
         *
         * @Name forwardFlat[]
         * @RawName <LocationTag.forwardFlat[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved forward based purely on its yaw (ignoring pitch and Y-axis) by the specified distance.
         *
         * @Implements LocationTag.forward_flat[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "forwardFlat", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            Location loc = obj.getLocation().clone();
            loc.setPitch(0);
            return new LocationTag(obj.getLocation().clone().add(loc.getDirection().multiply(distance)));
        });

        /* @doc tag
         *
         * @Name backwardFlat[]
         * @RawName <LocationTag.backwardFlat[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved backward based purely on its yaw (ignoring pitch and Y-axis) by the specified distance.
         *
         * @Implements LocationTag.backward_flat[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "backwardFlat", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            Location loc = obj.getLocation().clone();
            loc.setPitch(0);
            return new LocationTag(obj.getLocation().clone().subtract(loc.getDirection().multiply(distance)));
        });

        /* @doc tag
         *
         * @Name left[]
         * @RawName <LocationTag.left[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved to the left relative to its current facing direction by the specified distance.
         *
         * @Implements LocationTag.left[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "left", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            Location loc = obj.getLocation().clone();
            loc.setPitch(0);
            return new LocationTag(loc.add(loc.getDirection().rotateAroundY(Math.PI / 2).multiply(distance)));
        });

        /* @doc tag
         *
         * @Name right[]
         * @RawName <LocationTag.right[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved to the right relative to its current facing direction by the specified distance.
         *
         * @Implements LocationTag.right[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "right", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            Location loc = obj.getLocation().clone();
            loc.setPitch(0);
            return new LocationTag(loc.subtract(loc.getDirection().rotateAroundY(Math.PI / 2).multiply(distance)));
        });

        /* @doc tag
         *
         * @Name up[]
         * @RawName <LocationTag.up[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved mathematically 'up' relative to its current pitch and yaw orientations by the specified distance.
         *
         * @Implements LocationTag.up[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "up", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            Location loc = obj.getLocation().clone();
            loc.setPitch(loc.getPitch() - 90);
            return new LocationTag(obj.getLocation().clone().add(loc.getDirection().multiply(distance)));
        });

        /* @doc tag
         *
         * @Name down[]
         * @RawName <LocationTag.down[(<#.#>)]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @Description
         * Returns a new location moved mathematically 'down' relative to its current pitch and yaw orientations by the specified distance.
         *
         * @Implements LocationTag.down[(<#.#>)]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "down", (attr, obj) -> {
            double distance = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 1.0;
            Location loc = obj.getLocation().clone();
            loc.setPitch(loc.getPitch() - 90);
            return new LocationTag(obj.getLocation().clone().subtract(loc.getDirection().multiply(distance)));
        });

        /* @doc tag
         *
         * @Name center
         * @RawName <LocationTag.center>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a new location perfectly centered in the middle of its corresponding block coordinates (X+0.5, Y+0.5, Z+0.5).
         *
         * @Implements LocationTag.center
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "center", (attr, obj) -> {
            Location loc = obj.getLocation();
            return new LocationTag(new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5));
        });

        /* @doc tag
         *
         * @Name directionTo[]
         * @RawName <LocationTag.directionTo[<location>]>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Description
         * Returns the compass direction (North, East, South, West) pointing from this location towards the specified target location.
         *
         * @Implements LocationTag.direction[(<location>)]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "directionTo", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;

            Vector dir = target.getLocation().toVector().subtract(obj.getLocation().toVector());
            if (dir.lengthSquared() == 0) return new ElementTag("none");
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));

            yaw = yaw % 360;
            if (yaw < 0) yaw += 360;
            if (yaw >= 315 || yaw < 45) return new ElementTag("South");
            if (yaw >= 45 && yaw < 135) return new ElementTag("West");
            if (yaw >= 135 && yaw < 225) return new ElementTag("North");
            return new ElementTag("East");
        }).test("1,1,4");

        /* @doc tag
         *
         * @Name directionVector
         * @RawName <LocationTag.directionVector>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a new location representing the 1-length directional vector of this location's pitch and yaw.
         *
         * @Implements LocationTag.direction.vector
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "directionVector", (attr, obj) ->
                new LocationTag(new Location(obj.getLocation().getWorld(),
                        obj.getLocation().getDirection().getX(),
                        obj.getLocation().getDirection().getY(),
                        obj.getLocation().getDirection().getZ()))
        );

        /* @doc tag
         *
         * @Name yawTo[]
         * @RawName <LocationTag.yawTo[<location>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Calculates and returns the precise yaw angle required to face the target location from this location.
         *
         * @Implements LocationTag.direction[<location>].yaw
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "yawTo", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;
            Vector dir = target.getLocation().toVector().subtract(obj.getLocation().toVector());
            if (dir.lengthSquared() == 0) return new ElementTag(0);
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
            yaw = yaw % 360;
            if (yaw < 0) yaw += 360;
            return new ElementTag(yaw);
        }).test("2,3,4");

        /* @doc tag
         *
         * @Name pitchTo[]
         * @RawName <LocationTag.pitchTo[<location>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Calculates and returns the precise pitch angle required to face the target location from this location.
         *
         * @Implements LocationTag.direction[<location>].pitch
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "pitchTo", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;
            Vector dir = target.getLocation().toVector().subtract(obj.getLocation().toVector());
            if (dir.lengthSquared() == 0) return new ElementTag(0);
            double xz = Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ());
            return new ElementTag((float) Math.toDegrees(Math.atan2(-dir.getY(), xz)));
        }).test("2,3,4");

        /* @doc tag
         *
         * @Name face[]
         * @RawName <LocationTag.face[<location>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of this location with its pitch and yaw modified to perfectly look at the specified target location.
         *
         * @Implements LocationTag.face[<location>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "face", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;
            Location loc = obj.getLocation().clone();
            loc.setDirection(target.getLocation().toVector().subtract(loc.toVector()));
            return new LocationTag(loc, false);
        }).test("1,2,3");

        /* @doc tag
         *
         * @Name isFacing[]
         * @RawName <LocationTag.isFacing[<location>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Checks if this location's direction is currently facing towards the target location.
         * By default, this uses a 45-degree cone tolerance.
         *
         * @Implements LocationTag.facing[<location>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isFacing", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;

            double degrees = 45.0;

            /* @doc tag
             *
             * @Name isFacing[].degrees[]
             * @RawName <LocationTag.isFacing[<location>].degrees[<#.#>]>
             * @Object LocationTag
             * @ReturnType ElementTag(Boolean)
             * @ArgRequired
             * @Description
             * Specifies the exact angle tolerance (cone width) in degrees for the facing check.
             *
             * @Implements LocationTag.facing[<location>].degrees[<#.#>]
             */
            if (attr.matchesNext("degrees") && attr.hasNextParam()) {
                degrees = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);
            }

            Location l1 = obj.getLocation();
            Location l2 = target.getLocation();

            double dx = l2.getX() - l1.getX();
            double dy = l2.getY() - l1.getY();
            double dz = l2.getZ() - l1.getZ();

            double lenSq = dx * dx + dy * dy + dz * dz;
            if (lenSq < 1e-12) return new ElementTag(true);

            double len = Math.sqrt(lenSq);
            dx /= len;
            dy /= len;
            dz /= len;

            double rotX = Math.toRadians(l1.getYaw());
            double rotY = Math.toRadians(l1.getPitch());
            double xz = Math.cos(rotY);

            double dirX = -xz * Math.sin(rotX);
            double dirY = -Math.sin(rotY);
            double dirZ = xz * Math.cos(rotX);

            double dot = (dx * dirX) + (dy * dirY) + (dz * dirZ);
            double threshold = Math.cos(Math.toRadians(degrees));

            return new ElementTag(dot >= threshold);
        }).test("3,3,3");

        /* @doc tag
         *
         * @Name hasLineOfSight[]
         * @RawName <LocationTag.hasLineOfSight[<location>]>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Checks if there is a clear, unobstructed line of sight from this location to the target location.
         *
         * @Implements LocationTag.line_of_sight[<location>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasLineOfSight", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;
            World world = obj.getLocation().getWorld();
            if (world == null || target.getLocation().getWorld() != world) return new ElementTag(false);

            BukkitSchedulerAdapter.requireRegion(obj.getLocation());

            Vector dir = target.getLocation().toVector().subtract(obj.getLocation().toVector());
            double distance = dir.length();
            if (distance == 0) return new ElementTag(true);
            dir.normalize();

            RayTraceResult result = world.rayTraceBlocks(obj.getLocation(), dir, distance, FluidCollisionMode.NEVER, true);
            return new ElementTag(result == null || result.getHitBlock() == null);
        }).test("1,1,6");

        /* @doc tag
         *
         * @Name pointsAroundX[]
         * @RawName <LocationTag.pointsAroundX[<radius>,<points>]>
         * @Object LocationTag
         * @ReturnType ListTag(LocationTag)
         * @ArgRequired
         * @Description
         * Returns a list of locations forming a circle rotating around the X-axis, calculated using the provided radius and point count.
         *
         * @Implements LocationTag.points_around_x[radius=<#.#>;points=<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "pointsAroundX", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String[] split = attr.getParam().split(",");
            if (split.length < 2) return null;
            double radius = new ElementTag(split[0]).asDouble();
            int points = new ElementTag(split[1]).asInt();

            ListTag list = new ListTag();
            double angle = 2 * Math.PI / points;
            for (int i = 0; i < points; i++) {
                double y = radius * Math.cos(angle * i);
                double z = radius * Math.sin(angle * i);
                list.addObject(new LocationTag(obj.getLocation().clone().add(0, y, z)));
            }
            return list;
        }).test("1,4");

        /* @doc tag
         *
         * @Name pointsAroundY[]
         * @RawName <LocationTag.pointsAroundY[<radius>,<points>]>
         * @Object LocationTag
         * @ReturnType ListTag(LocationTag)
         * @ArgRequired
         * @Description
         * Returns a list of locations forming a circle rotating around the Y-axis, calculated using the provided radius and point count.
         *
         * @Implements LocationTag.points_around_y[radius=<#.#>;points=<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "pointsAroundY", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String[] split = attr.getParam().split(",");
            if (split.length < 2) return null;
            double radius = new ElementTag(split[0]).asDouble();
            int points = new ElementTag(split[1]).asInt();

            ListTag list = new ListTag();
            double angle = 2 * Math.PI / points;
            for (int i = 0; i < points; i++) {
                double x = radius * Math.cos(angle * i);
                double z = radius * Math.sin(angle * i);
                list.addObject(new LocationTag(obj.getLocation().clone().add(x, 0, z)));
            }
            return list;
        }).test("1,4");

        /* @doc tag
         *
         * @Name pointsAroundZ[]
         * @RawName <LocationTag.pointsAroundZ[<radius>,<points>]>
         * @Object LocationTag
         * @ReturnType ListTag(LocationTag)
         * @ArgRequired
         * @Description
         * Returns a list of locations forming a circle rotating around the Z-axis, calculated using the provided radius and point count.
         *
         * @Implements LocationTag.points_around_z[radius=<#.#>;points=<#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "pointsAroundZ", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String[] split = attr.getParam().split(",");
            if (split.length < 2) return null;
            double radius = new ElementTag(split[0]).asDouble();
            int points = new ElementTag(split[1]).asInt();

            ListTag list = new ListTag();
            double angle = 2 * Math.PI / points;
            for (int i = 0; i < points; i++) {
                double x = radius * Math.cos(angle * i);
                double y = radius * Math.sin(angle * i);
                list.addObject(new LocationTag(obj.getLocation().clone().add(x, y, 0)));
            }
            return list;
        }).test("1,4");

        /* @doc tag
         *
         * @Name pointsBetween[]
         * @RawName <LocationTag.pointsBetween[<location>]>
         * @Object LocationTag
         * @ReturnType ListTag(LocationTag)
         * @ArgRequired
         * @Description
         * Calculates and returns a list of locations spread evenly in a straight line between this location and the target.
         * The default distance between each point is 1 block.
         *
         * @Implements LocationTag.points_between[<location>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "pointsBetween", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag target = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (target == null) return null;

            double distance = 1.0;

            /* @doc tag
             *
             * @Name pointsBetween[].distance[]
             * @RawName <LocationTag.pointsBetween[<location>].distance[<#.#>]>
             * @Object LocationTag
             * @ReturnType ListTag(LocationTag)
             * @ArgRequired
             * @Description
             * Specifies the exact spacing (in blocks) between each generated point in the line.
             *
             * @Implements LocationTag.points_between[<location>].distance[<#.#>]
             */
            if (attr.matchesNext("distance") && attr.hasNextParam()) {
                distance = new ElementTag(attr.getNextParam()).asDouble();
                if (distance <= 0) distance = 1.0;
                attr.fulfill(1);
            }

            ListTag list = new ListTag();
            Location l1 = obj.getLocation();
            Location l2 = target.getLocation();

            double dx = l2.getX() - l1.getX();
            double dy = l2.getY() - l1.getY();
            double dz = l2.getZ() - l1.getZ();

            double lenSq = dx * dx + dy * dy + dz * dz;
            if (lenSq < 1e-12) return list;

            double len = Math.sqrt(lenSq);
            World w = l1.getWorld();
            float yaw = l1.getYaw();
            float pitch = l1.getPitch();

            double stepRatioX = dx / len;
            double stepRatioY = dy / len;
            double stepRatioZ = dz / len;

            for (double i = 0; i <= len; i += distance) {
                double currentX = l1.getX() + (stepRatioX * i);
                double currentY = l1.getY() + (stepRatioY * i);
                double currentZ = l1.getZ() + (stepRatioZ * i);

                list.addObject(new LocationTag(new Location(w, currentX, currentY, currentZ, yaw, pitch)));
            }
            return list;
        }).test("l@1,3,1,world");

        /* @doc tag
         *
         * @Name randomOffset[]
         * @RawName <LocationTag.randomOffset[<limit>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of this location randomly offset by a maximum value.
         * Input can be a single number (applied equally to all axes) or a LocationTag vector for individual axis limits.
         *
         * @Implements LocationTag.random_offset[<limit>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "randomOffset", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Vector limit;
            LocationTag limitTag = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (limitTag != null) {
                limit = limitTag.getLocation().toVector();
            } else {
                double val = new ElementTag(attr.getParam()).asDouble();
                limit = new Vector(val, val, val);
            }

            ThreadLocalRandom r = ThreadLocalRandom.current();
            double ox = (r.nextDouble() * 2 - 1) * limit.getX();
            double oy = (r.nextDouble() * 2 - 1) * limit.getY();
            double oz = (r.nextDouble() * 2 - 1) * limit.getZ();
            return new LocationTag(obj.getLocation().clone().add(ox, oy, oz));
        }).test("3");

        /* @doc tag
         *
         * @Name relative[]
         * @RawName <LocationTag.relative[<location>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a new location offset relative to this location's facing direction.
         * The input vector represents left (X), up (Y), and forward (Z) movements.
         *
         * @Implements LocationTag.relative[<location>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "relative", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag offsetLoc = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (offsetLoc == null) return null;

            Location loc = obj.getLocation().clone();
            Vector offset = loc.getDirection().multiply(offsetLoc.getLocation().getZ());
            loc.setPitch(loc.getPitch() - 90);
            offset.add(loc.getDirection().multiply(offsetLoc.getLocation().getY()));
            loc.setPitch(0);
            offset.add(loc.getDirection().rotateAroundY(Math.PI / 2).multiply(offsetLoc.getLocation().getX()));

            return new LocationTag(obj.getLocation().clone().add(offset));
        }).test("1,-1,2");

        /* @doc tag
         *
         * @Name rotateAroundX[]
         * @RawName <LocationTag.rotateAroundX[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns this location vector rotated mathematically around the X-axis by the specified angle in radians.
         *
         * @Implements LocationTag.rotate_around_x[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "rotateAroundX", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double angle = new ElementTag(attr.getParam()).asDouble();
            double cos = Math.cos(angle), sin = Math.sin(angle);
            Location loc = obj.getLocation().clone();
            double y = loc.getY(), z = loc.getZ();
            loc.setY((y * cos) - (z * sin));
            loc.setZ((y * sin) + (z * cos));
            return new LocationTag(loc, false);
        }).test("1,2,1");

        /* @doc tag
         *
         * @Name rotateAroundY[]
         * @RawName <LocationTag.rotateAroundY[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns this location vector rotated mathematically around the Y-axis by the specified angle in radians.
         *
         * @Implements LocationTag.rotate_around_y[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "rotateAroundY", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double angle = new ElementTag(attr.getParam()).asDouble();
            double cos = Math.cos(angle), sin = Math.sin(angle);
            Location loc = obj.getLocation().clone();
            double x = loc.getX(), z = loc.getZ();
            loc.setX((x * cos) + (z * sin));
            loc.setZ((x * -sin) + (z * cos));
            return new LocationTag(loc, false);
        }).test("1,2,1");

        /* @doc tag
         *
         * @Name rotateAroundZ[]
         * @RawName <LocationTag.rotateAroundZ[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns this location vector rotated mathematically around the Z-axis by the specified angle in radians.
         *
         * @Implements LocationTag.rotate_around_z[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "rotateAroundZ", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double angle = new ElementTag(attr.getParam()).asDouble();
            double cos = Math.cos(angle), sin = Math.sin(angle);
            Location loc = obj.getLocation().clone();
            double x = loc.getX(), y = loc.getY();
            loc.setX((x * cos) - (y * sin));
            loc.setY((x * sin) + (y * cos));
            return new LocationTag(loc, false);
        }).test("1,2,1");

        /* @doc tag
         *
         * @Name rotatePitch[]
         * @RawName <LocationTag.rotatePitch[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of this location with its pitch rotated by the specified angle amount, capped naturally between -90 and 90 degrees.
         *
         * @Implements LocationTag.rotate_pitch[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "rotatePitch", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Location loc = obj.getLocation().clone();
            float pitch = loc.getPitch() + ((float) new ElementTag(attr.getParam()).asDouble());
            loc.setPitch(Math.max(-90, Math.min(90, pitch)));
            return new LocationTag(loc, false);
        }).test("22");

        /* @doc tag
         *
         * @Name rotateYaw[]
         * @RawName <LocationTag.rotateYaw[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of this location with its yaw rotated mathematically by the specified angle amount.
         *
         * @Implements LocationTag.rotate_yaw[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "rotateYaw", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Location loc = obj.getLocation().clone();
            loc.setYaw(loc.getYaw() + ((float) new ElementTag(attr.getParam()).asDouble()));
            return new LocationTag(loc, false);
        }).test("14");

        /* @doc tag
         *
         * @Name round
         * @RawName <LocationTag.round>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a copy of this location with all spatial coordinates, yaw, and pitch mathematically rounded to the nearest whole number.
         *
         * @Implements LocationTag.round
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "round", (attr, obj) -> {
            Location loc = obj.getLocation().clone();
            loc.setX(Math.round(loc.getX()));
            loc.setY(Math.round(loc.getY()));
            loc.setZ(Math.round(loc.getZ()));
            loc.setYaw(Math.round(loc.getYaw()));
            loc.setPitch(Math.round(loc.getPitch()));
            return new LocationTag(loc, false);
        });

        /* @doc tag
         *
         * @Name floor
         * @RawName <LocationTag.floor>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a copy of this location with all spatial coordinates, yaw, and pitch rounded completely down (floored).
         *
         * @Implements LocationTag.round_down
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "floor", (attr, obj) -> {
            Location loc = obj.getLocation().clone();
            loc.setX(Math.floor(loc.getX()));
            loc.setY(Math.floor(loc.getY()));
            loc.setZ(Math.floor(loc.getZ()));
            loc.setYaw((float) Math.floor(loc.getYaw()));
            loc.setPitch((float) Math.floor(loc.getPitch()));
            return new LocationTag(loc, false);
        });

        /* @doc tag
         *
         * @Name ceil
         * @RawName <LocationTag.ceil>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a copy of this location with all spatial coordinates, yaw, and pitch rounded completely up (ceiling).
         *
         * @Implements LocationTag.round_up
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "ceil", (attr, obj) -> {
            Location loc = obj.getLocation().clone();
            loc.setX(Math.ceil(loc.getX()));
            loc.setY(Math.ceil(loc.getY()));
            loc.setZ(Math.ceil(loc.getZ()));
            loc.setYaw((float) Math.ceil(loc.getYaw()));
            loc.setPitch((float) Math.ceil(loc.getPitch()));
            return new LocationTag(loc, false);
        });

        /* @doc tag
         *
         * @Name roundTo[]
         * @RawName <LocationTag.roundTo[<#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of this location with all coordinates rounded dynamically to the specified number of decimal places.
         *
         * @Implements LocationTag.round_to[<#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "roundTo", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int places = new ElementTag(attr.getParam()).asInt();
            double scale = Math.pow(10, places);
            Location loc = obj.getLocation().clone();
            loc.setX(Math.round(loc.getX() * scale) / scale);
            loc.setY(Math.round(loc.getY() * scale) / scale);
            loc.setZ(Math.round(loc.getZ() * scale) / scale);
            loc.setYaw((float) (Math.round(loc.getYaw() * scale) / scale));
            loc.setPitch((float) (Math.round(loc.getPitch() * scale) / scale));
            return new LocationTag(loc, false);
        }).test("2");

        /* @doc tag
         *
         * @Name roundToPrecision[]
         * @RawName <LocationTag.roundToPrecision[<#.#>]>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Returns a copy of this location with all coordinates mathematically rounded to the nearest multiple of the specified precision value.
         *
         * @Implements LocationTag.round_to_precision[<#.#>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "roundToPrecision", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double precision = new ElementTag(attr.getParam()).asDouble();
            if (precision == 0) return new LocationTag(obj.getLocation());
            double scale = 1.0 / precision;
            Location loc = obj.getLocation().clone();
            loc.setX(Math.round(loc.getX() * scale) / scale);
            loc.setY(Math.round(loc.getY() * scale) / scale);
            loc.setZ(Math.round(loc.getZ() * scale) / scale);
            loc.setYaw((float) (Math.round(loc.getYaw() * scale) / scale));
            loc.setPitch((float) (Math.round(loc.getPitch() * scale) / scale));
            return new LocationTag(loc, false);
        }).test("0.05");

        /* @doc tag
         *
         * @Name simplex3d
         * @RawName <LocationTag.simplex3d>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the 3D simplex noise value (from -1 to 1) for this location's exact X/Y/Z coordinates.
         *
         * @Implements LocationTag.simplex_3d
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "simplex3d", (attr, obj) -> {
            Location loc = obj.getLocation();
            return new ElementTag(SimplexNoiseGenerator.getNoise(loc.getX(), loc.getY(), loc.getZ()));
        });

        /* @doc tag
         *
         * @Name attachedTo
         * @RawName <LocationTag.attachedTo>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the location of the block this block is physically attached to (e.g. the wall behind a torch or the ceiling above a bell).
         *
         * @Implements LocationTag.attached_to
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "attachedTo", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            BlockData data = loc.getBlock().getBlockData();
            BlockFace face = null;

            if (data instanceof Directional dir) {
                face = dir.getFacing().getOppositeFace();
                if (data instanceof Switch sw) {
                    face = switch (sw.getAttachedFace()) {
                        case CEILING -> BlockFace.UP;
                        case FLOOR -> BlockFace.DOWN;
                        case WALL -> dir.getFacing().getOppositeFace();
                    };
                } else if (data instanceof org.bukkit.block.data.type.Bell bell) {
                    face = switch (bell.getAttachment()) {
                        case CEILING -> BlockFace.UP;
                        case FLOOR -> BlockFace.DOWN;
                        case SINGLE_WALL, DOUBLE_WALL -> dir.getFacing().getOppositeFace();
                    };
                }
            } else if (data instanceof FaceAttachable attachable) {
                face = switch (attachable.getAttachedFace()) {
                    case CEILING -> BlockFace.UP;
                    case FLOOR -> BlockFace.DOWN;
                    case WALL -> BlockFace.SELF;
                };
            }

            if (face != null && face != BlockFace.SELF) {
                return new LocationTag(new Location(loc.getWorld(), loc.getX() + face.getModX(), loc.getY() + face.getModY(), loc.getZ() + face.getModZ()));
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name beaconPrimaryEffect
         * @RawName <LocationTag.beaconPrimaryEffect>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the primary potion effect name currently active on this beacon block.
         *
         * @Implements LocationTag.beacon_primary_effect
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "beaconPrimaryEffect", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Beacon beacon && beacon.getPrimaryEffect() != null) {
                return new ElementTag(beacon.getPrimaryEffect().getType().getKey().getKey());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name beaconSecondaryEffect
         * @RawName <LocationTag.beaconSecondaryEffect>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the secondary potion effect name currently active on this beacon block.
         *
         * @Implements LocationTag.beacon_secondary_effect
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "beaconSecondaryEffect", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Beacon beacon && beacon.getSecondaryEffect() != null) {
                return new ElementTag(beacon.getSecondaryEffect().getType().getKey().getKey());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name age
         * @RawName <LocationTag.age>
         * @Object LocationTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description
         * Returns the age of an End Gateway block in ticks.
         *
         * @Implements LocationTag.age
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "age", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof EndGateway gateway) {
                return new DurationTag(gateway.getAge());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name beaconTier
         * @RawName <LocationTag.beaconTier>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the tier level (0-4) of the beacon pyramid at this location.
         *
         * @Implements LocationTag.beacon_tier
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "beaconTier", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Beacon beacon) {
                return new ElementTag(beacon.getTier());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name blockFacing
         * @RawName <LocationTag.blockFacing>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a location vector (e.g. 1,0,0) representing the direction this directional block is facing.
         *
         * @Implements LocationTag.block_facing
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "blockFacing", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getBlockData() instanceof Directional dir) {
                BlockFace face = dir.getFacing();
                return new LocationTag(new Location(loc.getWorld(), face.getModX(), face.getModY(), face.getModZ()));
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name brewingFuelLevel
         * @RawName <LocationTag.brewingFuelLevel>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the current fuel level of the brewing stand at this location.
         *
         * @Implements LocationTag.brewing_fuel_level
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "brewingFuelLevel", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof BrewingStand stand) {
                return new ElementTag(stand.getFuelLevel());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name brewingTime
         * @RawName <LocationTag.brewingTime>
         * @Object LocationTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description
         * Returns the remaining brewing time of the brewing stand at this location.
         *
         * @Implements LocationTag.brewing_time
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "brewingTime", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof BrewingStand stand) {
                return new DurationTag(stand.getBrewingTime());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name campfireItems
         * @RawName <LocationTag.campfireItems>
         * @Object LocationTag
         * @ReturnType ListTag(ItemTag)
         * @NoArg
         * @Description
         * Returns a list of all items currently cooking in the campfire. Empty slots will return air.
         *
         * @Implements LocationTag.campfire_items
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "campfireItems", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Campfire campfire) {
                ListTag list = new ListTag();
                for (int i = 0; i < campfire.getSize(); i++) {
                    ItemStack item = campfire.getItem(i);
                    list.addObject(new ItemTag(item != null ? item : new ItemStack(Material.AIR)));
                }
                return list;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name command
         * @RawName <LocationTag.command>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the command string stored inside this command block.
         *
         * @Implements LocationTag.command_block
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "command", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CommandBlock cmdBlock) {
                return new ElementTag(cmdBlock.getCommand());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name commandBlockName
         * @RawName <LocationTag.commandBlockName>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the assigned name of this command block.
         *
         * @Implements LocationTag.command_block_name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "commandBlockName", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CommandBlock cmdBlock) {
                return new ElementTag(cmdBlock.name());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name customName
         * @RawName <LocationTag.customName>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the custom name of this block, if it is a nameable container or block (e.g. named chest).
         *
         * @Implements LocationTag.custom_name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "customName", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Nameable nameable && nameable.customName() != null) {
                return new ElementTag(nameable.customName());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name disabledSlots
         * @RawName <LocationTag.disabledSlots>
         * @Object LocationTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Description
         * Returns a list of disabled slot indexes (1-9) for a crafter block.
         *
         * @Implements LocationTag.disabled_slots
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "disabledSlots", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Crafter crafter) {
                ListTag list = new ListTag();
                for (int i = 0; i < 9; i++) {
                    if (crafter.isSlotDisabled(i)) list.addObject(new ElementTag(i + 1));
                }
                return list;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name drops[]
         * @RawName <LocationTag.drops[(<item>)]>
         * @Object LocationTag
         * @ReturnType ListTag(ItemTag)
         * @Description
         * Returns a list of items this block would drop if broken naturally, optionally providing the tool used.
         *
         * @Implements LocationTag.drops[(<item>)]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "drops", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            ItemStack tool = null;
            if (attr.hasParam()) {
                ItemTag itemTag = attr.getParamObject(ItemTag.class, ItemTag::new);
                if (itemTag != null) tool = itemTag.getItemStack();
            }

            ListTag list = new ListTag();
            for (ItemStack drop : loc.getBlock().getDrops(tool)) {
                list.addObject(new ItemTag(drop));
            }
            return list;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name exitLocation
         * @RawName <LocationTag.exitLocation>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the destination exit location of an End Gateway.
         *
         * @Implements LocationTag.exit_location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "exitLocation", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof EndGateway gateway && gateway.getExitLocation() != null) {
                return new LocationTag(gateway.getExitLocation());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name facingBlocks[]
         * @RawName <LocationTag.facingBlocks[(<#>)]>
         * @Object LocationTag
         * @ReturnType ListTag(LocationTag)
         * @Description
         * Returns a list of block locations directly in the line of sight of this location's facing direction.
         * The default range is 100 blocks.
         *
         * @Implements LocationTag.facing_blocks[(<#>)]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "facingBlocks", (attr, obj) -> {
            int range = attr.hasParam() ? new ElementTag(attr.getParam()).asInt() : 100;
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;

            ListTag list = new ListTag();
            BlockIterator iterator = new BlockIterator(loc, 0, Math.max(1, range));

            while (iterator.hasNext()) {
                Block nextBlock = iterator.next();

                BukkitSchedulerAdapter.requireRegion(nextBlock.getLocation());
                list.addObject(new LocationTag(new Location(loc.getWorld(), nextBlock.getX(), nextBlock.getY(), nextBlock.getZ())));
            }
            return list;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name floodFill[]
         * @RawName <LocationTag.floodFill[<limit>]>
         * @Object LocationTag
         * @ReturnType ListTag(LocationTag)
         * @ArgRequired
         * @Description
         * Returns a contiguous list of connected blocks of the same type starting from this location.
         * The parameter defines the maximum radius. Accepts an optional `.types[<matcher>]` lookahead.
         * Limited to a safe maximum of 2048 blocks to prevent server thread hangs.
         *
         * @Implements LocationTag.flood_fill[<limit>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "floodFill", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double radius = new ElementTag(attr.getParam()).asDouble();
            int maxBlocks = 2048;

            Location startLoc = obj.getLocation();
            World world = startLoc.getWorld();
            if (world == null) return null;
            BukkitSchedulerAdapter.requireRegion(startLoc);

            int startX = startLoc.getBlockX(), startY = startLoc.getBlockY(), startZ = startLoc.getBlockZ();
            Material targetMat = world.getBlockAt(startX, startY, startZ).getType();

            /* @doc tag
             *
             * @Name floodFill[].types[]
             * @RawName <LocationTag.floodFill[<limit>].types[<matcher>]>
             * @Object LocationTag
             * @ReturnType ListTag(LocationTag)
             * @ArgRequired
             * @Description
             * Returns a contiguous list of connected blocks that match the provided MaterialTag matcher.
             *
             * @Implements LocationTag.flood_fill[<limit>].types[<matcher>]
             */
            boolean hasMatcher = attr.matchesNext("types") && attr.hasNextParam();
            String matcher = hasMatcher ? attr.getNextParam() : null;
            if (hasMatcher) attr.fulfill(1);

            ListTag result = new ListTag();

            Set<Long> visited = new HashSet<>();
            long[] queue = new long[maxBlocks + 6];
            int head = 0, tail = 0;

            long startPacked = packBlockLoc(startX, startY, startZ);
            queue[tail++] = startPacked;
            visited.add(startPacked);

            int rSq = (int) (radius * radius);
            int[][] dirs = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};

            while (head < tail && result.size() < maxBlocks) {
                long curr = queue[head++];
                int cx = unpackX(curr), cy = unpackY(curr), cz = unpackZ(curr);

                Location currLoc = new Location(world, cx, cy, cz);
                BukkitSchedulerAdapter.requireRegion(currLoc);

                Block b = world.getBlockAt(cx, cy, cz);
                boolean match = hasMatcher ? new MaterialTag(b).tryAdvancedMatcher(matcher) : b.getType() == targetMat;

                if (match) {
                    result.addObject(new LocationTag(currLoc));

                    for (int[] d : dirs) {
                        int nx = cx + d[0], ny = cy + d[1], nz = cz + d[2];

                        if (ny < world.getMinHeight() || ny >= world.getMaxHeight()) continue;
                        if (((nx - startX) * (nx - startX) + (ny - startY) * (ny - startY) + (nz - startZ) * (nz - startZ)) > rSq) continue;

                        long packed = packBlockLoc(nx, ny, nz);
                        if (visited.add(packed) && tail < queue.length) {
                            queue[tail++] = packed;
                        }
                    }
                }
            }
            return result;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name furnaceBurnDuration
         * @RawName <LocationTag.furnaceBurnDuration>
         * @Object LocationTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description
         * Returns the remaining burn time of the furnace at this location.
         *
         * @Implements LocationTag.furnace_burn_duration
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "furnaceBurnDuration", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Furnace furnace) {
                return new DurationTag((long) furnace.getBurnTime());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name furnaceCookDuration
         * @RawName <LocationTag.furnaceCookDuration>
         * @Object LocationTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description
         * Returns how long the furnace at this location has been cooking its current item.
         *
         * @Implements LocationTag.furnace_cook_duration
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "furnaceCookDuration", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Furnace furnace) {
                return new DurationTag((long) furnace.getCookTime());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name furnaceCookDurationTotal
         * @RawName <LocationTag.furnaceCookDurationTotal>
         * @Object LocationTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description
         * Returns the total required cook time for the current item in this furnace.
         *
         * @Implements LocationTag.furnace_cook_duration_total
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "furnaceCookDurationTotal", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Furnace furnace) {
                return new DurationTag((long) furnace.getCookTimeTotal());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name hasInventory
         * @RawName <LocationTag.hasInventory>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the block at this location is a container with an inventory (chest, hopper, etc.).
         *
         * @Implements LocationTag.has_inventory
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasInventory", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().getState() instanceof InventoryHolder);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name hasLootTable
         * @RawName <LocationTag.hasLootTable>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the container at this location has an ungenerated vanilla loot table.
         *
         * @Implements LocationTag.has_loot_table
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasLootTable", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().getState() instanceof Lootable lootable && lootable.getLootTable() != null);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name headRotation
         * @RawName <LocationTag.headRotation>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the numerical rotation value (1-16) of a skull/head block at this location.
         *
         * @Implements LocationTag.head_rotation
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "headRotation", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getBlockData() instanceof Rotatable rotatable) {
                return new ElementTag(blockFaceToRotationIndex(rotatable.getRotation()));
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name highest
         * @RawName <LocationTag.highest>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the location of the highest solid block directly above or below this location's X and Z coordinates.
         *
         * @Implements LocationTag.highest
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "highest", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            Location highest = loc.getWorld().getHighestBlockAt(loc).getLocation();
            return new LocationTag(new Location(loc.getWorld(), highest.getX(), highest.getY(), highest.getZ()));
        }).ignoreTest();

        /* @doc tag
         *
         * @Name hiveBeeCount
         * @RawName <LocationTag.hiveBeeCount>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the current number of bees housed inside this beehive.
         *
         * @Implements LocationTag.hive_bee_count
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hiveBeeCount", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Beehive hive) {
                return new ElementTag(hive.getEntityCount());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name hiveMaxBees
         * @RawName <LocationTag.hiveMaxBees>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the maximum capacity of bees for this beehive.
         *
         * @Implements LocationTag.hive_max_bees
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hiveMaxBees", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Beehive hive) {
                return new ElementTag(hive.getMaxEntities());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isExactTeleport
         * @RawName <LocationTag.isExactTeleport>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an End Gateway block is set to exactly teleport the entity to its exit location, rather than randomly placing them nearby.
         *
         * @Implements LocationTag.is_exact_teleport
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isExactTeleport", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof EndGateway gateway) {
                return new ElementTag(gateway.isExactTeleport());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isLiquid
         * @RawName <LocationTag.isLiquid>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the block at this location is a liquid (water or lava).
         *
         * @Implements LocationTag.is_liquid
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isLiquid", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().isLiquid());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isLockable
         * @RawName <LocationTag.isLockable>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the block at this location is a container that supports vanilla locking mechanics.
         *
         * @Implements LocationTag.is_lockable
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isLockable", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().getState() instanceof Lockable);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isLocked
         * @RawName <LocationTag.isLocked>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the container at this location is currently locked with a password key.
         *
         * @Implements LocationTag.is_locked
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isLocked", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().getState() instanceof Lockable lockable && lockable.isLocked());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isPassable
         * @RawName <LocationTag.isPassable>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the block at this location has no solid collision box and can be walked through (e.g. air, tall grass, signs).
         *
         * @Implements LocationTag.is_passable
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isPassable", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().isPassable());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isSpawnable
         * @RawName <LocationTag.isSpawnable>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether this location is structurally safe to spawn a player or mob.
         * Ensures the block and the block above are passable and not liquid, and the block below is solid.
         *
         * @Implements LocationTag.is_spawnable
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isSpawnable", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            Block b = loc.getBlock();
            Block above = b.getRelative(BlockFace.UP);
            Block below = b.getRelative(BlockFace.DOWN);

            boolean safe = b.isPassable() && !b.isLiquid() &&
                    above.isPassable() && !above.isLiquid() &&
                    below.getType().isSolid();

            return new ElementTag(safe);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name isWithinBorder
         * @RawName <LocationTag.isWithinBorder>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether this location is within the bounds of the world border.
         *
         * @Implements LocationTag.is_within_border
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isWithinBorder", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;

            return new ElementTag(loc.getWorld().getWorldBorder().isInside(loc));
        }).ignoreTest();

        /* @doc tag
         *
         * @Name jukeboxIsPlaying
         * @RawName <LocationTag.jukeboxIsPlaying>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the jukebox at this location is currently playing a music disc.
         *
         * @Implements LocationTag.jukebox_is_playing
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "jukeboxIsPlaying", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Jukebox jukebox) {
                return new ElementTag(jukebox.isPlaying());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name jukeboxRecord
         * @RawName <LocationTag.jukeboxRecord>
         * @Object LocationTag
         * @ReturnType ItemTag
         * @NoArg
         * @Description
         * Returns the music disc item currently inserted into the jukebox. Returns air if empty.
         *
         * @Implements LocationTag.jukebox_record
         */
        TAG_PROCESSOR.registerTag(ItemTag.class, "jukeboxRecord", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Jukebox jukebox) {
                ItemStack record = jukebox.getRecord();
                return new ItemTag(record);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name page
         * @RawName <LocationTag.page>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the page number (starting from 1) currently opened on a lectern.
         *
         * @Implements LocationTag.page
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "page", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Lectern lectern) {
                return new ElementTag(lectern.getPage() + 1);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name light
         * @RawName <LocationTag.light>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the overall light level (0-15) at this location.
         * Can use `.sky` or `.blocks` to get specific light sources.
         *
         * @Implements LocationTag.light
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "light", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            Block b = loc.getBlock();

            /* @doc tag
             *
             * @Name light.sky
             * @RawName <LocationTag.light.sky>
             * @Object LocationTag
             * @ReturnType ElementTag(Number)
             * @NoArg
             * @Description
             * Returns the amount of light emitted solely from the sky at this location.
             *
             * @Implements LocationTag.light.sky
             */
            if (attr.matchesNext("sky")) {
                attr.fulfill(1);
                return new ElementTag(b.getLightFromSky());
            }

            /* @doc tag
             *
             * @Name light.blocks
             * @RawName <LocationTag.light.blocks>
             * @Object LocationTag
             * @ReturnType ElementTag(Number)
             * @NoArg
             * @Description
             * Returns the amount of light emitted by nearby light-emitting blocks at this location.
             *
             * @Implements LocationTag.light.blocks
             */
            if (attr.matchesNext("blocks")) {
                attr.fulfill(1);
                return new ElementTag(b.getLightFromBlocks());
            }

            return new ElementTag(b.getLightLevel());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name password
         * @RawName <LocationTag.password>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the password string (item name required to open) for a locked container.
         *
         * @Implements LocationTag.password
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "password", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Lockable lockable && lockable.isLocked()) {
                return new ElementTag(lockable.getLock());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name lootTableId
         * @RawName <LocationTag.lootTableId>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the NamespacedKey of the vanilla loot table assigned to this container.
         *
         * @Implements LocationTag.loot_table_id
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "lootTableId", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Lootable lootable && lootable.getLootTable() != null) {
                return new ElementTag(lootable.getLootTable().getKey().toString());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name mapColor
         * @RawName <LocationTag.mapColor>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the map color of the block at this location, formatted as an R,G,B string.
         *
         * @Implements LocationTag.map_color
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mapColor", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            org.bukkit.Color color = loc.getBlock().getBlockData().getMapColor();
            return new ElementTag(color.getRed() + "," + color.getGreen() + "," + color.getBlue());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name otherBlock
         * @RawName <LocationTag.otherBlock>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the location of the other half of a double-block structure (e.g. doors, beds, tall plants, double chests).
         *
         * @Implements LocationTag.other_block
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "otherBlock", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            BlockData data = loc.getBlock().getBlockData();
            int modX = 0, modY = 0, modZ = 0;

            switch (data) {
                case Bisected bisected -> modY = bisected.getHalf() == Bisected.Half.TOP ? -1 : 1;
                case org.bukkit.block.data.type.Bed bed -> {
                    BlockFace face = bed.getFacing();
                    if (bed.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD) face = face.getOppositeFace();
                    modX = face.getModX();
                    modZ = face.getModZ();
                }
                case org.bukkit.block.data.type.Chest chest -> {
                    BlockFace face = chest.getFacing();
                    if (chest.getType() == org.bukkit.block.data.type.Chest.Type.LEFT) {
                        modX = face.getModZ();
                        modZ = -face.getModX();
                    } else if (chest.getType() == org.bukkit.block.data.type.Chest.Type.RIGHT) {
                        modX = -face.getModZ();
                        modZ = face.getModX();
                    }
                }
                default -> {
                }
            }

            if (modX != 0 || modY != 0 || modZ != 0) {
                return new LocationTag(new Location(loc.getWorld(), loc.getX() + modX, loc.getY() + modY, loc.getZ() + modZ));
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name patterns
         * @RawName <LocationTag.patterns>
         * @Object LocationTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Description
         * Returns a list of the patterns applied to this banner block.
         * Formatted as: "COLOR/PATTERN_TYPE".
         *
         * @Implements LocationTag.patterns
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "patterns", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            Registry<PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
            if (loc.getBlock().getState() instanceof Banner banner) {
                ListTag list = new ListTag();
                for (Pattern pattern : banner.getPatterns()) {
                    list.addObject(new ElementTag(Objects.requireNonNull(registry.getKey(pattern.getPattern())).getKey()));
                }
                return list;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name power
         * @RawName <LocationTag.power>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the current redstone power level (0-15) of the block at this location.
         *
         * @Implements LocationTag.power
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "power", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            return new ElementTag(loc.getBlock().getBlockPower());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name rayTrace[]
         * @RawName <LocationTag.rayTrace[(range=<#.#>/{200});(return=<{precise}/block/normal>);(default=<{null}/air>);(fluids=<true/{false}>);(nonsolids=<true/{false}>);(entities=<matcher>);(ignore=<entity>|...);(raysize=<#.#>/{0});(pierce=<true/{false}>)]>
         * @Object LocationTag
         * @ReturnType LocationTag, ListTag(LocationTag)
         * @ArgRequired
         * @Description
         * Traces a line from this location towards its facing direction, returning the hit location.
         *
         * Parameters (parsed as a MapTag):
         * - range: Max distance to trace (default: 200)
         * - return: 'precise' (exact hit coords), 'block' (center of hit block), or 'normal' (the face of the block hit).
         * - default: What to return if nothing is hit. 'null' or 'air' (end of the ray).
         * - fluids: Whether to hit fluids like water.
         * - nonsolids: Whether to hit passable blocks like tall grass.
         * - entities: Entity matcher to check for entity hits (leave empty to ignore entities).
         * - ignore: A list of EntityTags to ignore during the trace (separated by '|').
         * - raysize: Expands the hitboxes of entities to make them easier to hit.
         * - pierce: If 'true', the ray will pierce through EVERYTHING and return a ListTag of ALL impact locations in exact mathematical order!
         *
         * @Implements LocationTag.ray_trace[(range=<#.#>/{200});(return=<{precise}/block/normal>);(default=<{null}/air>);(fluids=<true/{false}>);(nonsolids=<true/{false}>);(entities=<matcher>);(ignore=<entity>|...);(raysize=<#.#>/{0})]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "rayTrace", (attr, obj) -> {
            if (!attr.hasParam()) return null;

            String param = attr.getParam();
            if (!param.startsWith("[")) param = "[" + param + "]";
            MapTag input = new MapTag(param);

            double range = 200.0;
            String returnMode = "precise";
            String defaultMode = "null";
            boolean fluids = false;
            boolean nonsolids = false;
            String entitiesMatcher = "";
            double raySize = 0.0;
            boolean pierce = false;
            Set<UUID> ignoreList = new HashSet<>();

            AbstractTag rangeTag = input.getObject("range");
            if (rangeTag != null) range = new ElementTag(rangeTag.identify()).asDouble();

            AbstractTag returnTag = input.getObject("return");
            if (returnTag != null) returnMode = returnTag.identify().toLowerCase();

            AbstractTag defaultTag = input.getObject("default");
            if (defaultTag != null) defaultMode = defaultTag.identify().toLowerCase();

            AbstractTag fluidsTag = input.getObject("fluids");
            if (fluidsTag != null) fluids = new ElementTag(fluidsTag.identify()).asBoolean();

            AbstractTag nonsolidsTag = input.getObject("nonsolids");
            if (nonsolidsTag != null) nonsolids = new ElementTag(nonsolidsTag.identify()).asBoolean();

            AbstractTag entitiesTag = input.getObject("entities");
            if (entitiesTag != null) entitiesMatcher = entitiesTag.identify();

            AbstractTag raysizeTag = input.getObject("raysize");
            if (raysizeTag != null) raySize = new ElementTag(raysizeTag.identify()).asDouble();

            AbstractTag pierceTag = input.getObject("pierce");
            if (pierceTag != null) pierce = new ElementTag(pierceTag.identify()).asBoolean();

            AbstractTag ignoreTag = input.getObject("ignore");
            if (ignoreTag != null) {
                for (String e : ObjectFetcher.splitIgnoringBrackets(ignoreTag.identify(), '|')) {
                    AbstractTag parsed = ObjectFetcher.pickObject(e);
                    if (parsed instanceof EntityTag et) {
                        ignoreList.add(et.getEntity().getUniqueId());
                    }
                }
            }

            Location start = obj.getLocation();
            World world = start.getWorld();
            if (world == null) return null;
            BukkitSchedulerAdapter.requireRegion(start);

            Vector startVec = start.toVector();
            Vector dirVec = start.getDirection();
            String finalReturnMode = returnMode;

            Function<RayTraceResult, LocationTag> mapHit = hit -> {
                if ("block".equals(finalReturnMode)) {
                    if (hit.getHitBlock() != null) return new LocationTag(hit.getHitBlock().getLocation());
                    if (hit.getHitEntity() != null) return new LocationTag(hit.getHitEntity().getLocation());
                } else if ("normal".equals(finalReturnMode)) {
                    if (hit.getHitBlockFace() != null) return new LocationTag(new Location(world, hit.getHitBlockFace().getModX(), hit.getHitBlockFace().getModY(), hit.getHitBlockFace().getModZ()));
                } else {
                    LocationTag loc = new LocationTag(new Location(world, hit.getHitPosition().getX(), hit.getHitPosition().getY(), hit.getHitPosition().getZ()));
                    if (hit.getHitBlockFace() != null) loc.getLocation().setDirection(new Vector(hit.getHitBlockFace().getModX(), hit.getHitBlockFace().getModY(), hit.getHitBlockFace().getModZ()));
                    return loc;
                }
                return null;
            };

            if (pierce) {
                ListTag results = new ListTag();
                List<RayTraceResult> hits = new ArrayList<>();
                FluidCollisionMode fluidMode = fluids ? FluidCollisionMode.ALWAYS : FluidCollisionMode.NEVER;

                BlockIterator bit = new BlockIterator(world, startVec, dirVec, 0, (int) Math.ceil(range));
                while (bit.hasNext()) {
                    Block b = bit.next();
                    BukkitSchedulerAdapter.requireRegion(b.getLocation());

                    if (b.isEmpty() || (b.isPassable() && !nonsolids)) continue;

                    RayTraceResult res = b.rayTrace(start, dirVec, range, fluidMode);
                    if (res != null) hits.add(res);
                }

                if (!entitiesMatcher.isEmpty()) {
                    Vector endVec = startVec.clone().add(dirVec.clone().multiply(range));
                    BoundingBox bounds = BoundingBox.of(startVec, endVec).expand(raySize);
                    for (Entity e : world.getNearbyEntities(bounds)) {
                        if (ignoreList.contains(e.getUniqueId())) continue;
                        if (!new EntityTag(e).tryAdvancedMatcher(entitiesMatcher)) continue;

                        RayTraceResult res = e.getBoundingBox().expand(raySize).rayTrace(startVec, dirVec, range);
                        if (res != null) hits.add(new RayTraceResult(res.getHitPosition(), e));;
                    }
                }

                hits.sort(Comparator.comparingDouble(h -> startVec.distanceSquared(h.getHitPosition())));

                for (RayTraceResult hit : hits) {
                    LocationTag mapped = mapHit.apply(hit);
                    if (mapped != null) results.addObject(mapped);
                }

                if (results.getList().isEmpty() && "air".equals(defaultMode)) {
                    results.addObject(new LocationTag(start.clone().add(dirVec.clone().multiply(range))));
                }
                return results;
            }

            RayTraceResult result;
            if (entitiesMatcher.isEmpty()) {
                result = world.rayTraceBlocks(start, dirVec, range, fluids ? FluidCollisionMode.ALWAYS : FluidCollisionMode.NEVER, !nonsolids);
            } else {
                String finalEntitiesMatcher = entitiesMatcher;
                result = world.rayTrace(start, dirVec, range, fluids ? FluidCollisionMode.ALWAYS : FluidCollisionMode.NEVER, !nonsolids, raySize, e -> {
                    if (ignoreList.contains(e.getUniqueId())) return false;
                    return new EntityTag(e).tryAdvancedMatcher(finalEntitiesMatcher);
                });
            }

            if (result != null) {
                LocationTag mapped = mapHit.apply(result);
                if (mapped != null) return mapped;
            }

            if ("air".equals(defaultMode)) {
                return new LocationTag(start.clone().add(dirVec.clone().multiply(range)));
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name rayTraceTarget[]
         * @RawName <LocationTag.rayTraceTarget[(range=<#.#>/{200});(blocks=<{true}/false>);(fluids=<true/{false}>);(nonsolids=<true/{false}>);(entities=<matcher>);(ignore=<entity>|...);(raysize=<#.#>/{0});(pierce=<true/{false}>)]>
         * @Object LocationTag
         * @ReturnType EntityTag, ListTag(EntityTag)
         * @ArgRequired
         * @Description
         * Traces a line to find entities. Parsed as a MapTag.
         * If pierce=true, returns a ListTag of all entities pierced by the ray.
         * If blocks=true, the piercing stops at the first solid block it hits.
         *
         * @Implements LocationTag.ray_trace_target[(range=<#.#>/{200});(blocks=<{true}/false>);(fluids=<true/{false}>);(nonsolids=<true/{false}>);(entities=<matcher>);(ignore=<entity>|...);(raysize=<#.#>/{0})]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "rayTraceTarget", (attr, obj) -> {
            if (!attr.hasParam()) return null;

            String param = attr.getParam();
            if (!param.startsWith("[")) param = "[" + param + "]";
            MapTag input = new MapTag(param);

            double range = 200.0;
            boolean blocks = true;
            boolean fluids = false;
            boolean nonsolids = false;
            String entitiesMatcher = "";
            double raySize = 0.0;
            boolean pierce = false;
            Set<UUID> ignoreList = new HashSet<>();

            AbstractTag rangeTag = input.getObject("range");
            if (rangeTag != null) range = new ElementTag(rangeTag.identify()).asDouble();

            AbstractTag blocksTag = input.getObject("blocks");
            if (blocksTag != null) blocks = new ElementTag(blocksTag.identify()).asBoolean();

            AbstractTag fluidsTag = input.getObject("fluids");
            if (fluidsTag != null) fluids = new ElementTag(fluidsTag.identify()).asBoolean();

            AbstractTag nonsolidsTag = input.getObject("nonsolids");
            if (nonsolidsTag != null) nonsolids = new ElementTag(nonsolidsTag.identify()).asBoolean();

            AbstractTag entitiesTag = input.getObject("entities");
            if (entitiesTag != null) entitiesMatcher = entitiesTag.identify();

            AbstractTag raysizeTag = input.getObject("raysize");
            if (raysizeTag != null) raySize = new ElementTag(raysizeTag.identify()).asDouble();

            AbstractTag pierceTag = input.getObject("pierce");
            if (pierceTag != null) pierce = new ElementTag(pierceTag.identify()).asBoolean();

            AbstractTag ignoreTag = input.getObject("ignore");
            if (ignoreTag != null) {
                for (String e : ObjectFetcher.splitIgnoringBrackets(ignoreTag.identify(), '|')) {
                    AbstractTag parsed = ObjectFetcher.pickObject(e);
                    if (parsed instanceof EntityTag et) {
                        ignoreList.add(et.getEntity().getUniqueId());
                    }
                }
            }

            Location start = obj.getLocation();
            World world = start.getWorld();
            if (world == null) return null;
            BukkitSchedulerAdapter.requireRegion(start);

            Vector startVec = start.toVector();
            Vector dirVec = start.getDirection();

            if (pierce) {
                ListTag results = new ListTag();
                double blockHitDistSq = Double.MAX_VALUE;

                if (blocks) {
                    RayTraceResult blockHit = world.rayTraceBlocks(start, dirVec, range, fluids ? FluidCollisionMode.ALWAYS : FluidCollisionMode.NEVER, !nonsolids);
                    if (blockHit != null) {
                        blockHitDistSq = startVec.distanceSquared(blockHit.getHitPosition());
                    }
                }

                List<RayTraceResult> entityHits = new ArrayList<>();
                Vector endVec = startVec.clone().add(dirVec.clone().multiply(range));
                BoundingBox bounds = BoundingBox.of(startVec, endVec).expand(raySize);

                for (Entity e : world.getNearbyEntities(bounds)) {
                    if (ignoreList.contains(e.getUniqueId())) continue;
                    if (!entitiesMatcher.isEmpty() && !new EntityTag(e).tryAdvancedMatcher(entitiesMatcher)) continue;

                    RayTraceResult res = e.getBoundingBox().expand(raySize).rayTrace(startVec, dirVec, range);
                    if (res != null) {
                        double distSq = startVec.distanceSquared(res.getHitPosition());
                        if (distSq <= blockHitDistSq) {
                            entityHits.add(new RayTraceResult(res.getHitPosition(), e));
                        }
                    }
                }

                entityHits.sort(Comparator.comparingDouble(h -> startVec.distanceSquared(h.getHitPosition())));

                for (RayTraceResult hit : entityHits) {
                    results.addObject(new EntityTag(hit.getHitEntity()));
                }
                return results;
            }

            RayTraceResult result;
            String finalEntitiesMatcher = entitiesMatcher;
            Predicate<Entity> filter = e -> {
                if (ignoreList.contains(e.getUniqueId())) return false;
                return finalEntitiesMatcher.isEmpty() || new EntityTag(e).tryAdvancedMatcher(finalEntitiesMatcher);
            };

            if (!blocks) {
                result = world.rayTraceEntities(start, dirVec, range, raySize, filter);
            } else {
                result = world.rayTrace(start, dirVec, range, fluids ? FluidCollisionMode.ALWAYS : FluidCollisionMode.NEVER, !nonsolids, raySize, filter);
            }

            if (result != null && result.getHitEntity() != null) {
                return new EntityTag(result.getHitEntity());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name signContents[]
         * @RawName <LocationTag.signContents[(<side>)]>
         * @Object LocationTag
         * @ReturnType ListTag(ElementTag)
         * @Description
         * Returns a list of the text lines currently written on the specified side of this sign block.
         * The side parameter can be 'front' (default) or 'back'.
         *
         * @Implements LocationTag.sign_contents
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "signContents", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Sign sign) {
                Side side = Side.FRONT;
                if (attr.hasParam() && attr.getParam().equalsIgnoreCase("back")) {
                    side = Side.BACK;
                }

                ListTag list = new ListTag();
                for (String line : sign.getSide(side).getLines()) {
                    list.addObject(new ElementTag(line));
                }
                return list;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name signGlowColor[]
         * @RawName <LocationTag.signGlowColor[(<side>)]>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @Description
         * Returns the name of the dye color used for the glowing text on the specified side of this sign.
         * The side parameter can be 'front' (default) or 'back'.
         *
         * @Implements LocationTag.sign_glow_color
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "signGlowColor", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Sign sign) {
                Side side = Side.FRONT;
                if (attr.hasParam() && attr.getParam().equalsIgnoreCase("back")) {
                    side = Side.BACK;
                }

                DyeColor color = sign.getSide(side).getColor();
                return color != null ? new ElementTag(color.name()) : null;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name signGlowing[]
         * @RawName <LocationTag.signGlowing[(<side>)]>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @Description
         * Returns whether the text on the specified side of this sign is currently glowing.
         * The side parameter can be 'front' (default) or 'back'.
         *
         * @Implements LocationTag.sign_glowing
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "signGlowing", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Sign sign) {
                Side side = Side.FRONT;
                if (attr.hasParam() && attr.getParam().equalsIgnoreCase("back")) {
                    side = Side.BACK;
                }

                return new ElementTag(sign.getSide(side).isGlowingText());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name waxed
         * @RawName <LocationTag.waxed>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether this sign or copper block is waxed (preventing further edits or oxidation).
         *
         * @Implements LocationTag.waxed
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "waxed", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Sign sign) {
                return new ElementTag(sign.isWaxed());
            }
            return new ElementTag(false);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name skullName
         * @RawName <LocationTag.skullName>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the profile name assigned to this player skull block.
         *
         * @Implements LocationTag.skull_name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "skullName", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof org.bukkit.block.Skull skull) {
                PlayerProfile profile = skull.getPlayerProfile();
                return profile != null && profile.getName() != null ? new ElementTag(profile.getName()) : null;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name skullSkin
         * @RawName <LocationTag.skullSkin>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the UUID or Name of the player profile assigned to this skull block.
         *
         * @Implements LocationTag.skull_skin
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "skullSkin", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof org.bukkit.block.Skull skull) {
                PlayerProfile profile = skull.getPlayerProfile();
                if (profile == null) return null;

                /* @doc tag
                 *
                 * @Name skullSkin.full
                 * @RawName <LocationTag.skullSkin.full>
                 * @Object LocationTag
                 * @ReturnType ElementTag
                 * @NoArg
                 * @Description
                 * Returns the UUID or Name of the player profile assigned to this skull block in 'UUID|TextureURL' format.
                 *
                 * @Implements LocationTag.skull_skin.full
                 */
                boolean full = attr.matchesNext("full");
                if (full) attr.fulfill(1);

                String id = profile.getId() != null ? profile.getId().toString() : profile.getName();
                if (full) {
                    URL texture = profile.getTextures().getSkin();
                    return new ElementTag(id + (texture != null ? "|" + texture : ""));
                }
                return new ElementTag(id);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name skullType
         * @RawName <LocationTag.skullType>
         * @Object LocationTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the specific material type of this skull block (e.g. PLAYER_HEAD, CREEPER_HEAD).
         *
         * @Implements LocationTag.skull_type
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "skullType", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Skull) {
                return new ElementTag(loc.getBlock().getType().name());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name spawnerCount
         * @RawName <LocationTag.spawnerCount>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the number of mobs this spawner attempts to spawn per cycle.
         *
         * @Implements LocationTag.spawner_count
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "spawnerCount", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CreatureSpawner spawner) {
                return new ElementTag(spawner.getSpawnCount());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name spawnerDisplayEntity
         * @RawName <LocationTag.spawnerDisplayEntity>
         * @Object LocationTag
         * @ReturnType EntityTag
         * @NoArg
         * @Description
         * Returns the EntityTag representing the type of mob this spawner will spawn.
         *
         * @Implements LocationTag.spawner_display_entity, LocationTag.spawner_type
         */
        TAG_PROCESSOR.registerTag(EntityTag.class, "spawnerDisplayEntity", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CreatureSpawner spawner && spawner.getSpawnedType() != null) {
                return new EntityTag(spawner.getSpawnedType().name());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name spawnerMaxNearbyEntities
         * @RawName <LocationTag.spawnerMaxNearbyEntities>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the maximum number of similar entities allowed near this spawner before it pauses.
         *
         * @Implements LocationTag.spawner_max_nearby_entities
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "spawnerMaxNearbyEntities", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CreatureSpawner spawner) {
                return new ElementTag(spawner.getMaxNearbyEntities());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name spawnerDelayData
         * @RawName <LocationTag.spawnerDelayData>
         * @Object LocationTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a MapTag containing the delay timings (current, min, max) of this spawner.
         * Keys: delay, min, max
         *
         * @Implements LocationTag.spawner_maximum_spawn_delay, LocationTag.spawner_minimum_spawn_delay, LocationTag.spawner_spawn_delay
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "spawnerDelayData", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CreatureSpawner spawner) {
                MapTag map = new MapTag();
                map.putObject("delay", new ElementTag(spawner.getDelay()));
                map.putObject("min", new ElementTag(spawner.getMinSpawnDelay()));
                map.putObject("max", new ElementTag(spawner.getMaxSpawnDelay()));
                return map;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name spawnerPlayerRange
         * @RawName <LocationTag.spawnerPlayerRange>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the required player distance for this spawner to become active.
         *
         * @Implements LocationTag.spawner_player_range
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "spawnerPlayerRange", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CreatureSpawner spawner) {
                return new ElementTag(spawner.getRequiredPlayerRange());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name spawnerRange
         * @RawName <LocationTag.spawnerRange>
         * @Object LocationTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the block radius around the spawner where mobs can be spawned.
         *
         * @Implements LocationTag.spawner_range
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "spawnerRange", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof CreatureSpawner spawner) {
                return new ElementTag(spawner.getSpawnRange());
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name structureBlockData
         * @RawName <LocationTag.structureBlockData>
         * @Object LocationTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a MapTag containing all data assigned to a Structure Block.
         *
         * @Implements LocationTag.structure_block_data
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "structureBlockData", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getState() instanceof Structure struct) {
                MapTag map = new MapTag();
                map.putObject("author", new ElementTag(struct.getAuthor()));
                map.putObject("integrity", new ElementTag(struct.getIntegrity()));
                map.putObject("metadata", new ElementTag(struct.getMetadata()));
                map.putObject("mirror", new ElementTag(struct.getMirror().name()));
                map.putObject("rotation", new ElementTag(struct.getRotation().name()));
                map.putObject("seed", new ElementTag(struct.getSeed()));
                map.putObject("structure_name", new ElementTag(struct.getStructureName()));
                map.putObject("mode", new ElementTag(struct.getUsageMode().name()));
                map.putObject("box_visible", new ElementTag(struct.isBoundingBoxVisible()));
                map.putObject("ignore_entities", new ElementTag(struct.isIgnoreEntities()));
                map.putObject("show_invisible", new ElementTag(struct.isShowAir()));

                BlockVector pos = struct.getRelativePosition();
                map.putObject("box_position", new LocationTag(new Location(loc.getWorld(), pos.getX(), pos.getY(), pos.getZ())));

                BlockVector size = struct.getStructureSize();
                map.putObject("size", new LocationTag(new Location(loc.getWorld(), size.getX(), size.getY(), size.getZ())));

                return map;
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name activated
         * @RawName <LocationTag.activated>
         * @Object LocationTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the block at this location is logically "activated" (e.g., powered lever, lit campfire, open door).
         *
         * @Implements LocationTag.switched, LocationTag.activated
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "activated", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            BlockData data = loc.getBlock().getBlockData();

            boolean active = switch (data) {
                case Powerable p -> p.isPowered();
                case Openable o -> o.isOpen();
                case Lightable l -> l.isLit();
                default -> false;
            };
            return new ElementTag(active);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name withFacingDirection
         * @RawName <LocationTag.withFacingDirection>
         * @Object LocationTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns a copy of this location with its yaw and pitch mathematically rotated to match the direction the block is physically facing.
         *
         * @Implements LocationTag.with_facing_direction
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "withFacingDirection", (attr, obj) -> {
            Location loc = obj.getLocation();
            if (loc.getWorld() == null) return null;
            BukkitSchedulerAdapter.requireRegion(loc);

            if (loc.getBlock().getBlockData() instanceof Directional dir) {
                Vector facing = dir.getFacing().getDirection();
                float yaw = (float) Math.toDegrees(Math.atan2(-facing.getX(), facing.getZ()));
                float pitch = (float) Math.toDegrees(Math.asin(-facing.getY()));

                return new LocationTag(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), yaw, pitch));
            }
            return null;
        }).ignoreTest();
    }

    private static long packBlockLoc(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int blockFaceToRotationIndex(BlockFace face) {
        return switch (face) {
            case NORTH_NORTH_EAST -> 2;
            case NORTH_EAST -> 3;
            case EAST_NORTH_EAST -> 4;
            case EAST -> 5;
            case EAST_SOUTH_EAST -> 6;
            case SOUTH_EAST -> 7;
            case SOUTH_SOUTH_EAST -> 8;
            case SOUTH -> 9;
            case SOUTH_SOUTH_WEST -> 10;
            case SOUTH_WEST -> 11;
            case WEST_SOUTH_WEST -> 12;
            case WEST -> 13;
            case WEST_NORTH_WEST -> 14;
            case NORTH_WEST -> 15;
            case NORTH_NORTH_WEST -> 16;
            default -> 1;
        };
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    private static int unpackZ(long packed) {
        return (int) ((packed << 26) >> 38);
    }

    private static int unpackY(long packed) {
        return (int) ((packed << 52) >> 52);
    }

    public LocationTag(Location location) {
        this(location, true);
    }

    private LocationTag(Location location, boolean doClone) {
        this.location = doClone ? location.clone() : location;
    }

    public LocationTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.location = new Location(null, 0, 0, 0);
            return;
        }

        if (raw.startsWith(prefix + "@")) {
            raw = raw.substring(2);
        }

        int commaCount = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == ',') commaCount++;
        }

        String[] split = new String[commaCount + 1];
        int start = 0, index = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == ',') {
                split[index++] = raw.substring(start, i).trim();
                start = i + 1;
            }
        }
        split[index] = raw.substring(start).trim();

        double x = 0, y = 0, z = 0;
        float pitch = 0, yaw = 0;
        World world = null;

        int size = split.length;
        try {
            if (size >= 2) {
                x = Double.parseDouble(split[0]);
                y = Double.parseDouble(split[1]);
            }
            if (size >= 3) {
                z = Double.parseDouble(split[2]);
            }

            if (size == 4) {
                world = Bukkit.getWorld(split[3]);
            } else if (size >= 5) {
                pitch = Float.parseFloat(split[3]);
                yaw = Float.parseFloat(split[4]);
                if (size == 6) world = Bukkit.getWorld(split[5]);
            }
        } catch (NumberFormatException ignored) {}

        this.location = new Location(world, x, y, z, yaw, pitch);
    }

    public Location getLocation() {
        return location;
    }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(prefix).append("@");
        appendCleanDouble(sb, location.getX()).append(",");
        appendCleanDouble(sb, location.getY()).append(",");
        appendCleanDouble(sb, location.getZ());

        boolean hasAngles = location.getPitch() != 0.0f || location.getYaw() != 0.0f;
        boolean hasWorld = location.getWorld() != null;

        if (hasAngles || hasWorld) {
            if (hasAngles) {
                sb.append(",");
                appendCleanDouble(sb, location.getPitch()).append(",");
                appendCleanDouble(sb, location.getYaw());
            }
            if (hasWorld) {
                sb.append(",").append(location.getWorld().getName());
            }
        }
        return sb.toString();
    }

    private StringBuilder appendCleanDouble(StringBuilder sb, double d) {
        long l = (long) d;
        if (d == l) sb.append(l);
        else sb.append(d);
        return sb;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<LocationTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        if (location.getWorld() == null) return null;
        return new LocationPdcFlagTracker(location, identify());
    }

    @Override
    public @NonNull String getTestValue() {
        return "l@1,1,1,world";
    }
}
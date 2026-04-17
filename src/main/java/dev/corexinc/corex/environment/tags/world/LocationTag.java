package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.LocationPdcFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

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
         * @Implements VectorObject.x
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
         * @Implements VectorObject.y
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
         * @Implements VectorObject.z
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "z", (attr, obj) -> new ElementTag(obj.location.getZ()));

        /* @doc tag
         *
         * @Name yaw
         * @RawName <LocationTag.yaw>
         * @Object LocationTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the normalized yaw rotation value of an object positioned at this location.
         * Yaw represents the horizontal rotation around the Y-axis.
         *
         * @Implements LocationTag.yaw
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "yaw", (attr, obj) -> new ElementTag(obj.location.getYaw()));

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
         * This operation does not modify the original LocationTag, but provides a new one with the combined values.
         *
         * @Implements VectorObject.add
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "add", (attr, obj) -> {
            if (!attr.hasParam()) return null;

            String param = attr.getParam();
            LocationTag other;

            Object fetched = ObjectFetcher.pickObject(param);
            if (fetched instanceof LocationTag) {
                other = (LocationTag) fetched;
            } else {
                other = new LocationTag(param);
            }

            Location loc = obj.location.clone();
            loc.add(other.getLocation().getX(), other.getLocation().getY(), other.getLocation().getZ());
            return new LocationTag(loc);
        }).test("1, 2, 3");

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

            return new LocationTag(loc);
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
        TAG_PROCESSOR.registerTag(RegionTag.class, "region", (attribute, locationTag) ->
                new RegionTag(
                locationTag.getLocation().getWorld(),
                locationTag.getLocation().getBlockX() >> 4,
                locationTag.getLocation().getBlockZ() >> 4
        ));
    }

    public LocationTag(Location location) {
        this.location = location.clone();
    }

    public LocationTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.location = new Location(null, 0, 0, 0);
            return;
        }

        if (raw.toLowerCase().startsWith(prefix + "@")) {
            raw = raw.substring(2);
        }

        String[] split = raw.trim().split("\\s*,\\s*");
        double x = 0, y = 0, z = 0;
        float pitch = 0, yaw = 0;
        World world = null;

        try {
            int size = split.length;
            if (size >= 2) { x = Double.parseDouble(split[0]); y = Double.parseDouble(split[1]); }
            if (size >= 3) { z = Double.parseDouble(split[2]); }

            if (size == 4) {
                world = Bukkit.getWorld(split[3]);
            } else if (size == 5 || size == 6) {
                pitch = Float.parseFloat(split[3]);
                yaw = Float.parseFloat(split[4]);
                if (size == 6) world = Bukkit.getWorld(split[5]);
            }
        } catch (Exception ignored) {
        }

        this.location = new Location(world, x, y, z, yaw, pitch);
    }

    public Location getLocation() {
        return location;
    }

    private static String cleanDouble(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.valueOf(d);
    }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        StringBuilder sb = new StringBuilder(prefix).append("@")
                .append(cleanDouble(location.getX())).append(",")
                .append(cleanDouble(location.getY())).append(",")
                .append(cleanDouble(location.getZ()));

        boolean hasAngles = location.getPitch() != 0.0f || location.getYaw() != 0.0f;
        boolean hasWorld = location.getWorld() != null;

        if (hasAngles || hasWorld) {
            if (hasAngles) {
                sb.append(",").append(cleanDouble(location.getPitch()))
                        .append(",").append(cleanDouble(location.getYaw()));
            }
            if (hasWorld) {
                sb.append(",").append(location.getWorld().getName());
            }
        }
        return sb.toString();
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
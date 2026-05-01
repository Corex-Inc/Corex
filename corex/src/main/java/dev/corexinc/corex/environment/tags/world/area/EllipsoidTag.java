package dev.corexinc.corex.environment.tags.world.area;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/* @doc object
 *
 * @Name EllipsoidTag
 * @Prefix ellipsoid
 *
 * @Format
 * The identity format for ellipsoids is `ellipsoid@x,y,z,world,xRadius,yRadius,zRadius`.
 * For example: `ellipsoid@1,2,3,space,7,7,7`.
 *
 * @Description
 * An EllipsoidTag represents an ellipsoidal region in the world.
 * The word 'ellipsoid' means a less strict sphere.
 * Basically: an "ellipsoid" is to a 3D "sphere" what an "ellipse" is to a 2D "circle".
 *
 * A sphere is a special case where all three radii are equal.
 * Containment is determined by the standard ellipsoid equation:
 * (dx/rx)² + (dy/ry)² + (dz/rz)² ≤ 1
 */
public class EllipsoidTag implements AbstractTag, AbstractAreaObject, Flaggable {

    private static final String PREFIX = "ellipsoid";

    private final double x;
    private final double y;
    private final double z;
    private final World world;
    private final double radiusX;
    private final double radiusY;
    private final double radiusZ;

    public static final TagProcessor<EllipsoidTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("ellipsoid", attr -> new EllipsoidTag(attr.getParam()));
        ObjectFetcher.registerFetcher(PREFIX, EllipsoidTag::new);

        /* @doc tag
         *
         * @Name contains[]
         * @RawName <EllipsoidTag.contains[<location>]>
         * @Object EllipsoidTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Returns whether the given location is inside this ellipsoid region.
         * Uses the standard ellipsoid equation: (dx/rx)² + (dy/ry)² + (dz/rz)² ≤ 1.
         *
         * @Implements AreaObject.contains
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "contains", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            LocationTag lt = fetched instanceof LocationTag ? (LocationTag) fetched : new LocationTag(attr.getParam());
            return new ElementTag(obj.contains(lt.getLocation()));
        }).test("l@0,64,0,world");

        /* @doc tag
         *
         * @Name center
         * @RawName <EllipsoidTag.center>
         * @Object EllipsoidTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the center point of this ellipsoid region.
         *
         * @Implements AreaObject.center
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "center", (attr, obj) -> obj.getCenter());

        /* @doc tag
         *
         * @Name blocks
         * @RawName <EllipsoidTag.blocks>
         * @Object EllipsoidTag
         * @ReturnType ListTag(LocationTag)
         * @NoArg
         * @Description
         * Returns a list of all block locations contained within this ellipsoid region.
         * Each block position is tested against the ellipsoid equation.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "blocks", (attr, obj) -> {
            ListTag result = new ListTag();
            for (LocationTag loc : obj.getBlocks()) result.addObject(loc);
            return result;
        });
    }

    public EllipsoidTag(double x, double y, double z, World world, double radiusX, double radiusY, double radiusZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.radiusX = Math.abs(radiusX);
        this.radiusY = Math.abs(radiusY);
        this.radiusZ = Math.abs(radiusZ);
    }

    public EllipsoidTag(String raw) {
        if (raw == null || raw.isBlank()) {
            this.x = this.y = this.z = 0;
            this.world = null;
            this.radiusX = this.radiusY = this.radiusZ = 1.0;
            return;
        }

        if (raw.toLowerCase().startsWith(PREFIX + "@")) {
            raw = raw.substring(PREFIX.length() + 1);
        }

        String[] parts = raw.trim().split("\\s*,\\s*");

        double px = 0, py = 0, pz = 0, rx = 1, ry = 1, rz = 1;
        World pw = null;

        try {
            if (parts.length > 0) px = Double.parseDouble(parts[0]);
            if (parts.length > 1) py = Double.parseDouble(parts[1]);
            if (parts.length > 2) pz = Double.parseDouble(parts[2]);
            if (parts.length > 3) pw = Bukkit.getWorld(parts[3]);
            if (parts.length > 4) rx = Double.parseDouble(parts[4]);
            if (parts.length > 5) ry = Double.parseDouble(parts[5]);
            if (parts.length > 6) rz = Double.parseDouble(parts[6]);
        } catch (Exception ignored) {}

        this.x = px;
        this.y = py;
        this.z = pz;
        this.world = pw;
        this.radiusX = Math.abs(rx);
        this.radiusY = Math.abs(ry);
        this.radiusZ = Math.abs(rz);
    }

    @Override
    public boolean contains(@NonNull Location location) {
        if (world != null && location.getWorld() != null && !world.equals(location.getWorld())) {
            return false;
        }

        if (radiusX == 0 || radiusY == 0 || radiusZ == 0) return false;

        double dx = (location.getX() - x) / radiusX;
        double dy = (location.getY() - y) / radiusY;
        double dz = (location.getZ() - z) / radiusZ;

        return (dx * dx + dy * dy + dz * dz) <= 1.0;
    }

    @Override
    public LocationTag getCenter() {
        return new LocationTag(new Location(world, x, y, z));
    }

    @Override
    public List<LocationTag> getBlocks() {
        List<LocationTag> blocks = new ArrayList<>();

        int minX = (int) Math.floor(x - radiusX);
        int minY = (int) Math.floor(y - radiusY);
        int minZ = (int) Math.floor(z - radiusZ);
        int maxX = (int) Math.ceil(x + radiusX);
        int maxY = (int) Math.ceil(y + radiusY);
        int maxZ = (int) Math.ceil(z + radiusZ);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    Location loc = new Location(world, bx, by, bz);
                    if (contains(loc)) {
                        blocks.add(new LocationTag(loc));
                    }
                }
            }
        }

        return blocks;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public World getWorld() { return world; }
    public double getRadiusX() { return radiusX; }
    public double getRadiusY() { return radiusY; }
    public double getRadiusZ() { return radiusZ; }

    private static String cleanDouble(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.valueOf(d);
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@"
                + cleanDouble(x) + ","
                + cleanDouble(y) + ","
                + cleanDouble(z) + ","
                + (world != null ? world.getName() : "null") + ","
                + cleanDouble(radiusX) + ","
                + cleanDouble(radiusY) + ","
                + cleanDouble(radiusZ);
    }

    @Override
    public @NonNull AbstractFlagTracker getFlagTracker() {
        File dbFile = new File(world.getWorldFolder(), "__flags.db");
        return new SqlFlagTracker(dbFile, identify());
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<EllipsoidTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NonNull String getTestValue() { return "ellipsoid@1,2,3,space,5,5,5"; }
}
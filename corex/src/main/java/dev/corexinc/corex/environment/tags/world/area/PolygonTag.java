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
import java.util.Collections;
import java.util.List;

/* @doc object
 *
 * @Name PolygonTag
 * @Prefix polygon
 *
 * @Format
 * The identity format for a PolygonTag is `polygon@world,yMin,yMax,x1,z1,x2,z2,...`.
 * The x,z pair repeats for each vertex of the polygon.
 * For example: `polygon@world,-64,320,0,0,10,0,10,10,0,10`.
 *
 * @Description
 * A PolygonTag represents a polygonal region in the world.
 * The word 'polygon' means an arbitrary 2D shape. PolygonTags, in addition to
 * a 2D polygon, contain a minimum and maximum Y coordinate to allow them to function in 3D.
 *
 * PolygonTags are NOT polyhedra.
 *
 * A PolygonTag with 4 points at right angles covers an area equivalent to a CuboidTag,
 * however all other shapes a PolygonTag can form are unique.
 *d
 * Compared to CuboidTags, PolygonTags are generally slower to process (O(n) per check)
 * and more complex to work with, but support more intricate shapes.
 *
 * Containment is determined using the ray-casting algorithm on the X/Z plane,
 * followed by a Y bounds check.
 *
 * Note: forming invalid polygons (duplicate corners, self-intersecting shapes, etc.)
 * will not raise any errors, but may cause incorrect results.
 */
public class PolygonTag implements AbstractTag, AbstractAreaObject, Flaggable {

    private static final String PREFIX = "polygon";

    private final World world;
    private final double yMin;
    private final double yMax;
    private final List<double[]> points;

    public static final TagProcessor<PolygonTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("polygon", attr -> new PolygonTag(attr.getParam()));
        ObjectFetcher.registerFetcher(PREFIX, PolygonTag::new);

        /* @doc tag
         *
         * @Name contains[]
         * @RawName <PolygonTag.contains[<location>]>
         * @Object PolygonTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Returns whether the given location is inside this polygon region.
         * Uses the ray-casting algorithm on the X/Z plane with a Y bounds check.
         *
         * @Implements AreaObject.contains
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "contains", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            LocationTag lt = fetched instanceof LocationTag ? (LocationTag) fetched : new LocationTag(attr.getParam());
            return new ElementTag(obj.contains(lt.getLocation()));
        }).test("l@5,64,5,world");

        /* @doc tag
         *
         * @Name center
         * @RawName <PolygonTag.center>
         * @Object PolygonTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the centroid of this polygon at the mid-Y level.
         * The centroid is computed as the average of all vertex positions.
         *
         * @Implements AreaObject.center
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "center", (attr, obj) -> obj.getCenter());

        /* @doc tag
         *
         * @Name blocks
         * @RawName <PolygonTag.blocks>
         * @Object PolygonTag
         * @ReturnType ListTag(LocationTag)
         * @NoArg
         * @Description
         * Returns a list of all block locations contained within this polygon region.
         * Each block position is tested via the ray-casting algorithm on the X/Z plane
         * with a Y bounds check.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "blocks", (attr, obj) -> {
            ListTag result = new ListTag();
            for (LocationTag loc : obj.getBlocks()) result.addObject(loc);
            return result;
        });
    }

    public PolygonTag(World world, double yMin, double yMax, List<double[]> points) {
        this.world = world;
        this.yMin = Math.min(yMin, yMax);
        this.yMax = Math.max(yMin, yMax);
        this.points = List.copyOf(points);
    }

    public PolygonTag(String raw) {
        if (raw == null || raw.isBlank()) {
            this.world = null;
            this.yMin = 0;
            this.yMax = 0;
            this.points = Collections.emptyList();
            return;
        }

        if (raw.toLowerCase().startsWith(PREFIX + "@")) {
            raw = raw.substring(PREFIX.length() + 1);
        }

        String[] parts = raw.trim().split("\\s*,\\s*");

        World resolvedWorld = null;
        double resolvedYMin = 0, resolvedYMax = 0;
        List<double[]> resolvedPoints = new ArrayList<>();

        try {
            if (parts.length >= 1) resolvedWorld = Bukkit.getWorld(parts[0]);
            if (parts.length >= 2) resolvedYMin = Double.parseDouble(parts[1]);
            if (parts.length >= 3) resolvedYMax = Double.parseDouble(parts[2]);

            for (int i = 3; i + 1 < parts.length; i += 2) {
                double x = Double.parseDouble(parts[i]);
                double z = Double.parseDouble(parts[i + 1]);
                resolvedPoints.add(new double[]{x, z});
            }
        } catch (Exception ignored) {}

        this.world = resolvedWorld;
        this.yMin = Math.min(resolvedYMin, resolvedYMax);
        this.yMax = Math.max(resolvedYMin, resolvedYMax);
        this.points = Collections.unmodifiableList(resolvedPoints);
    }

    @Override
    public boolean contains(@NonNull Location location) {
        if (world != null && location.getWorld() != null && !world.equals(location.getWorld())) {
            return false;
        }

        double y = location.getY();
        if (y < yMin || y > yMax) return false;

        if (points.size() < 3) return false;

        double px = location.getX();
        double pz = location.getZ();
        boolean inside = false;
        int n = points.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i)[0], zi = points.get(i)[1];
            double xj = points.get(j)[0], zj = points.get(j)[1];

            boolean intersects = (zi > pz) != (zj > pz)
                    && px < (xj - xi) * (pz - zi) / (zj - zi) + xi;

            if (intersects) inside = !inside;
        }

        return inside;
    }

    @Override
    public LocationTag getCenter() {
        if (points.isEmpty()) {
            return new LocationTag(new Location(world, 0, (yMin + yMax) / 2.0, 0));
        }

        double sumX = 0, sumZ = 0;
        for (double[] point : points) {
            sumX += point[0];
            sumZ += point[1];
        }

        return new LocationTag(new Location(
                world,
                sumX / points.size(),
                (yMin + yMax) / 2.0,
                sumZ / points.size()
        ));
    }

    @Override
    public List<LocationTag> getBlocks() {
        if (points.size() < 3) return Collections.emptyList();

        double minPX = Double.MAX_VALUE, minPZ = Double.MAX_VALUE;
        double maxPX = -Double.MAX_VALUE, maxPZ = -Double.MAX_VALUE;

        for (double[] point : points) {
            if (point[0] < minPX) minPX = point[0]; if (point[0] > maxPX) maxPX = point[0];
            if (point[1] < minPZ) minPZ = point[1]; if (point[1] > maxPZ) maxPZ = point[1];
        }

        int minX = (int) Math.floor(minPX);
        int maxX = (int) Math.floor(maxPX);
        int minY = (int) Math.floor(yMin);
        int maxY = (int) Math.floor(yMax);
        int minZ = (int) Math.floor(minPZ);
        int maxZ = (int) Math.floor(maxPZ);

        List<LocationTag> blocks = new ArrayList<>();

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                if (!containsXZ(bx, bz)) continue;
                for (int by = minY; by <= maxY; by++) {
                    blocks.add(new LocationTag(new Location(world, bx, by, bz)));
                }
            }
        }

        return blocks;
    }

    private boolean containsXZ(double px, double pz) {
        boolean inside = false;
        int n = points.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i)[0], zi = points.get(i)[1];
            double xj = points.get(j)[0], zj = points.get(j)[1];

            boolean intersects = (zi > pz) != (zj > pz)
                    && px < (xj - xi) * (pz - zi) / (zj - zi) + xi;

            if (intersects) inside = !inside;
        }

        return inside;
    }

    public World getWorld() { return world; }
    public double getYMin() { return yMin; }
    public double getYMax() { return yMax; }
    public List<double[]> getPoints() { return points; }

    private static String cleanDouble(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.valueOf(d);
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        StringBuilder sb = new StringBuilder(PREFIX).append("@")
                .append(world != null ? world.getName() : "null").append(",")
                .append(cleanDouble(yMin)).append(",")
                .append(cleanDouble(yMax));

        for (double[] point : points) {
            sb.append(",").append(cleanDouble(point[0]))
                    .append(",").append(cleanDouble(point[1]));
        }

        return sb.toString();
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
    public @NonNull TagProcessor<PolygonTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NonNull String getTestValue() { return "polygon@world,-1,2,0,0,5,0,5,5,0,5"; }
}
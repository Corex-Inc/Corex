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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* @doc object
 *
 * @Name CuboidTag
 * @Prefix cu
 *
 * @Format
 * The identity format for cuboids is `cu@world,x1,y1,z1,x2,y2,z2`.
 * Multi-member cuboids can simply continue listing x,y,z pairs.
 * For example: `cu@space,1,2,3,4,5,6` or `cu@world,0,0,0,5,5,5,10,10,10,15,15,15`.
 *
 * @Description
 * A CuboidTag represents a cuboidal region in the world.
 * One 'cuboid' consists of two points: the low point and a high point.
 *
 * A CuboidTag can contain as many cuboids within itself as needed,
 * allowing complex shapes to be formed from a single CuboidTag.
 *
 * Coordinates are inclusive - a cuboid from "5,5,5" to "5,5,5" contains
 * one full block and has a size of "1,1,1".
 */
public class CuboidTag implements AbstractTag, AbstractAreaObject, Flaggable {

    private static final String PREFIX = "cu";

    private final World world;

    private final List<double[][]> members;

    public static final TagProcessor<CuboidTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("cuboid", attr -> new CuboidTag(attr.getParam()));
        ObjectFetcher.registerFetcher(PREFIX, CuboidTag::new);

        /* @doc tag
         *
         * @Name contains[]
         * @RawName <CuboidTag.contains[<location>]>
         * @Object CuboidTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Returns whether the given location is inside any member of this cuboid region.
         * Bounds are inclusive on both sides.
         *
         * @Implements AreaObject.contains
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "contains", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            LocationTag lt = fetched instanceof LocationTag ? (LocationTag) fetched : new LocationTag(attr.getParam());
            return new ElementTag(obj.contains(lt.getLocation()));
        }).test("l@3,3,3,world");

        /* @doc tag
         *
         * @Name center
         * @RawName <CuboidTag.center>
         * @Object CuboidTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the center point of the overall bounding box of this cuboid.
         * For multi-member cuboids, this is the center spanning all members combined.
         *
         * @Implements AreaObject.center
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "center", (attr, obj) -> obj.getCenter());

        /* @doc tag
         *
         * @Name blocks
         * @RawName <CuboidTag.blocks>
         * @Object CuboidTag
         * @ReturnType ListTag(LocationTag)
         * @NoArg
         * @Description
         * Returns a list of all block locations contained within this cuboid region.
         * For multi-member cuboids, blocks from all members are included.
         * Overlapping blocks between members are deduplicated.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "blocks", (attr, obj) -> {
            ListTag result = new ListTag();
            for (LocationTag loc : obj.getBlocks()) result.addObject(loc);
            return result;
        });
    }

    public CuboidTag(World world, @NonNull List<double[][]> members) {
        this.world = world;
        this.members = List.copyOf(members);
    }

    public CuboidTag(String raw) {
        if (raw == null || raw.isBlank()) {
            this.world = null;
            this.members = Collections.emptyList();
            return;
        }

        if (raw.toLowerCase().startsWith(PREFIX + "@")) {
            raw = raw.substring(PREFIX.length() + 1);
        }

        // Format: world,x1,y1,z1,x2,y2,z2[,x3,y3,z3,x4,y4,z4,...]
        String[] parts = raw.trim().split("\\s*,\\s*");

        World resolvedWorld = null;
        List<double[][]> resolvedMembers = new ArrayList<>();

        try {
            if (parts.length >= 1) {
                resolvedWorld = Bukkit.getWorld(parts[0]);
            }

            // Parse coordinate triples starting at index 1
            List<double[]> points = new ArrayList<>();
            for (int i = 1; i + 2 < parts.length; i += 3) {
                double x = Double.parseDouble(parts[i]);
                double y = Double.parseDouble(parts[i + 1]);
                double z = Double.parseDouble(parts[i + 2]);
                points.add(new double[]{x, y, z});
            }

            // Pair up consecutive points into cuboid members
            for (int i = 0; i + 1 < points.size(); i += 2) {
                resolvedMembers.add(new double[][]{points.get(i), points.get(i + 1)});
            }

        } catch (Exception ignored) {}

        this.world = resolvedWorld;
        this.members = Collections.unmodifiableList(resolvedMembers);
    }

    @Override
    public boolean contains(@NonNull Location location) {
        if (world != null && location.getWorld() != null && !world.equals(location.getWorld())) {
            return false;
        }

        for (double[][] member : members) {
            double[] a = member[0], b = member[1];

            if (location.getX() >= Math.min(a[0], b[0]) && location.getX() <= Math.max(a[0], b[0])
                    && location.getY() >= Math.min(a[1], b[1]) && location.getY() <= Math.max(a[1], b[1])
                    && location.getZ() >= Math.min(a[2], b[2]) && location.getZ() <= Math.max(a[2], b[2])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public LocationTag getCenter() {
        if (members.isEmpty()) {
            return new LocationTag(new Location(world, 0, 0, 0));
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (double[][] member : members) {
            for (double[] p : member) {
                if (p[0] < minX) minX = p[0]; if (p[0] > maxX) maxX = p[0];
                if (p[1] < minY) minY = p[1]; if (p[1] > maxY) maxY = p[1];
                if (p[2] < minZ) minZ = p[2]; if (p[2] > maxZ) maxZ = p[2];
            }
        }

        return new LocationTag(new Location(world,
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0));
    }

    @Override
    public List<LocationTag> getBlocks() {
        Set<String> seen = new HashSet<>();
        List<LocationTag> blocks = new ArrayList<>();

        for (double[][] member : members) {
            double[] a = member[0], b = member[1];

            int minX = (int) Math.floor(Math.min(a[0], b[0]));
            int minY = (int) Math.floor(Math.min(a[1], b[1]));
            int minZ = (int) Math.floor(Math.min(a[2], b[2]));
            int maxX = (int) Math.floor(Math.max(a[0], b[0]));
            int maxY = (int) Math.floor(Math.max(a[1], b[1]));
            int maxZ = (int) Math.floor(Math.max(a[2], b[2]));

            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        String key = bx + "," + by + "," + bz;
                        if (seen.add(key)) {
                            blocks.add(new LocationTag(new Location(world, bx, by, bz)));
                        }
                    }
                }
            }
        }

        return blocks;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        File dbFile = new File(world.getWorldFolder(), "__flags.db");
        return new SqlFlagTracker(dbFile, identify());
    }

    public World getWorld() { return world; }
    public List<double[][]> getMembers() { return members; }

    private static String cleanDouble(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.valueOf(d);
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        StringBuilder sb = new StringBuilder(PREFIX).append("@")
                .append(world != null ? world.getName() : "null");

        for (double[][] member : members) {
            for (double[] point : member) {
                sb.append(",").append(cleanDouble(point[0]))
                        .append(",").append(cleanDouble(point[1]))
                        .append(",").append(cleanDouble(point[2]));
            }
        }

        return sb.toString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<CuboidTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NonNull String getTestValue() { return "cu@world,0,0,0,5,5,5"; }
}
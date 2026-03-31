package dev.corexmc.corex.environment.tags.world;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.tags.TagManager;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

public class LocationTag implements AbstractTag {

    private static String prefix = "l";
    private final Location location;

    public static final TagProcessor<LocationTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        TagManager.registerBaseTag("location", attr -> new LocationTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, LocationTag::new);

        PROCESSOR.registerTag(ElementTag.class, "x", (attr, obj) -> new ElementTag(obj.location.getX()));
        PROCESSOR.registerTag(ElementTag.class, "y", (attr, obj) -> new ElementTag(obj.location.getY()));
        PROCESSOR.registerTag(ElementTag.class, "z", (attr, obj) -> new ElementTag(obj.location.getZ()));
        PROCESSOR.registerTag(ElementTag.class, "yaw", (attr, obj) -> new ElementTag(obj.location.getYaw()));
        PROCESSOR.registerTag(ElementTag.class, "pitch", (attr, obj) -> new ElementTag(obj.location.getPitch()));

        PROCESSOR.registerTag(ElementTag.class, "world", (attr, obj) -> {
            World w = obj.location.getWorld();
            return new ElementTag(w != null ? w.getName() : null);
        });

        PROCESSOR.registerTag(LocationTag.class, "add", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            LocationTag other = (LocationTag) ObjectFetcher.pickObject(attr.getParam());
            Location loc = obj.location.clone();
            loc.add(other.location.getX(), other.location.getY(), other.location.getZ());
            return new LocationTag(loc);
        }).test("l@1,2,3");

        PROCESSOR.registerTag(LocationTag.class, "withWorld", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            Location loc = obj.location.clone();
            loc.setWorld(Bukkit.getWorld(attr.getParam()));
            return new LocationTag(loc);
        }).test("world");

        PROCESSOR.registerTag(LocationTag.class, "block", (attr, obj) -> {
            return new LocationTag(new Location(
                    obj.location.getWorld(),
                    obj.location.getBlockX(),
                    obj.location.getBlockY(),
                    obj.location.getBlockZ()
            ));
        });

        PROCESSOR.registerTag(MaterialTag.class, "material", (attr, obj) -> {
            if (obj.getLocation().getWorld() != null) {
                return new MaterialTag(obj.getLocation().getBlock());
            }
            return null;
        });


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

        String[] split = raw.split(",");
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
    @Override public @NonNull AbstractTag setPrefix(@NonNull String prefix) { this.prefix = prefix; return this; }

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
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public TagProcessor<LocationTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public String getTestValue() {
        return "l@1,1,1,world";
    }
}
package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Location;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/* @doc object
 *
 * @Name QuaternionTag
 * @Prefix quat
 *
 * @Format
 * The standard format for a QuaternionTag is `x,y,z,w`.
 * Example: `quat@0,0,0,1` (This is the "Identity" quaternion, meaning zero rotation).
 * You can also just write `quat@identity` as a shortcut.
 *
 * @Description
 * A QuaternionTag represents a rotation in 3D space.
 *
 * If you've worked with rotations before, you probably used "Pitch, Yaw, and Roll" (also called Euler angles). While Pitch/Yaw/Roll are easy to imagine, they have a massive flaw: if you look straight up or straight down, your rotation axes overlap, and the math breaks. This is called "Gimbal Lock". Furthermore, if you try to smoothly animate a transition from one Pitch/Yaw to another, the object will often spin in weird, unnatural arcs.
 *
 * Quaternions solve this problem.
 * Instead of thinking "pitch up 45 degrees, then yaw right 90 degrees", a quaternion thinks: "Imagine a pole sticking through the object in a specific direction (X, Y, Z), now spin the object around that pole by a certain amount (W)."
 * Because of this, quaternions never suffer from Gimbal Lock, and they allow for perfectly smooth animations between any two rotations.
 *
 * In modern Minecraft, Display Entities (Block Displays, Item Displays, and Text Displays) use quaternions for their `left_rotation` and `right_rotation` properties. If you want to smoothly rotate a flying sword or a floating block, you must use quaternions.
 *
 * Because quaternion math involves complex 4D numbers, **you should almost never write the X, Y, Z, W numbers manually.** Instead, use the built-in generator tags to create them intuitively:
 *
 * 1. No rotation: Use `quat@identity`. This means "default rotation, don't twist anything".
 * 2. Spinning around an axis: Take a direction vector and use `.toAxisAngleQuaternion`.
 *    Example: `<location[0,1,0].toAxisAngleQuaternion[3.14]>` will create a quaternion that rotates the object 180 degrees (3.14 radians) around the Y-axis (straight up).
 * 3. **Looking from A to B:** If you want an object to face a certain way, use `.quaternionBetween`.
 *    Example: `<location[0,0,1].quaternionBetween[<player.location>]>` creates a rotation that transitions from looking South (Z=1) to looking wherever the player is looking.
 *
 * The biggest superpower of quaternions is the `.slerp` tag (Spherical Linear Interpolation). It allows you to smoothly blend two rotations together.
 * For example: `<[rotationA].slerp[<[rotationB]>].amount[0.5]>` will give you a quaternion that is exactly halfway between Rotation A and Rotation B. By changing `amount` from `0.0` to `1.0` in a repeating script, you can create buttery-smooth 3D rotation animations.
 */
public class QuaternionTag implements AbstractTag {

    private static final String prefix = "quat";

    private final Quaterniond q;

    public static final TagProcessor<QuaternionTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("quaternion", attr -> {
            if (!attr.hasParam()) return new QuaternionTag(0, 0, 0, 1);
            return new QuaternionTag(attr.getParam());
        });

        ObjectFetcher.registerFetcher(prefix, QuaternionTag::new);

        /* @doc tag
         *
         * @Name x
         * @RawName <QuaternionTag.x>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the raw X component value of this quaternion.
         *
         * @Implements QuaternionTag.x
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "x", (attr, obj) -> new ElementTag(obj.q.x));

        /* @doc tag
         *
         * @Name y
         * @RawName <QuaternionTag.y>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the raw Y component value of this quaternion.
         *
         * @Implements QuaternionTag.y
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "y", (attr, obj) -> new ElementTag(obj.q.y));

        /* @doc tag
         *
         * @Name z
         * @RawName <QuaternionTag.z>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the raw Z component value of this quaternion.
         *
         * @Implements QuaternionTag.z
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "z", (attr, obj) -> new ElementTag(obj.q.z));

        /* @doc tag
         *
         * @Name w
         * @RawName <QuaternionTag.w>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the raw W component value of this quaternion.
         *
         * @Implements QuaternionTag.z
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "w", (attr, obj) -> new ElementTag(obj.q.w));

        /* @doc tag
         *
         * @Name xyz
         * @RawName <QuaternionTag.xyz>
         * @Object QuaternionTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the X, Y, and Z values of this quaternion in a simple comma-separated format ("x,y,z").
         *
         * @Implements QuaternionTag.xyz
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "xyz", (attr, obj) ->
                new ElementTag(cleanDouble(obj.q.x) + "," + cleanDouble(obj.q.y) + "," + cleanDouble(obj.q.z)));

        /* @doc tag
         *
         * @Name xyzw
         * @RawName <QuaternionTag.xyzw>
         * @Object QuaternionTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the X, Y, Z, and W values of this quaternion in a simple comma-separated format ("x,y,z,w").
         *
         * @Implements QuaternionTag.xyzw
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "xyzw", (attr, obj) ->
                new ElementTag(cleanDouble(obj.q.x) + "," + cleanDouble(obj.q.y) + "," + cleanDouble(obj.q.z) + "," + cleanDouble(obj.q.w)));

        /* @doc tag
         *
         * @Name vector
         * @RawName <QuaternionTag.vector>
         * @Object QuaternionTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Converts the X, Y, and Z components of this quaternion into a LocationTag (vector).
         * The W component is ignored.
         *
         * @Implements QuaternionTag.xyz_vector
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "vector", (attr, obj) ->
                new LocationTag(new Location(null, obj.q.x, obj.q.y, obj.q.z)));

        /* @doc tag
         *
         * @Name add[]
         * @RawName <QuaternionTag.add[<quaternion>]>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Returns this quaternion mathematically added to another quaternion (component-wise addition).
         * Note: This is NOT the same as combining rotations. To combine rotations, use {@link tag QuaternionTag.mul}.
         *
         * @Implements QuaternionTag.plus
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "add", (attr, obj) -> {
            QuaternionTag other = attr.getParamObject(QuaternionTag.class, QuaternionTag::new);
            if (other == null) return null;
            return new QuaternionTag(new Quaterniond(obj.q).add(other.q));
        });

        /* @doc tag
         *
         * @Name mul[]
         * @RawName <QuaternionTag.mul[<quaternion>]>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Returns this quaternion multiplied by another quaternion.
         * This effectively combines the rotations represented by the two quaternions.
         *
         * @Implements QuaternionTag.mul
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "mul", (attr, obj) -> {
            QuaternionTag other = attr.getParamObject(QuaternionTag.class, QuaternionTag::new);
            if (other == null) return null;
            return new QuaternionTag(new Quaterniond(obj.q).mul(other.q));
        });

        /* @doc tag
         *
         * @Name scale[]
         * @RawName <QuaternionTag.scale[<decimal>]>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Returns this quaternion with all its components multiplied by the given scaling factor.
         * Note: This does not simply increase the rotation angle.
         *
         * @Implements QuaternionTag.scale
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "scale", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag scale = new ElementTag(attr.getParam());
            if (!scale.isDouble()) return null;
            double s = scale.asDouble();
            return new QuaternionTag(obj.q.x * s, obj.q.y * s, obj.q.z * s, obj.q.w * s);
        });

        /* @doc tag
         *
         * @Name normalize
         * @RawName <QuaternionTag.normalize>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @NoArg
         * @Description
         * Returns a normalized copy of this quaternion (length equal to 1).
         * Useful to ensure valid rotations after mathematical operations.
         *
         * @Implements QuaternionTag.normalize
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "normalize", (attr, obj) ->
                new QuaternionTag(new Quaterniond(obj.q).normalize()));

        /* @doc tag
         *
         * @Name length
         * @RawName <QuaternionTag.length>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the geometric length (magnitude) of this quaternion.
         *
         * @Implements QuaternionTag.length
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "length", (attr, obj) ->
                new ElementTag(Math.sqrt(obj.q.x * obj.q.x + obj.q.y * obj.q.y + obj.q.z * obj.q.z + obj.q.w * obj.q.w)));

        /* @doc tag
         *
         * @Name lengthSquared
         * @RawName <QuaternionTag.lengthSquared>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the squared geometric length (magnitude) of this quaternion.
         * Mathematically cheaper than calculating the true length.
         *
         * @Implements QuaternionTag.length_squared
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "lengthSquared", (attr, obj) -> new ElementTag(obj.q.lengthSquared()));

        /* @doc tag
         *
         * @Name conjugate
         * @RawName <QuaternionTag.conjugate>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @NoArg
         * @Description
         * Returns the mathematical conjugate of this quaternion.
         * This is equivalent to negating its X, Y, and Z components (-x, -y, -z, w).
         *
         * @Implements QuaternionTag.conjugate
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "conjugate", (attr, obj) ->
                new QuaternionTag(new Quaterniond(obj.q).conjugate()));

        /* @doc tag
         *
         * @Name inverse
         * @RawName <QuaternionTag.inverse>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @NoArg
         * @Description
         * Returns the inverse of this quaternion.
         * The inverse represents the exact same amount of rotation, but applied in the opposite direction.
         *
         * @Implements QuaternionTag.inverse
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "inverse", (attr, obj) ->
                new QuaternionTag(new Quaterniond(obj.q).invert()));

        /* @doc tag
         *
         * @Name negative
         * @RawName <QuaternionTag.negative>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @NoArg
         * @Description
         * Returns the negative of this quaternion (-x, -y, -z, -w).
         * Note: If you want a reverse rotation, use the `inverse` tag instead.
         *
         * @Implements QuaternionTag.negative
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "negative", (attr, obj) ->
                new QuaternionTag(-obj.q.x, -obj.q.y, -obj.q.z, -obj.q.w));

        /* @doc tag
         *
         * @Name quaternionBetween[]
         * @RawName <QuaternionTag.quaternionBetween[<quaternion>]>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Returns the quaternion representing the rotational difference between this quaternion and the provided target quaternion.
         *
         * @Implements QuaternionTag.quaternion_between
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "quaternionBetween", (attr, obj) -> {
            QuaternionTag other = attr.getParamObject(QuaternionTag.class, QuaternionTag::new);
            if (other == null) return null;
            return new QuaternionTag(new Quaterniond(obj.q).mul(new Quaterniond(other.q).conjugate()));
        });

        /* @doc tag
         *
         * @Name transform[]
         * @RawName <QuaternionTag.transform[<location/vector>]>
         * @Object QuaternionTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Takes the given location/vector and applies this quaternion's rotation to it, returning the newly transformed vector.
         *
         * @Implements QuaternionTag.transform
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "transform", (attr, obj) -> {
            LocationTag v = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (v == null) return null;

            Vector3d vec = new Vector3d(v.getLocation().getX(), v.getLocation().getY(), v.getLocation().getZ());
            obj.q.transform(vec);

            return new LocationTag(new Location(null, vec.x, vec.y, vec.z));
        });

        /* @doc tag
         *
         * @Name representedAngle
         * @RawName <QuaternionTag.representedAngle>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Retrieves the angle (in radians) represented by this quaternion's rotation.
         * Best used alongside the {@link tag QuaternionTag.representedAxis} tag.
         *
         * @Implements QuaternionTag.represented_angle
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "representedAngle", (attr, obj) ->
                new ElementTag(obj.q.angle()));

        /* @doc tag
         *
         * @Name representedAxis
         * @RawName <QuaternionTag.representedAxis>
         * @Object QuaternionTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Retrieves the directional axis represented by this quaternion's rotation as a vector (LocationTag).
         * Best used alongside the {@link tag QuaternionTag.representedAngle} tag.
         *
         * @Implements QuaternionTag.represented_axis
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "representedAxis", (attr, obj) -> {
            double sign = Math.signum(obj.q.w);
            double x = obj.q.x * sign;
            double y = obj.q.y * sign;
            double z = obj.q.z * sign;

            double len = Math.sqrt(x * x + y * y + z * z);
            if (len == 0) len = 1;
            len = 1 / len;

            return new LocationTag(new Location(null, x * len, y * len, z * len));
        });

        /* @doc tag
         *
         * @Name axisAngleFor[]
         * @RawName <QuaternionTag.axisAngleFor[<location/vector>]>
         * @Object QuaternionTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the angle (in radians) of rotation around the specified axis vector created by this quaternion.
         *
         * @Implements QuaternionTag.axis_angle_for
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "axisAngleFor", (attr, obj) -> {
            LocationTag axisLoc = attr.getParamObject(LocationTag.class, LocationTag::new);
            if (axisLoc == null) return null;

            Vector3d axis = new Vector3d(axisLoc.getLocation().getX(), axisLoc.getLocation().getY(), axisLoc.getLocation().getZ());
            double lenSq = axis.lengthSquared();
            if (lenSq == 0) return new ElementTag(0);

            Vector3d raVec = new Vector3d(obj.q.x, obj.q.y, obj.q.z);
            Vector3d p = new Vector3d(axis).mul(raVec.dot(axis) / lenSq);

            Quaterniond twist = new Quaterniond(p.x, p.y, p.z, obj.q.w).normalize();
            Vector3d newForward = twist.transform(new Vector3d(1, 0, 0));

            return new ElementTag(vecToAngle(newForward.x, newForward.y));
        });

        /* @doc tag
         *
         * @Name slerp[].amount[]
         * @RawName <QuaternionTag.slerp[<quaternion>].amount[<decimal>]>
         * @Object QuaternionTag
         * @ReturnType QuaternionTag
         * @ArgRequired
         * @Description
         * Computes the Spherical Linear Interpolation (Slerp) between this quaternion and the target quaternion.
         * The `amount` dictates the progression (e.g. 0.5 represents exactly halfway between the two rotations).
         */
        TAG_PROCESSOR.registerTag(QuaternionTag.class, "slerp", (attr, obj) -> {
            QuaternionTag end = attr.getParamObject(QuaternionTag.class, QuaternionTag::new);
            if (end == null || !attr.matchesNext("amount") || !attr.hasNextParam()) return null;

            ElementTag amountTag = new ElementTag(attr.getNextParam());
            if (!amountTag.isDouble()) return null;

            attr.fulfill(1);
            return new QuaternionTag(new Quaterniond(obj.q).slerp(end.q, amountTag.asDouble()));
        });
    }

    public QuaternionTag(Quaterniond q) {
        this.q = new Quaterniond(q);
    }

    public QuaternionTag(double x, double y, double z, double w) {
        this.q = new Quaterniond(x, y, z, w);
    }

    public QuaternionTag(String raw) {
        Quaterniond tempQ = new Quaterniond(0, 0, 0, 1);

        if (raw != null && !raw.isEmpty() && !raw.equalsIgnoreCase("identity") && !raw.equalsIgnoreCase(prefix + "@identity")) {
            String parseStr = raw;
            if (parseStr.toLowerCase().startsWith(prefix + "@")) {
                parseStr = parseStr.substring(prefix.length() + 1);
            }

            String[] split = parseStr.split(",");
            if (split.length == 4) {
                try {
                    tempQ = new Quaterniond(
                            Double.parseDouble(split[0]),
                            Double.parseDouble(split[1]),
                            Double.parseDouble(split[2]),
                            Double.parseDouble(split[3])
                    );
                } catch (NumberFormatException ignored) {}
            }
        }

        this.q = tempQ;
    }

    public Quaterniond getQuaternion() {
        return new Quaterniond(q);
    }

    private static String cleanDouble(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.valueOf(d);
    }

    private static double vecToAngle(double x, double y) {
        if (x == 0 && y == 0) return 0;
        if (x != 0) return Math.atan2(y, x);
        if (y > 0) return 0;
        return Math.PI;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + cleanDouble(q.x) + "," + cleanDouble(q.y) + "," + cleanDouble(q.z) + "," + cleanDouble(q.w);
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<QuaternionTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "quat@0,0,0,1";
    }
}
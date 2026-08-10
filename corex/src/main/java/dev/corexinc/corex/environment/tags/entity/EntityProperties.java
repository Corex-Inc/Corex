package dev.corexinc.corex.environment.tags.entity;

import dev.corexinc.corex.api.properties.PropertyType;
import dev.corexinc.corex.api.properties.PropertyTypes;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.core.QuaternionTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.entities.LiveEntityView;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static dev.corexinc.corex.environment.tags.entity.EntityTag.clearableProperty;
import static dev.corexinc.corex.environment.tags.entity.EntityTag.property;
import static dev.corexinc.corex.environment.tags.entity.EntityTag.readOnlyProperty;

final class EntityProperties {

    private EntityProperties() {}

    static final PropertyType<Vector3f> VECTOR3F = PropertyTypes.mapping(
            PropertyTypes.LOCATION,
            location -> location.getVector().toVector3f(),
            vector -> new LocationTag(vector.x(), vector.y(), vector.z(), 0, 0));

    static final PropertyType<Quaternionf> QUATERNION = PropertyTypes.mapping(
            PropertyTypes.QUATERNION,
            QuaternionTag::getQuaternionf,
            quaternion -> new QuaternionTag(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w()));

    static final PropertyType<Color> BUKKIT_COLOR = PropertyTypes.mapping(
            PropertyTypes.COLOR,
            colorTag -> Color.fromARGB(colorTag.alpha, colorTag.red, colorTag.green, colorTag.blue),
            color -> new dev.corexinc.corex.environment.tags.core.ColorTag(
                    color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));

    static final PropertyType<org.bukkit.inventory.ItemStack> ITEM_STACK = PropertyTypes.mapping(
            PropertyTypes.ITEM, ItemTag::getItemStack, ItemTag::new);

    static final PropertyType<BlockData> BLOCK_DATA = PropertyTypes.mapping(
            PropertyTypes.STRING,
            raw -> {
                try {
                    return Bukkit.createBlockData(raw.toLowerCase());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            },
            BlockData::getAsString);

    static final PropertyType<Byte> OPACITY = PropertyTypes.mapping(
            PropertyTypes.range(PropertyTypes.INTEGER, -1, 255),
            Integer::byteValue,
            opacity -> (int) opacity);

    static final PropertyType<DyeColor> DYE_COLOR = PropertyTypes.enumOf(DyeColor.class);

    static void register() {
        registerGeneral();
        registerLiving();
        registerDisplay();
        registerTextDisplay();
        registerOtherDisplays();
        EntitySpeciesProperties.register();
    }

    private static void registerGeneral() {

        /* @doc tag
         *
         * @Name glowing
         * @RawName <EntityTag.glowing>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the entity has the glowing outline effect.
         *
         * @Implements EntityTag.glowing
         */
        /* @doc mechanism
         *
         * @Name glowing
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity has the glowing outline effect.
         * The outline is white unless a team color or <@link mechanism EntityTag.glowColor> says otherwise.
         *
         * @Usage
         * - adjust <[entity]> glowing:true
         *
         * @Implements EntityTag.glowing
         */
        property("glowing", Entity.class, PropertyTypes.BOOLEAN,
                Entity::isGlowing, LiveEntityView::setGlowing);

        /* @doc tag
         *
         * @Name gravity
         * @RawName <EntityTag.gravity>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the entity is affected by gravity.
         *
         * @Implements EntityTag.gravity
         */
        /* @doc mechanism
         *
         * @Name gravity
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity is affected by gravity. An entity without gravity keeps whatever
         * velocity it already had, so set velocity to zero as well if you want it to hang still.
         *
         * @Implements EntityTag.gravity
         */
        property("gravity", Entity.class, PropertyTypes.BOOLEAN,
                Entity::hasGravity, LiveEntityView::setGravity);

        /* @doc tag
         *
         * @Name invulnerable
         * @RawName <EntityTag.invulnerable>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the entity is immune to damage.
         *
         * @Implements EntityTag.invulnerable
         */
        /* @doc mechanism
         *
         * @Name invulnerable
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity is immune to all damage sources except the void and /kill.
         *
         * @Implements EntityTag.invulnerable
         */
        property("invulnerable", Entity.class, PropertyTypes.BOOLEAN,
                Entity::isInvulnerable, LiveEntityView::setInvulnerable);

        /* @doc tag
         *
         * @Name silent
         * @RawName <EntityTag.silent>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the entity is silenced.
         *
         * @Implements EntityTag.silent
         */
        /* @doc mechanism
         *
         * @Name silent
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity produces sounds.
         *
         * @Implements EntityTag.silent
         */
        property("silent", Entity.class, PropertyTypes.BOOLEAN,
                Entity::isSilent, LiveEntityView::setSilent);

        /* @doc tag
         *
         * @Name customNameVisible
         * @RawName <EntityTag.customNameVisible>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the entity's custom name is always shown.
         *
         * @Implements EntityTag.custom_name_visible
         */
        /* @doc mechanism
         *
         * @Name customNameVisible
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity's custom name is always visible, rather than only when looked at.
         *
         * @Implements EntityTag.custom_name_visible
         */
        property("customNameVisible", Entity.class, PropertyTypes.BOOLEAN,
                Entity::isCustomNameVisible, LiveEntityView::setCustomNameVisible);

        /* @doc tag
         *
         * @Name visibleByDefault
         * @RawName <EntityTag.visibleByDefault>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the entity is visible to players that have not been told otherwise.
         */
        /* @doc mechanism
         *
         * @Name visibleByDefault
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity is shown to players by default. Setting this to 'false' hides it
         * from everyone except players explicitly shown the entity, which is the usual way to build
         * per-player visuals out of real entities.
         */
        property("visibleByDefault", Entity.class, PropertyTypes.BOOLEAN,
                Entity::isVisibleByDefault, LiveEntityView::setVisibleByDefault);

        /* @doc tag
         *
         * @Name fireTicks
         * @RawName <EntityTag.fireTicks>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many ticks the entity will keep burning for.
         *
         * @Implements EntityTag.fire_time
         */
        /* @doc mechanism
         *
         * @Name fireTicks
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long the entity stays on fire. Accepts a duration ("3s") or a plain tick count.
         *
         * @Implements EntityTag.fire_time
         */
        property("fireTicks", Entity.class, PropertyTypes.TICKS,
                Entity::getFireTicks, LiveEntityView::setFireTicks);

        /* @doc tag
         *
         * @Name freezeTicks
         * @RawName <EntityTag.freezeTicks>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many ticks of powder snow freezing the entity has built up.
         *
         * @Implements EntityTag.freeze_duration
         */
        /* @doc mechanism
         *
         * @Name freezeTicks
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many ticks of powder snow freezing the entity has accumulated.
         * The full freeze effect starts at 140 ticks for most entities.
         *
         * @Implements EntityTag.freeze_duration
         */
        property("freezeTicks", Entity.class, PropertyTypes.TICKS,
                Entity::getFreezeTicks, LiveEntityView::setFreezeTicks);

        /* @doc tag
         *
         * @Name ticksLived
         * @RawName <EntityTag.ticksLived>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many ticks the entity has existed for.
         *
         * @Implements EntityTag.time_lived
         */
        /* @doc mechanism
         *
         * @Name ticksLived
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the entity's age in ticks. Values below 1 are raised to 1, since the server treats 0 as unset.
         * On entities with a lifetime (dropped items, arrows) this shortens or extends how long they last.
         *
         * @Implements EntityTag.time_lived
         */
        property("ticksLived", Entity.class, PropertyTypes.TICKS,
                Entity::getTicksLived, LiveEntityView::setTicksLived);

        /* @doc tag
         *
         * @Name portalCooldown
         * @RawName <EntityTag.portalCooldown>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the remaining ticks before the entity may use a portal again.
         */
        /* @doc mechanism
         *
         * @Name portalCooldown
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long until the entity is allowed to use a portal again.
         */
        property("portalCooldown", Entity.class, PropertyTypes.TICKS,
                Entity::getPortalCooldown, LiveEntityView::setPortalCooldown);

        /* @doc tag
         *
         * @Name fallDistance
         * @RawName <EntityTag.fallDistance>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns how far the entity has fallen, in blocks. This is what fall damage is calculated from.
         *
         * @Implements EntityTag.fall_distance
         */
        /* @doc mechanism
         *
         * @Name fallDistance
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the distance the entity has fallen. Set it to 0 to cancel pending fall damage.
         *
         * @Implements EntityTag.fall_distance
         */
        property("fallDistance", Entity.class, PropertyTypes.FLOAT,
                Entity::getFallDistance, LiveEntityView::setFallDistance);

        /* @doc tag
         *
         * @Name velocity
         * @RawName <EntityTag.velocity>
         * @Object EntityTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the entity's current velocity as a vector, in blocks per tick.
         *
         * @Implements EntityTag.velocity
         */
        /* @doc mechanism
         *
         * @Name velocity
         * @Object EntityTag
         * @Input LocationTag
         * @Description
         * Sets the entity's velocity to the vector of the given location, in blocks per tick.
         * Note that a value above about 4 gets clamped by the client.
         *
         * @Usage
         * // Launch the entity upwards.
         * - adjust <[entity]> velocity:0,1.2,0
         *
         * @Implements EntityTag.velocity
         */
        property("velocity", Entity.class, PropertyTypes.LOCATION,
                entity -> {
                    Vector velocity = entity.getVelocity();
                    return new LocationTag(velocity.getX(), velocity.getY(), velocity.getZ(), 0, 0);
                },
                (view, location) -> view.setVelocity(location.getLocation().toVector()));

        /* @doc tag
         *
         * @Name rotation
         * @RawName <EntityTag.rotation>
         * @Object EntityTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the entity's body rotation as a location carrying only yaw and pitch.
         *
         * @Implements EntityTag.rotation
         */
        /* @doc mechanism
         *
         * @Name rotation
         * @Object EntityTag
         * @Input LocationTag
         * @Description
         * Sets the entity's body rotation to the yaw and pitch of the given location.
         * The position part of the input is ignored.
         *
         * @Implements EntityTag.rotation
         */
        property("rotation", Entity.class, PropertyTypes.LOCATION,
                entity -> new LocationTag(0, 0, 0, entity.getLocation().getYaw(), entity.getLocation().getPitch()),
                (view, location) -> {
                    Location rotation = location.getLocation();
                    view.setRotation(rotation.getYaw(), rotation.getPitch());
                });

        /* @doc mechanism
         *
         * @Name visualFire
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity is rendered on fire without actually burning.
         * Purely visual - it deals no damage and burns for no set time.
         *
         * @Implements EntityTag.visual_fire
         */
        EntityTag.PROPERTIES.property("visualFire", PropertyTypes.BOOLEAN)
                .read(object -> null)
                .write(LiveEntityView::setVisualFire)
                .register();
    }

    private static void registerLiving() {

        /* @doc tag
         *
         * @Name health
         * @RawName <EntityTag.health>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the current health of a living entity.
         *
         * @Implements EntityTag.health
         */
        /* @doc mechanism
         *
         * @Name health
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the current health of a living entity, clamped to its maximum health.
         * Setting it to 0 kills the entity with a generic death cause.
         *
         * @Implements EntityTag.health
         */
        property("health", LivingEntity.class, PropertyTypes.DOUBLE,
                LivingEntity::getHealth, LiveEntityView::setHealth);

        /* @doc tag
         *
         * @Name maxHealth
         * @RawName <EntityTag.maxHealth>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the base maximum health attribute of a living entity.
         *
         * @Implements EntityTag.max_health
         */
        /* @doc mechanism
         *
         * @Name maxHealth
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the base maximum health of a living entity. Current health is not raised to match,
         * so set health afterwards if you want the entity full.
         *
         * @Implements EntityTag.max_health
         */
        property("maxHealth", LivingEntity.class, PropertyTypes.DOUBLE,
                living -> {
                    AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
                    return maxHealth != null ? maxHealth.getBaseValue() : null;
                },
                LiveEntityView::setMaxHealth);

        /* @doc tag
         *
         * @Name ai
         * @RawName <EntityTag.ai>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity runs its AI.
         *
         * @Implements EntityTag.has_ai
         */
        /* @doc mechanism
         *
         * @Name ai
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity runs its AI - movement, targeting, and goals.
         * A mob with AI off still takes damage and still falls.
         *
         * @Implements EntityTag.has_ai
         */
        property("ai", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::hasAI, LiveEntityView::setAI);

        /* @doc tag
         *
         * @Name air
         * @RawName <EntityTag.air>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the remaining air of a living entity, in ticks.
         *
         * @Implements EntityTag.oxygen
         */
        /* @doc mechanism
         *
         * @Name air
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the remaining air of a living entity. Most mobs cap at 300 ticks (15 seconds).
         *
         * @Implements EntityTag.oxygen
         */
        property("air", LivingEntity.class, PropertyTypes.TICKS,
                LivingEntity::getRemainingAir, LiveEntityView::setRemainingAir);

        /* @doc tag
         *
         * @Name invisible
         * @RawName <EntityTag.invisible>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity is invisible.
         *
         * @Implements EntityTag.invisible
         */
        /* @doc mechanism
         *
         * @Name invisible
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity is invisible. Held and worn items still render,
         * as does the entity's outline while it is glowing.
         *
         * @Implements EntityTag.invisible
         */
        property("invisible", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::isInvisible, LiveEntityView::setInvisible);

        /* @doc tag
         *
         * @Name gliding
         * @RawName <EntityTag.gliding>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity is gliding, as with an elytra.
         *
         * @Implements EntityTag.gliding
         */
        /* @doc mechanism
         *
         * @Name gliding
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity is in the gliding (elytra) pose and movement mode.
         *
         * @Implements EntityTag.gliding
         */
        property("gliding", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::isGliding, LiveEntityView::setGliding);

        /* @doc tag
         *
         * @Name swimming
         * @RawName <EntityTag.swimming>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity is in the swimming pose.
         *
         * @Implements EntityTag.swimming
         */
        /* @doc mechanism
         *
         * @Name swimming
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity is in the swimming (crawling) pose.
         *
         * @Implements EntityTag.swimming
         */
        property("swimming", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::isSwimming, LiveEntityView::setSwimming);

        /* @doc tag
         *
         * @Name collidable
         * @RawName <EntityTag.collidable>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity collides with others.
         *
         * @Implements EntityTag.collidable
         */
        /* @doc mechanism
         *
         * @Name collidable
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity can be pushed by, and push, other entities.
         *
         * @Implements EntityTag.collidable
         */
        property("collidable", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::isCollidable, LiveEntityView::setCollidable);

        /* @doc tag
         *
         * @Name canPickupItems
         * @RawName <EntityTag.canPickupItems>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity picks up items it walks over.
         *
         * @Implements EntityTag.can_pickup_items
         */
        /* @doc mechanism
         *
         * @Name canPickupItems
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity picks up items from the ground.
         *
         * @Implements EntityTag.can_pickup_items
         */
        property("canPickupItems", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::getCanPickupItems, LiveEntityView::setCanPickupItems);

        /* @doc tag
         *
         * @Name removeWhenFarAway
         * @RawName <EntityTag.removeWhenFarAway>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a living entity despawns when no player is nearby.
         *
         * @Implements EntityTag.remove_when_far_away
         */
        /* @doc mechanism
         *
         * @Name removeWhenFarAway
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity is allowed to despawn naturally.
         * Set it to 'false' to keep a scripted mob around permanently.
         *
         * @Implements EntityTag.remove_when_far_away
         */
        property("removeWhenFarAway", LivingEntity.class, PropertyTypes.BOOLEAN,
                LivingEntity::getRemoveWhenFarAway, LiveEntityView::setRemoveWhenFarAway);

        /* @doc tag
         *
         * @Name bodyArrows
         * @RawName <EntityTag.bodyArrows>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many arrows are stuck in a living entity.
         *
         * @Implements EntityTag.body_arrows
         */
        /* @doc mechanism
         *
         * @Name bodyArrows
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many arrows appear stuck in a living entity.
         *
         * @Implements EntityTag.body_arrows
         */
        property("bodyArrows", LivingEntity.class, PropertyTypes.INTEGER,
                LivingEntity::getArrowsInBody, LiveEntityView::setArrowsInBody);

        /* @doc tag
         *
         * @Name bodyStingers
         * @RawName <EntityTag.bodyStingers>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many bee stingers are stuck in a living entity.
         */
        /* @doc mechanism
         *
         * @Name bodyStingers
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many bee stingers appear stuck in a living entity.
         */
        property("bodyStingers", LivingEntity.class, PropertyTypes.INTEGER,
                LivingEntity::getBeeStingersInBody, LiveEntityView::setBeeStingersInBody);

        /* @doc tag
         *
         * @Name absorption
         * @RawName <EntityTag.absorption>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the absorption (golden heart) health of a living entity.
         *
         * @Implements EntityTag.absorption_health
         */
        /* @doc mechanism
         *
         * @Name absorption
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the absorption health of a living entity - the extra golden hearts consumed before real health.
         *
         * @Implements EntityTag.absorption_health
         */
        property("absorption", LivingEntity.class, PropertyTypes.DOUBLE,
                LivingEntity::getAbsorptionAmount, LiveEntityView::setAbsorptionAmount);

        /* @doc tag
         *
         * @Name noDamage
         * @RawName <EntityTag.noDamage>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the remaining invulnerability ticks after a hit.
         *
         * @Implements EntityTag.no_damage_duration
         */
        /* @doc mechanism
         *
         * @Name noDamage
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the remaining damage-immunity ticks. Set it to 0 to let an entity be hit again immediately.
         *
         * @Implements EntityTag.no_damage_duration
         */
        property("noDamage", LivingEntity.class, PropertyTypes.TICKS,
                LivingEntity::getNoDamageTicks, LiveEntityView::setNoDamageTicks);

        /* @doc tag
         *
         * @Name maxNoDamage
         * @RawName <EntityTag.maxNoDamage>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how long a living entity stays immune after taking damage. Defaults to 20.
         *
         * @Implements EntityTag.max_no_damage_duration
         */
        /* @doc mechanism
         *
         * @Name maxNoDamage
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long a living entity is immune to damage after being hit. The vanilla default is 20 ticks.
         *
         * @Implements EntityTag.max_no_damage_duration
         */
        property("maxNoDamage", LivingEntity.class, PropertyTypes.TICKS,
                LivingEntity::getMaximumNoDamageTicks, LiveEntityView::setMaxNoDamageTicks);
    }

    private static void registerDisplay() {

        /* @doc tag
         *
         * @Name viewRange
         * @RawName <EntityTag.viewRange>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the view range multiplier of a Display entity.
         *
         * @Implements EntityTag.view_range
         */
        /* @doc mechanism
         *
         * @Name viewRange
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the view range of a Display entity, as a multiplier of the client's entity render distance.
         * 1.0 is the default; smaller values make the display vanish sooner.
         *
         * @Implements EntityTag.view_range
         */
        property("viewRange", Display.class, PropertyTypes.FLOAT,
                Display::getViewRange, LiveEntityView::setViewRange);

        /* @doc tag
         *
         * @Name billboard
         * @RawName <EntityTag.billboard>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the billboard mode of a Display entity: FIXED, VERTICAL, HORIZONTAL, or CENTER.
         *
         * @Implements EntityTag.pivot
         */
        /* @doc mechanism
         *
         * @Name billboard
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets which axes a Display entity pivots around to face the viewer.
         * FIXED (the default) never turns, VERTICAL turns around the Y axis, HORIZONTAL around X,
         * and CENTER always faces the player.
         *
         * @Usage
         * // A nameplate that always faces the player.
         * - adjust <[display]> billboard:center
         *
         * @Implements EntityTag.pivot
         */
        property("billboard", Display.class, PropertyTypes.enumOf(Display.Billboard.class),
                Display::getBillboard, LiveEntityView::setBillboard);

        /* @doc tag
         *
         * @Name shadowRadius
         * @RawName <EntityTag.shadowRadius>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the radius of the shadow drawn under a Display entity.
         *
         * @Implements EntityTag.shadow_radius
         */
        /* @doc mechanism
         *
         * @Name shadowRadius
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the radius of the shadow under a Display entity, in blocks. 0 removes the shadow.
         *
         * @Implements EntityTag.shadow_radius
         */
        property("shadowRadius", Display.class, PropertyTypes.FLOAT,
                Display::getShadowRadius, LiveEntityView::setShadowRadius);

        /* @doc tag
         *
         * @Name shadowStrength
         * @RawName <EntityTag.shadowStrength>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the opacity of the shadow under a Display entity.
         *
         * @Implements EntityTag.shadow_strength
         */
        /* @doc mechanism
         *
         * @Name shadowStrength
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets how dark the shadow under a Display entity is. 1.0 is the vanilla default.
         *
         * @Implements EntityTag.shadow_strength
         */
        property("shadowStrength", Display.class, PropertyTypes.FLOAT,
                Display::getShadowStrength, LiveEntityView::setShadowStrength);

        /* @doc tag
         *
         * @Name displayWidth
         * @RawName <EntityTag.displayWidth>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the culling box width of a Display entity.
         */
        /* @doc mechanism
         *
         * @Name displayWidth
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the width of the box the client uses to decide whether a Display entity is on screen.
         * This is culling only - it does not scale or clip the rendered content. A value of 0 disables culling.
         * Scaled-up displays usually need this raised, or they vanish when you look away from their origin.
         */
        property("displayWidth", Display.class, PropertyTypes.FLOAT,
                Display::getDisplayWidth, LiveEntityView::setDisplayWidth);

        /* @doc tag
         *
         * @Name displayHeight
         * @RawName <EntityTag.displayHeight>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the culling box height of a Display entity.
         */
        /* @doc mechanism
         *
         * @Name displayHeight
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the height of the culling box of a Display entity. See <@link mechanism EntityTag.displayWidth>.
         */
        property("displayHeight", Display.class, PropertyTypes.FLOAT,
                Display::getDisplayHeight, LiveEntityView::setDisplayHeight);

        /* @doc tag
         *
         * @Name glowColor
         * @RawName <EntityTag.glowColor>
         * @Object EntityTag
         * @ReturnType ColorTag
         * @NoArg
         * @Description
         * Returns the custom glow outline color of a Display entity.
         *
         * @Implements EntityTag.glow_color
         */
        /* @doc mechanism
         *
         * @Name glowColor
         * @Object EntityTag
         * @Input ColorTag
         * @Description
         * Sets the glow outline color of a Display entity, overriding its team color.
         * Only visible while the entity is glowing. Takes any RGB color, and '!' to drop the
         * override and go back to the team color.
         *
         * @Implements EntityTag.glow_color
         */
        clearableProperty("glowColor", Display.class, BUKKIT_COLOR,
                Display::getGlowColorOverride, LiveEntityView::setGlowColorOverride);

        /* @doc tag
         *
         * @Name teleportDuration
         * @RawName <EntityTag.teleportDuration>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how long a Display entity takes to interpolate to a new position.
         *
         * @Implements EntityTag.teleport_duration
         */
        /* @doc mechanism
         *
         * @Name teleportDuration
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many ticks a Display entity spends smoothly moving to a new position after being teleported.
         * 0 snaps instantly. Valid values are 0 to 59; higher values are ignored by the client.
         *
         * @Implements EntityTag.teleport_duration
         */
        property("teleportDuration", Display.class, PropertyTypes.TICKS,
                Display::getTeleportDuration, LiveEntityView::setTeleportDuration);

        /* @doc tag
         *
         * @Name interpolationDuration
         * @RawName <EntityTag.interpolationDuration>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how long a Display entity takes to animate to a new transformation.
         *
         * @Implements EntityTag.interpolation_duration
         */
        /* @doc mechanism
         *
         * @Name interpolationDuration
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets, in ticks, how long a Display entity takes to animate from its old transformation to a new one.
         * Applies to <@link mechanism EntityTag.translation>, <@link mechanism EntityTag.scale>,
         * <@link mechanism EntityTag.leftRotation>, and <@link mechanism EntityTag.rightRotation>.
         * 0 snaps instantly.
         *
         * @Implements EntityTag.interpolation_duration
         */
        property("interpolationDuration", Display.class, PropertyTypes.TICKS,
                Display::getInterpolationDuration, LiveEntityView::setInterpolationDuration);

        /* @doc tag
         *
         * @Name interpolationStart
         * @RawName <EntityTag.interpolationStart>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the delay before a Display entity begins interpolating.
         *
         * @Implements EntityTag.interpolation_start
         */
        /* @doc mechanism
         *
         * @Name interpolationStart
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many ticks a Display entity waits after a transformation update before it starts
         * animating towards it.
         *
         * @Implements EntityTag.interpolation_start
         */
        property("interpolationStart", Display.class, PropertyTypes.TICKS,
                Display::getInterpolationDelay, LiveEntityView::setInterpolationDelay);

        /* @doc tag
         *
         * @Name translation
         * @RawName <EntityTag.translation>
         * @Object EntityTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the translation (positional offset) of a Display entity's transformation.
         *
         * @Implements EntityTag.translation
         */
        /* @doc mechanism
         *
         * @Name translation
         * @Object EntityTag
         * @Input LocationTag
         * @Description
         * Sets the translation of a Display entity's transformation, leaving scale and rotation alone.
         * The offset is in blocks, relative to the entity's own position.
         *
         * @Implements EntityTag.translation
         */
        property("translation", Display.class, VECTOR3F,
                display -> display.getTransformation().getTranslation(), LiveEntityView::setTranslation);

        /* @doc tag
         *
         * @Name scale
         * @RawName <EntityTag.scale>
         * @Object EntityTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the scale of a Display entity's transformation.
         *
         * @Implements EntityTag.scale
         */
        /* @doc mechanism
         *
         * @Name scale
         * @Object EntityTag
         * @Input LocationTag
         * @Description
         * Sets the scale of a Display entity's transformation, leaving translation and rotation alone.
         * Scaling up usually also needs <@link mechanism EntityTag.displayWidth> raised, or the display
         * gets culled when its origin leaves the screen.
         *
         * @Usage
         * // Twice as large on every axis.
         * - adjust <[display]> scale:2,2,2
         *
         * @Implements EntityTag.scale
         */
        property("scale", Display.class, VECTOR3F,
                display -> display.getTransformation().getScale(), LiveEntityView::setScale);

        /* @doc tag
         *
         * @Name leftRotation
         * @RawName <EntityTag.leftRotation>
         * @Object EntityTag
         * @ReturnType QuaternionTag
         * @NoArg
         * @Description
         * Returns the left rotation (applied before scale) of a Display entity's transformation.
         *
         * @Implements EntityTag.left_rotation
         */
        /* @doc mechanism
         *
         * @Name leftRotation
         * @Object EntityTag
         * @Input QuaternionTag
         * @Description
         * Sets the left rotation of a Display entity's transformation - the one applied before scaling.
         *
         * @Implements EntityTag.left_rotation
         */
        property("leftRotation", Display.class, QUATERNION,
                display -> display.getTransformation().getLeftRotation(), LiveEntityView::setLeftRotation);

        /* @doc tag
         *
         * @Name rightRotation
         * @RawName <EntityTag.rightRotation>
         * @Object EntityTag
         * @ReturnType QuaternionTag
         * @NoArg
         * @Description
         * Returns the right rotation (applied after scale) of a Display entity's transformation.
         *
         * @Implements EntityTag.right_rotation
         */
        /* @doc mechanism
         *
         * @Name rightRotation
         * @Object EntityTag
         * @Input QuaternionTag
         * @Description
         * Sets the right rotation of a Display entity's transformation - the one applied after scaling.
         *
         * @Implements EntityTag.right_rotation
         */
        property("rightRotation", Display.class, QUATERNION,
                display -> display.getTransformation().getRightRotation(), LiveEntityView::setRightRotation);

        /* @doc tag
         *
         * @Name brightness
         * @RawName <EntityTag.brightness>
         * @Object EntityTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns the brightness override of a Display entity as a map with 'block' and 'sky' keys,
         * or nothing when the display uses the world's natural lighting.
         *
         * @Implements EntityTag.brightness
         */
        readOnlyProperty("brightness", Display.class, PropertyTypes.MAP, display -> {
            Display.Brightness brightness = display.getBrightness();
            if (brightness == null) return null;
            MapTag result = new MapTag();
            result.putObject("block", new ElementTag(brightness.getBlockLight()));
            result.putObject("sky", new ElementTag(brightness.getSkyLight()));
            return result;
        });
    }

    private static void registerTextDisplay() {

        /* @doc tag
         *
         * @Name text
         * @RawName <EntityTag.text>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the displayed text of a text_display entity.
         *
         * @Implements EntityTag.text
         */
        /* @doc mechanism
         *
         * @Name text
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the text shown by a text_display entity. Colors and formatters are kept intact.
         *
         * @Usage
         * - adjust <[display]> text:"<&gradient[#ff0000;#0000ff]>Hello"
         *
         * @Implements EntityTag.text
         */
        property("text", TextDisplay.class, PropertyTypes.TEXT,
                TextDisplay::text, LiveEntityView::setText);

        /* @doc tag
         *
         * @Name textShadowed
         * @RawName <EntityTag.textShadowed>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a text_display entity's text has a drop shadow.
         *
         * @Implements EntityTag.text_shadowed
         */
        /* @doc mechanism
         *
         * @Name textShadowed
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a text_display entity's text is drawn with a drop shadow.
         *
         * @Implements EntityTag.text_shadowed
         */
        property("textShadowed", TextDisplay.class, PropertyTypes.BOOLEAN,
                TextDisplay::isShadowed, LiveEntityView::setTextShadowed);

        /* @doc tag
         *
         * @Name backgroundColor
         * @RawName <EntityTag.backgroundColor>
         * @Object EntityTag
         * @ReturnType ColorTag
         * @NoArg
         * @Description
         * Returns the background color behind a text_display entity's text.
         *
         * @Implements EntityTag.background_color
         */
        /* @doc mechanism
         *
         * @Name backgroundColor
         * @Object EntityTag
         * @Input ColorTag
         * @Description
         * Sets the background color behind a text_display entity's text. Takes any RGB color, where
         * the alpha channel controls how transparent the plate is, and '!' to fall back to the
         * client's own background setting.
         *
         * @Implements EntityTag.background_color
         */
        clearableProperty("backgroundColor", TextDisplay.class, BUKKIT_COLOR,
                TextDisplay::getBackgroundColor, LiveEntityView::setBackgroundColor);

        /* @doc tag
         *
         * @Name lineWidth
         * @RawName <EntityTag.lineWidth>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the maximum line width, in pixels, before a text_display wraps.
         *
         * @Implements EntityTag.line_width
         */
        /* @doc mechanism
         *
         * @Name lineWidth
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how wide a text_display entity's text may get, in pixels, before it wraps to a new line.
         * The vanilla default is 200.
         *
         * @Implements EntityTag.line_width
         */
        property("lineWidth", TextDisplay.class, PropertyTypes.INTEGER,
                TextDisplay::getLineWidth, LiveEntityView::setLineWidth);

        /* @doc tag
         *
         * @Name opacity
         * @RawName <EntityTag.opacity>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the text opacity of a text_display entity.
         *
         * @Implements EntityTag.opacity
         */
        /* @doc mechanism
         *
         * @Name opacity
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the opacity of a text_display entity's text, from 0 (invisible) to 255 (solid).
         * Use -1 for the vanilla default. Values below 26 are rendered as fully transparent by the client.
         *
         * @Implements EntityTag.opacity
         */
        property("opacity", TextDisplay.class, OPACITY,
                TextDisplay::getTextOpacity, LiveEntityView::setTextOpacity);

        /* @doc tag
         *
         * @Name seeThrough
         * @RawName <EntityTag.seeThrough>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a text_display entity is visible through blocks.
         *
         * @Implements EntityTag.see_through
         */
        /* @doc mechanism
         *
         * @Name seeThrough
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a text_display entity's text renders through walls.
         *
         * @Implements EntityTag.see_through
         */
        property("seeThrough", TextDisplay.class, PropertyTypes.BOOLEAN,
                TextDisplay::isSeeThrough, LiveEntityView::setSeeThrough);

        /* @doc tag
         *
         * @Name defaultBackground
         * @RawName <EntityTag.defaultBackground>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a text_display entity uses the client's default background color.
         *
         * @Implements EntityTag.default_background
         */
        /* @doc mechanism
         *
         * @Name defaultBackground
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a text_display entity uses the player's own text background setting
         * instead of <@link mechanism EntityTag.backgroundColor>.
         *
         * @Implements EntityTag.default_background
         */
        property("defaultBackground", TextDisplay.class, PropertyTypes.BOOLEAN,
                TextDisplay::isDefaultBackground, LiveEntityView::setDefaultBackground);

        /* @doc tag
         *
         * @Name alignment
         * @RawName <EntityTag.alignment>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the text alignment of a text_display entity: CENTER, LEFT, or RIGHT.
         */
        /* @doc mechanism
         *
         * @Name alignment
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets how a multi-line text_display aligns its lines: CENTER (the default), LEFT, or RIGHT.
         */
        property("alignment", TextDisplay.class, PropertyTypes.enumOf(TextDisplay.TextAlignment.class),
                TextDisplay::getAlignment, LiveEntityView::setTextAlignment);
    }

    private static void registerOtherDisplays() {

        /* @doc tag
         *
         * @Name display
         * @RawName <EntityTag.display>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns which model transform an item_display entity renders with.
         *
         * @Implements EntityTag.display
         */
        /* @doc mechanism
         *
         * @Name display
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets which model transform an item_display uses, matching the item model's display settings:
         * NONE renders the raw model;
         * GUI and FIXED render it flat, the way an inventory icon or an item frame looks;
         * GROUND renders it as a dropped item;
         * HEAD renders it as if worn;
         * FIRSTPERSON_LEFTHAND, FIRSTPERSON_RIGHTHAND, THIRDPERSON_LEFTHAND, and THIRDPERSON_RIGHTHAND
         * render it as if held.
         *
         * @Implements EntityTag.display
         */
        property("display", ItemDisplay.class,
                PropertyTypes.enumOf(ItemDisplay.ItemDisplayTransform.class),
                ItemDisplay::getItemDisplayTransform, LiveEntityView::setItemDisplayTransform);

        /* @doc tag
         *
         * @Name width
         * @RawName <EntityTag.width>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the width of an interaction entity's hitbox.
         *
         * @Implements EntityTag.width
         */
        /* @doc mechanism
         *
         * @Name width
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the width of an interaction entity's clickable box, in blocks.
         *
         * @Implements EntityTag.width
         */
        property("width", Interaction.class, PropertyTypes.FLOAT,
                Interaction::getInteractionWidth, LiveEntityView::setInteractionWidth);

        /* @doc tag
         *
         * @Name height
         * @RawName <EntityTag.height>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the height of an interaction entity's hitbox.
         *
         * @Implements EntityTag.height
         */
        /* @doc mechanism
         *
         * @Name height
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the height of an interaction entity's clickable box, in blocks.
         *
         * @Implements EntityTag.height
         */
        property("height", Interaction.class, PropertyTypes.FLOAT,
                Interaction::getInteractionHeight, LiveEntityView::setInteractionHeight);

        /* @doc tag
         *
         * @Name responsive
         * @RawName <EntityTag.responsive>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an interaction entity reacts visually to being hit.
         */
        /* @doc mechanism
         *
         * @Name responsive
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an interaction entity plays the hit sound and animation when attacked.
         */
        property("responsive", Interaction.class, PropertyTypes.BOOLEAN,
                Interaction::isResponsive, LiveEntityView::setInteractionResponsive);
    }

    static Registry<?> unusedRegistryReference() {
        return null;
    }
}

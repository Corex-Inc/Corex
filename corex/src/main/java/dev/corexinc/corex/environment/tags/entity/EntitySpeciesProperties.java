package dev.corexinc.corex.environment.tags.entity;

import dev.corexinc.corex.api.properties.PropertyType;
import dev.corexinc.corex.api.properties.PropertyTypes;
import dev.corexinc.corex.environment.utils.entities.LiveEntityView;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.DyeColor;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.Rotation;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Allay;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Steerable;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;

import java.util.function.Supplier;

import static dev.corexinc.corex.environment.tags.entity.EntityProperties.BLOCK_DATA;
import static dev.corexinc.corex.environment.tags.entity.EntityProperties.DYE_COLOR;
import static dev.corexinc.corex.environment.tags.entity.EntityProperties.ITEM_STACK;
import static dev.corexinc.corex.environment.tags.entity.EntityTag.dispatch;
import static dev.corexinc.corex.environment.tags.entity.EntityTag.livingProperty;

final class EntitySpeciesProperties {

    private EntitySpeciesProperties() {}

    private static <K extends Keyed> PropertyType<K> registry(Supplier<Registry<K>> registry, String description) {
        return EntityTag.registryOf(registry, description);
    }

    static void register() {
        registerVariants();
        registerColors();
        registerContents();
        registerAgeAndTaming();
        registerMobs();
        registerObjects();
    }

    private static void registerVariants() {

        /* @doc tag
         *
         * @Name variant
         * @RawName <EntityTag.variant>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns which variant of its species an entity is, for the mobs that come in several looks.
         * Covers cows, pigs, chickens, wolves, frogs, salmon, axolotls, parrots, mooshrooms, cats,
         * rabbits, and foxes. Returns nothing for anything else.
         *
         * @Usage
         * // Narrates "temperate" for an ordinary cow, or "warm"/"cold" for the biome variants.
         * - narrate <[cow].variant>
         *
         * @Implements EntityTag.variant
         */
        /* @doc mechanism
         *
         * @Name variant
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets which variant of its species an entity is. The accepted values depend on the entity:
         * Cows, Pigs, Chickens and Frogs take TEMPERATE, WARM, or COLD;
         * Wolves take a coat variant such as PALE, ASHEN, or STRIPED;
         * Salmon take SMALL, MEDIUM, or LARGE;
         * Axolotls take LUCY, WILD, GOLD, CYAN, or BLUE;
         * Parrots take RED, BLUE, GREEN, CYAN, or GRAY;
         * Mooshrooms take RED or BROWN;
         * Cats take a breed such as TABBY or SIAMESE;
         * Rabbits take BROWN, WHITE, BLACK, BLACK_AND_WHITE, GOLD, SALT_AND_PEPPER, or THE_KILLER_BUNNY;
         * Foxes take RED or SNOW.
         *
         * Giving a value the entity does not accept reports the valid set for that species.
         *
         * @Usage
         * - adjust <[cow]> variant:warm
         *
         * @Usage
         * - adjust <[wolf]> variant:striped
         *
         * @Implements EntityTag.variant
         */
        dispatch("variant")
                .on(Cow.class, registry(() -> RegistryAccess.registryAccess().getRegistry(RegistryKey.COW_VARIANT),
                        "a cow variant (temperate, warm, cold)"), Cow::getVariant, LiveEntityView::setCowVariant)
                .on(Pig.class, registry(() -> RegistryAccess.registryAccess().getRegistry(RegistryKey.PIG_VARIANT),
                        "a pig variant (temperate, warm, cold)"), Pig::getVariant, LiveEntityView::setPigVariant)
                .on(Chicken.class, registry(() -> RegistryAccess.registryAccess().getRegistry(RegistryKey.CHICKEN_VARIANT),
                        "a chicken variant (temperate, warm, cold)"), Chicken::getVariant, LiveEntityView::setChickenVariant)
                .on(Wolf.class, registry(() -> Registry.WOLF_VARIANT, "a wolf coat variant"),
                        Wolf::getVariant, LiveEntityView::setWolfVariant)
                .on(Frog.class, registry(() -> Registry.FROG_VARIANT, "a frog variant"),
                        Frog::getVariant, LiveEntityView::setFrogVariant)
                .on(Cat.class, registry(() -> Registry.CAT_VARIANT, "a cat breed"),
                        Cat::getCatType, LiveEntityView::setCatVariant)
                .on(Salmon.class, PropertyTypes.enumOf(Salmon.Variant.class), Salmon::getVariant, LiveEntityView::setSalmonVariant)
                .on(Axolotl.class, PropertyTypes.enumOf(Axolotl.Variant.class), Axolotl::getVariant, LiveEntityView::setAxolotlVariant)
                .on(Parrot.class, PropertyTypes.enumOf(Parrot.Variant.class), Parrot::getVariant, LiveEntityView::setParrotVariant)
                .on(MushroomCow.class, PropertyTypes.enumOf(MushroomCow.Variant.class),
                        MushroomCow::getVariant, LiveEntityView::setMooshroomVariant)
                .on(Rabbit.class, PropertyTypes.enumOf(Rabbit.Type.class), Rabbit::getRabbitType, LiveEntityView::setRabbitType)
                .on(Fox.class, PropertyTypes.enumOf(Fox.Type.class), Fox::getFoxType, LiveEntityView::setFoxType)
                .register();
    }

    private static void registerColors() {

        /* @doc tag
         *
         * @Name color
         * @RawName <EntityTag.color>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the color of an entity that has one: a sheep's wool, a shulker's shell, a horse's
         * or llama's coat, or a tropical fish's body. Returns nothing for entities with no color.
         *
         * @Implements EntityTag.color
         */
        /* @doc mechanism
         *
         * @Name color
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the color of an entity that has one. The accepted values depend on the entity:
         * Sheep, Shulkers, and Tropical fish take one of the 16 dye colors;
         * Horses take WHITE, CREAMY, CHESTNUT, BROWN, BLACK, GRAY, or DARK_BROWN;
         * Llamas take CREAMY, WHITE, BROWN, or GRAY.
         *
         * These are named colors, not RGB. Vanilla stores a sheep's or shulker's color as one of
         * 16 values, so an arbitrary hex color cannot be applied to them. For free RGB, use
         * <@link mechanism EntityTag.glowColor> on the entity's outline, or a text_display's
         * <@link mechanism EntityTag.backgroundColor>.
         *
         * Shulkers take '!' to strip the dye and return to the default undyed shell.
         *
         * @Usage
         * - adjust <[sheep]> color:magenta
         *
         * @Usage
         * // Undyes a shulker.
         * - adjust <[shulker]> color:!
         *
         * @Implements EntityTag.color
         */
        dispatch("color")
                .on(Sheep.class, DYE_COLOR, Sheep::getColor, LiveEntityView::setSheepColor)
                .onClearable(Shulker.class, DYE_COLOR, Shulker::getColor, LiveEntityView::setShulkerColor)
                .on(TropicalFish.class, DYE_COLOR, TropicalFish::getBodyColor, LiveEntityView::setFishBodyColor)
                .on(Horse.class, PropertyTypes.enumOf(Horse.Color.class), Horse::getColor, LiveEntityView::setHorseColor)
                .on(Llama.class, PropertyTypes.enumOf(Llama.Color.class), Llama::getColor, LiveEntityView::setLlamaColor)
                .register();

        /* @doc tag
         *
         * @Name collarColor
         * @RawName <EntityTag.collarColor>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the collar color of a tamed wolf or cat.
         */
        /* @doc mechanism
         *
         * @Name collarColor
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the collar color of a wolf or cat, as one of the 16 dye colors.
         * Only visible once the animal is tamed.
         */
        dispatch("collarColor")
                .on(Wolf.class, DYE_COLOR, Wolf::getCollarColor, LiveEntityView::setCollarColor)
                .on(Cat.class, DYE_COLOR, Cat::getCollarColor, LiveEntityView::setCatCollarColor)
                .register();

        /* @doc tag
         *
         * @Name patternColor
         * @RawName <EntityTag.patternColor>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the pattern color of a tropical fish.
         */
        /* @doc mechanism
         *
         * @Name patternColor
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the pattern color of a tropical fish, as one of the 16 dye colors.
         * The body color is set by <@link mechanism EntityTag.color>.
         */
        livingProperty("patternColor", TropicalFish.class, DYE_COLOR,
                TropicalFish::getPatternColor,
                LiveEntityView::setFishPatternColor);

        /* @doc tag
         *
         * @Name pattern
         * @RawName <EntityTag.pattern>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the body pattern of a tropical fish.
         */
        /* @doc mechanism
         *
         * @Name pattern
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the body pattern of a tropical fish, for example KOB, SUNSTREAK, or BETTY.
         */
        livingProperty("pattern", TropicalFish.class, PropertyTypes.enumOf(TropicalFish.Pattern.class),
                TropicalFish::getPattern,
                LiveEntityView::setFishPattern);

        /* @doc tag
         *
         * @Name sheared
         * @RawName <EntityTag.sheared>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a sheep has been sheared.
         *
         * @Implements EntityTag.sheared
         */
        /* @doc mechanism
         *
         * @Name sheared
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a sheep looks sheared. Setting it to 'false' regrows the wool without eating grass.
         *
         * @Implements EntityTag.sheared
         */
        livingProperty("sheared", Sheep.class, PropertyTypes.BOOLEAN,
                Sheep::isSheared,
                LiveEntityView::setSheared);
    }

    private static void registerContents() {

        /* @doc tag
         *
         * @Name material
         * @RawName <EntityTag.material>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the block an entity is made of or carrying: a block_display's block, a falling
         * block's block, or the block an enderman is holding. In vanilla block-state format.
         *
         * @Implements EntityTag.material
         */
        /* @doc mechanism
         *
         * @Name material
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the block an entity renders as or carries, from a material name or a full block state:
         * Block displays render it;
         * Falling blocks render it and place it when they land;
         * Endermen hold it, and take '!' to drop what they are holding.
         *
         * @Usage
         * - adjust <[display]> material:oak_stairs[facing=east;half=top]
         *
         * @Implements EntityTag.material
         */
        dispatch("material")
                .on(BlockDisplay.class, BLOCK_DATA, BlockDisplay::getBlock, LiveEntityView::setBlock)
                .on(FallingBlock.class, BLOCK_DATA, FallingBlock::getBlockData, LiveEntityView::setFallingBlockData)
                .onClearable(Enderman.class, BLOCK_DATA, Enderman::getCarriedBlock, LiveEntityView::setCarriedBlock)
                .register();

        /* @doc tag
         *
         * @Name item
         * @RawName <EntityTag.item>
         * @Object EntityTag
         * @ReturnType ItemTag
         * @NoArg
         * @Description
         * Returns the item an entity holds: an item_display's item, a dropped item's stack, or the
         * item inside an item frame.
         *
         * @Implements EntityTag.item
         */
        /* @doc mechanism
         *
         * @Name item
         * @Object EntityTag
         * @Input ItemTag
         * @Description
         * Sets the item an entity shows:
         * Item displays render it, and take '!' to show nothing;
         * Item frames hold it, and take '!' to empty the frame;
         * Dropped items become that stack, and always need one.
         *
         * @Implements EntityTag.item
         */
        dispatch("item")
                .onClearable(ItemDisplay.class, ITEM_STACK, ItemDisplay::getItemStack, LiveEntityView::setItem)
                .on(Item.class, ITEM_STACK, Item::getItemStack, LiveEntityView::setDroppedItem)
                .onClearable(ItemFrame.class, ITEM_STACK, ItemFrame::getItem, LiveEntityView::setFrameItem)
                .register();
    }

    private static void registerAgeAndTaming() {

        /* @doc tag
         *
         * @Name age
         * @RawName <EntityTag.age>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the age of an ageable entity. Negative values are a baby counting up to 0, which is adult.
         *
         * @Implements EntityTag.age
         */
        /* @doc mechanism
         *
         * @Name age
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the age of an ageable entity. Use -24000 for a newborn baby and 0 for an adult.
         * Positive values are the breeding cooldown counting back down to 0.
         *
         * @Implements EntityTag.age
         */
        livingProperty("age", Ageable.class, PropertyTypes.INTEGER,
                Ageable::getAge,
                LiveEntityView::setAge);

        /* @doc tag
         *
         * @Name isBaby
         * @RawName <EntityTag.isBaby>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an ageable entity is currently a baby.
         *
         * @Implements EntityTag.is_baby
         */
        /* @doc mechanism
         *
         * @Name isBaby
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Turns an ageable entity into a baby ('true') or an adult ('false').
         *
         * @Implements EntityTag.is_baby
         */
        livingProperty("isBaby", Ageable.class, PropertyTypes.BOOLEAN,
                ageable -> !ageable.isAdult(),
                LiveEntityView::setBaby);

        /* @doc tag
         *
         * @Name ageLock
         * @RawName <EntityTag.ageLock>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a breedable entity's age is frozen.
         *
         * @Implements EntityTag.age_lock
         */
        /* @doc mechanism
         *
         * @Name ageLock
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Freezes a breedable entity's age, so a baby never grows up and an adult never loses its breed cooldown.
         *
         * @Implements EntityTag.age_lock
         */
        livingProperty("ageLock", Breedable.class, PropertyTypes.BOOLEAN,
                Breedable::getAgeLock,
                LiveEntityView::setAgeLock);

        /* @doc tag
         *
         * @Name tamed
         * @RawName <EntityTag.tamed>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a tameable entity is tamed.
         *
         * @Implements EntityTag.tame
         */
        /* @doc mechanism
         *
         * @Name tamed
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a tameable entity is tamed. Taming this way leaves the owner unset.
         *
         * @Implements EntityTag.tame
         */
        livingProperty("tamed", Tameable.class, PropertyTypes.BOOLEAN,
                Tameable::isTamed,
                LiveEntityView::setTamed);

        /* @doc tag
         *
         * @Name sitting
         * @RawName <EntityTag.sitting>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a sittable entity (wolf, cat, parrot, camel) is sitting.
         *
         * @Implements EntityTag.sitting
         */
        /* @doc mechanism
         *
         * @Name sitting
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Makes a sittable entity sit or stand.
         *
         * @Implements EntityTag.sitting
         */
        livingProperty("sitting", Sittable.class, PropertyTypes.BOOLEAN,
                Sittable::isSitting,
                LiveEntityView::setSitting);

        /* @doc tag
         *
         * @Name saddled
         * @RawName <EntityTag.saddled>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a steerable entity (pig, strider) has a saddle.
         *
         * @Implements EntityTag.saddle
         */
        /* @doc mechanism
         *
         * @Name saddled
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Puts a saddle on a steerable entity, or takes it off.
         *
         * @Implements EntityTag.saddle
         */
        livingProperty("saddled", Steerable.class, PropertyTypes.BOOLEAN,
                Steerable::hasSaddle,
                LiveEntityView::setSaddled);
    }

    private static void registerMobs() {

        /* @doc tag
         *
         * @Name size
         * @RawName <EntityTag.size>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the size of a slime, magma cube, or phantom.
         *
         * @Implements EntityTag.size
         */
        /* @doc mechanism
         *
         * @Name size
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how big an entity is. The accepted range depends on the entity:
         * Slimes and Magma cubes take 1 to 127, where 1 is the smallest, and scale their health and damage with it;
         * Phantoms take 0 to 64.
         *
         * @Implements EntityTag.size
         */
        dispatch("size")
                .on(Slime.class, PropertyTypes.range(PropertyTypes.INTEGER, 1, 127), Slime::getSize, LiveEntityView::setSlimeSize)
                .on(Phantom.class, PropertyTypes.range(PropertyTypes.INTEGER, 0, 64), Phantom::getSize, LiveEntityView::setPhantomSize)
                .register();

        /* @doc tag
         *
         * @Name puffState
         * @RawName <EntityTag.puffState>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how inflated a pufferfish is, from 0 to 2.
         *
         * @Implements EntityTag.puff_state
         */
        /* @doc mechanism
         *
         * @Name puffState
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how inflated a pufferfish is: 0 deflated, 1 half, 2 fully puffed.
         *
         * @Implements EntityTag.puff_state
         */
        livingProperty("puffState", PufferFish.class, PropertyTypes.range(PropertyTypes.INTEGER, 0, 2),
                PufferFish::getPuffState,
                LiveEntityView::setPuffState);

        /* @doc tag
         *
         * @Name powered
         * @RawName <EntityTag.powered>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a creeper is charged.
         *
         * @Implements EntityTag.powered
         */
        /* @doc mechanism
         *
         * @Name powered
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a creeper is lightning-charged, which doubles its explosion power.
         *
         * @Implements EntityTag.powered
         */
        livingProperty("powered", Creeper.class, PropertyTypes.BOOLEAN,
                Creeper::isPowered,
                LiveEntityView::setPowered);

        /* @doc tag
         *
         * @Name explosionRadius
         * @RawName <EntityTag.explosionRadius>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the explosion radius of a creeper.
         *
         * @Implements EntityTag.explosion_radius
         */
        /* @doc mechanism
         *
         * @Name explosionRadius
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the explosion radius of a creeper. The vanilla default is 3.
         *
         * @Implements EntityTag.explosion_radius
         */
        livingProperty("explosionRadius", Creeper.class, PropertyTypes.INTEGER,
                Creeper::getExplosionRadius,
                LiveEntityView::setExplosionRadius);

        /* @doc tag
         *
         * @Name maxFuseTicks
         * @RawName <EntityTag.maxFuseTicks>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how long a creeper's fuse burns for.
         *
         * @Implements EntityTag.max_fuse_ticks
         */
        /* @doc mechanism
         *
         * @Name maxFuseTicks
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many ticks a creeper hisses before exploding. The vanilla default is 30.
         *
         * @Implements EntityTag.max_fuse_ticks
         */
        livingProperty("maxFuseTicks", Creeper.class, PropertyTypes.TICKS,
                Creeper::getMaxFuseTicks,
                LiveEntityView::setMaxFuseTicks);

        /* @doc tag
         *
         * @Name peek
         * @RawName <EntityTag.peek>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns how far a shulker's lid is open, from 0.0 to 1.0.
         */
        /* @doc mechanism
         *
         * @Name peek
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets how far a shulker peeks out of its shell, from 0.0 (closed) to 1.0 (fully open).
         */
        livingProperty("peek", Shulker.class, PropertyTypes.range(PropertyTypes.FLOAT, 0f, 1f),
                Shulker::getPeek,
                LiveEntityView::setPeek);

        /* @doc tag
         *
         * @Name angry
         * @RawName <EntityTag.angry>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a wolf is angry.
         *
         * @Implements EntityTag.angry
         */
        /* @doc mechanism
         *
         * @Name angry
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a wolf is in its angry state.
         *
         * @Implements EntityTag.angry
         */
        livingProperty("angry", Wolf.class, PropertyTypes.BOOLEAN,
                Wolf::isAngry,
                LiveEntityView::setAngry);

        /* @doc tag
         *
         * @Name style
         * @RawName <EntityTag.style>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the coat marking style of a horse.
         */
        /* @doc mechanism
         *
         * @Name style
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets a horse's markings: NONE, WHITE, WHITEFIELD, WHITE_DOTS, or BLACK_DOTS.
         * The base coat is set by <@link mechanism EntityTag.color>.
         */
        livingProperty("style", Horse.class, PropertyTypes.enumOf(Horse.Style.class),
                Horse::getStyle,
                LiveEntityView::setHorseStyle);

        /* @doc tag
         *
         * @Name jumpStrength
         * @RawName <EntityTag.jumpStrength>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the jump strength of a horse-like entity.
         *
         * @Implements EntityTag.jump_strength
         */
        /* @doc mechanism
         *
         * @Name jumpStrength
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets how high a horse-like entity can jump, from 0 to 2. Around 0.7 is an average vanilla horse.
         *
         * @Implements EntityTag.jump_strength
         */
        livingProperty("jumpStrength", AbstractHorse.class, PropertyTypes.range(PropertyTypes.DOUBLE, 0.0, 2.0),
                AbstractHorse::getJumpStrength,
                LiveEntityView::setJumpStrength);

        /* @doc tag
         *
         * @Name temper
         * @RawName <EntityTag.temper>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how domesticated a horse-like entity is.
         *
         * @Implements EntityTag.temper
         */
        /* @doc mechanism
         *
         * @Name temper
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets a horse's taming progress. Once it reaches <@link tag EntityTag.maxTemper> the horse is tame.
         *
         * @Implements EntityTag.temper
         */
        livingProperty("temper", AbstractHorse.class, PropertyTypes.INTEGER,
                AbstractHorse::getDomestication,
                LiveEntityView::setTemper);

        /* @doc tag
         *
         * @Name maxTemper
         * @RawName <EntityTag.maxTemper>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the domestication level a horse-like entity must reach to become tame.
         *
         * @Implements EntityTag.max_temper
         */
        /* @doc mechanism
         *
         * @Name maxTemper
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how much taming a horse-like entity needs before it is tame. Lower means easier to tame.
         *
         * @Implements EntityTag.max_temper
         */
        livingProperty("maxTemper", AbstractHorse.class, PropertyTypes.INTEGER,
                AbstractHorse::getMaxDomestication,
                LiveEntityView::setMaxTemper);

        /* @doc tag
         *
         * @Name strength
         * @RawName <EntityTag.strength>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the strength of a llama, which sets its inventory size.
         *
         * @Implements EntityTag.strength
         */
        /* @doc mechanism
         *
         * @Name strength
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets a llama's strength, from 1 to 5. Each point adds three slots to its chest inventory.
         *
         * @Implements EntityTag.strength
         */
        livingProperty("strength", Llama.class, PropertyTypes.range(PropertyTypes.INTEGER, 1, 5),
                Llama::getStrength,
                LiveEntityView::setLlamaStrength);

        /* @doc tag
         *
         * @Name mainGene
         * @RawName <EntityTag.mainGene>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the visible gene of a panda.
         */
        /* @doc mechanism
         *
         * @Name mainGene
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets a panda's visible gene: NORMAL, LAZY, WORRIED, PLAYFUL, BROWN, WEAK, or AGGRESSIVE.
         */
        livingProperty("mainGene", Panda.class, PropertyTypes.enumOf(Panda.Gene.class),
                Panda::getMainGene,
                LiveEntityView::setMainGene);

        /* @doc tag
         *
         * @Name hiddenGene
         * @RawName <EntityTag.hiddenGene>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the recessive gene a panda passes to its offspring.
         */
        /* @doc mechanism
         *
         * @Name hiddenGene
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets a panda's recessive gene, which only shows up in its children.
         */
        livingProperty("hiddenGene", Panda.class, PropertyTypes.enumOf(Panda.Gene.class),
                Panda::getHiddenGene,
                LiveEntityView::setHiddenGene);

        /* @doc tag
         *
         * @Name crouching
         * @RawName <EntityTag.crouching>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a fox is in its stalking crouch.
         */
        /* @doc mechanism
         *
         * @Name crouching
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a fox is crouched, as it does before pouncing.
         */
        livingProperty("crouching", Fox.class, PropertyTypes.BOOLEAN,
                Fox::isCrouching,
                LiveEntityView::setCrouching);

        /* @doc tag
         *
         * @Name screaming
         * @RawName <EntityTag.screaming>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a goat is a screaming goat.
         *
         * @Implements EntityTag.screaming
         */
        /* @doc mechanism
         *
         * @Name screaming
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a goat screams instead of bleating.
         *
         * @Implements EntityTag.screaming
         */
        livingProperty("screaming", Goat.class, PropertyTypes.BOOLEAN,
                Goat::isScreaming,
                LiveEntityView::setScreaming);

        /* @doc tag
         *
         * @Name leftHorn
         * @RawName <EntityTag.leftHorn>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a goat still has its left horn.
         */
        /* @doc mechanism
         *
         * @Name leftHorn
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a goat has its left horn.
         */
        livingProperty("leftHorn", Goat.class, PropertyTypes.BOOLEAN,
                Goat::hasLeftHorn,
                LiveEntityView::setLeftHorn);

        /* @doc tag
         *
         * @Name rightHorn
         * @RawName <EntityTag.rightHorn>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a goat still has its right horn.
         */
        /* @doc mechanism
         *
         * @Name rightHorn
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a goat has its right horn.
         */
        livingProperty("rightHorn", Goat.class, PropertyTypes.BOOLEAN,
                Goat::hasRightHorn,
                LiveEntityView::setRightHorn);

        /* @doc tag
         *
         * @Name anger
         * @RawName <EntityTag.anger>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many ticks a bee stays angry for.
         *
         * @Implements EntityTag.anger
         */
        /* @doc mechanism
         *
         * @Name anger
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long a bee remains angry. 0 calms it down.
         *
         * @Implements EntityTag.anger
         */
        livingProperty("anger", Bee.class, PropertyTypes.TICKS,
                Bee::getAnger,
                LiveEntityView::setBeeAnger);

        /* @doc tag
         *
         * @Name hasNectar
         * @RawName <EntityTag.hasNectar>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a bee is carrying nectar.
         *
         * @Implements EntityTag.has_nectar
         */
        /* @doc mechanism
         *
         * @Name hasNectar
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a bee carries nectar, which is what makes it trail particles and pollinate crops.
         *
         * @Implements EntityTag.has_nectar
         */
        livingProperty("hasNectar", Bee.class, PropertyTypes.BOOLEAN,
                Bee::hasNectar,
                LiveEntityView::setHasNectar);

        /* @doc tag
         *
         * @Name hasStung
         * @RawName <EntityTag.hasStung>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a bee has already used its sting.
         *
         * @Implements EntityTag.has_stung
         */
        /* @doc mechanism
         *
         * @Name hasStung
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a bee has stung. A bee that has stung dies shortly after.
         *
         * @Implements EntityTag.has_stung
         */
        livingProperty("hasStung", Bee.class, PropertyTypes.BOOLEAN,
                Bee::hasStung,
                LiveEntityView::setHasStung);

        /* @doc tag
         *
         * @Name playerCreated
         * @RawName <EntityTag.playerCreated>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an iron golem was built by a player rather than spawned by a village.
         *
         * @Implements EntityTag.player_created
         */
        /* @doc mechanism
         *
         * @Name playerCreated
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an iron golem counts as player-built, which stops villagers from being wary of it.
         *
         * @Implements EntityTag.player_created
         */
        livingProperty("playerCreated", IronGolem.class, PropertyTypes.BOOLEAN,
                IronGolem::isPlayerCreated,
                LiveEntityView::setPlayerCreated);

        /* @doc tag
         *
         * @Name derp
         * @RawName <EntityTag.derp>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a snow golem has had its pumpkin sheared off.
         */
        /* @doc mechanism
         *
         * @Name derp
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a snow golem is missing its pumpkin head.
         */
        livingProperty("derp", Snowman.class, PropertyTypes.BOOLEAN,
                Snowman::isDerp,
                LiveEntityView::setDerp);

        /* @doc tag
         *
         * @Name zombificationImmune
         * @RawName <EntityTag.zombificationImmune>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a piglin is immune to turning into a zombified piglin.
         *
         * @Implements EntityTag.immune_to_zombification
         */
        /* @doc mechanism
         *
         * @Name zombificationImmune
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a piglin survives in the overworld without zombifying.
         *
         * @Implements EntityTag.immune_to_zombification
         */
        livingProperty("zombificationImmune", Piglin.class, PropertyTypes.BOOLEAN,
                Piglin::isImmuneToZombification,
                LiveEntityView::setZombificationImmune);

        /* @doc tag
         *
         * @Name trusting
         * @RawName <EntityTag.trusting>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an ocelot trusts players.
         */
        /* @doc mechanism
         *
         * @Name trusting
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an ocelot trusts players and stops fleeing from them.
         */
        livingProperty("trusting", Ocelot.class, PropertyTypes.BOOLEAN,
                Ocelot::isTrusting,
                LiveEntityView::setTrusting);

        /* @doc tag
         *
         * @Name charging
         * @RawName <EntityTag.charging>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a vex is in its charging attack state.
         */
        /* @doc mechanism
         *
         * @Name charging
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a vex is charging, which makes it glow red and dash at its target.
         */
        livingProperty("charging", Vex.class, PropertyTypes.BOOLEAN,
                Vex::isCharging,
                LiveEntityView::setCharging);

        /* @doc tag
         *
         * @Name canDuplicate
         * @RawName <EntityTag.canDuplicate>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an allay is allowed to duplicate itself.
         */
        /* @doc mechanism
         *
         * @Name canDuplicate
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an allay may duplicate when given an amethyst shard while dancing.
         */
        livingProperty("canDuplicate", Allay.class, PropertyTypes.BOOLEAN,
                Allay::canDuplicate,
                LiveEntityView::setCanDuplicate);

        /* @doc tag
         *
         * @Name phase
         * @RawName <EntityTag.phase>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the current behaviour phase of an ender dragon.
         */
        /* @doc mechanism
         *
         * @Name phase
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets an ender dragon's behaviour phase, for example CIRCLING, STRAFING, LAND_ON_PORTAL, or DYING.
         */
        livingProperty("phase", EnderDragon.class, PropertyTypes.enumOf(EnderDragon.Phase.class),
                EnderDragon::getPhase,
                LiveEntityView::setDragonPhase);

        /* @doc tag
         *
         * @Name profession
         * @RawName <EntityTag.profession>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the profession of a villager, for example farmer, librarian, none.
         *
         * @Implements EntityTag.profession
         */
        /* @doc mechanism
         *
         * @Name profession
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets a villager's profession, for example FARMER, LIBRARIAN, CLERIC, or NONE.
         * A villager with a workstation nearby may change it back on its own.
         *
         * @Implements EntityTag.profession
         */
        livingProperty("profession", Villager.class,
                registry(() -> Registry.VILLAGER_PROFESSION, "a villager profession"),
                Villager::getProfession,
                LiveEntityView::setProfession);

        /* @doc tag
         *
         * @Name villagerType
         * @RawName <EntityTag.villagerType>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the biome style of a villager, for example plains, desert, snow.
         */
        /* @doc mechanism
         *
         * @Name villagerType
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets a villager's biome outfit: PLAINS, DESERT, JUNGLE, SAVANNA, SNOW, SWAMP, or TAIGA.
         */
        livingProperty("villagerType", Villager.class,
                registry(() -> Registry.VILLAGER_TYPE, "a villager biome type"),
                Villager::getVillagerType,
                LiveEntityView::setVillagerType);

        /* @doc tag
         *
         * @Name level
         * @RawName <EntityTag.level>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the trading level of a villager, from 1 (novice) to 5 (master).
         */
        /* @doc mechanism
         *
         * @Name level
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets a villager's trading level, from 1 to 5. This is what the badge on its belt shows.
         */
        livingProperty("level", Villager.class, PropertyTypes.range(PropertyTypes.INTEGER, 1, 5),
                Villager::getVillagerLevel,
                LiveEntityView::setVillagerLevel);
    }

    @SuppressWarnings("removal")
    private static void registerObjects() {

        /* @doc tag
         *
         * @Name arms
         * @RawName <EntityTag.arms>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an armor stand shows arms.
         *
         * @Implements EntityTag.arms
         */
        /* @doc mechanism
         *
         * @Name arms
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an armor stand has visible arms.
         *
         * @Implements EntityTag.arms
         */
        livingProperty("arms", ArmorStand.class, PropertyTypes.BOOLEAN,
                ArmorStand::hasArms,
                LiveEntityView::setArms);

        /* @doc tag
         *
         * @Name basePlate
         * @RawName <EntityTag.basePlate>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an armor stand shows its stone base plate.
         *
         * @Implements EntityTag.base_plate
         */
        /* @doc mechanism
         *
         * @Name basePlate
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an armor stand renders its base plate.
         *
         * @Implements EntityTag.base_plate
         */
        livingProperty("basePlate", ArmorStand.class, PropertyTypes.BOOLEAN,
                ArmorStand::hasBasePlate,
                LiveEntityView::setBasePlate);

        /* @doc tag
         *
         * @Name marker
         * @RawName <EntityTag.marker>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an armor stand is a marker.
         *
         * @Implements EntityTag.marker
         */
        /* @doc mechanism
         *
         * @Name marker
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Turns an armor stand into a marker: no hitbox at all, which is what makes it usable as an
         * invisible anchor point.
         *
         * @Implements EntityTag.marker
         */
        livingProperty("marker", ArmorStand.class, PropertyTypes.BOOLEAN,
                ArmorStand::isMarker,
                LiveEntityView::setMarker);

        /* @doc tag
         *
         * @Name small
         * @RawName <EntityTag.small>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an armor stand is the small variant.
         *
         * @Implements EntityTag.small
         */
        /* @doc mechanism
         *
         * @Name small
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an armor stand is half-size.
         *
         * @Implements EntityTag.small
         */
        livingProperty("small", ArmorStand.class, PropertyTypes.BOOLEAN,
                ArmorStand::isSmall,
                LiveEntityView::setSmall);

        /* @doc tag
         *
         * @Name frameRotation
         * @RawName <EntityTag.frameRotation>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the rotation of the item inside an item frame.
         *
         * @Implements EntityTag.framed_item_rotation
         */
        /* @doc mechanism
         *
         * @Name frameRotation
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets how the item in an item frame is turned: NONE, CLOCKWISE_45, CLOCKWISE,
         * CLOCKWISE_135, FLIPPED, FLIPPED_45, COUNTER_CLOCKWISE, or COUNTER_CLOCKWISE_45.
         *
         * @Implements EntityTag.framed_item_rotation
         */
        livingProperty("frameRotation", ItemFrame.class, PropertyTypes.enumOf(Rotation.class),
                ItemFrame::getRotation,
                LiveEntityView::setFrameRotation);

        /* @doc tag
         *
         * @Name fixed
         * @RawName <EntityTag.fixed>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an item frame is fixed in place.
         *
         * @Implements EntityTag.fixed
         */
        /* @doc mechanism
         *
         * @Name fixed
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an item frame is locked: it cannot be broken, and its item cannot be taken or rotated.
         *
         * @Implements EntityTag.fixed
         */
        livingProperty("fixed", ItemFrame.class, PropertyTypes.BOOLEAN,
                ItemFrame::isFixed,
                LiveEntityView::setFrameFixed);

        /* @doc tag
         *
         * @Name fuseTicks
         * @RawName <EntityTag.fuseTicks>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the remaining fuse of primed TNT.
         *
         * @Implements EntityTag.fuse_ticks
         */
        /* @doc mechanism
         *
         * @Name fuseTicks
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long until primed TNT explodes. The vanilla default is 80 ticks.
         *
         * @Implements EntityTag.fuse_ticks
         */
        livingProperty("fuseTicks", TNTPrimed.class, PropertyTypes.TICKS,
                TNTPrimed::getFuseTicks,
                LiveEntityView::setFuseTicks);

        /* @doc tag
         *
         * @Name dropsItem
         * @RawName <EntityTag.dropsItem>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a falling block drops as an item when it cannot be placed.
         *
         * @Implements EntityTag.drops_item
         */
        /* @doc mechanism
         *
         * @Name dropsItem
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a falling block leaves an item behind when it lands somewhere it cannot be placed.
         *
         * @Implements EntityTag.drops_item
         */
        livingProperty("dropsItem", FallingBlock.class, PropertyTypes.BOOLEAN,
                FallingBlock::getDropItem,
                LiveEntityView::setDropsItem);

        /* @doc tag
         *
         * @Name hurtsEntities
         * @RawName <EntityTag.hurtsEntities>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether a falling block damages what it lands on.
         *
         * @Implements EntityTag.fallingblock_hurt_entities
         */
        /* @doc mechanism
         *
         * @Name hurtsEntities
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a falling block hurts entities it lands on, the way an anvil does.
         *
         * @Implements EntityTag.fallingblock_hurt_entities
         */
        livingProperty("hurtsEntities", FallingBlock.class, PropertyTypes.BOOLEAN,
                FallingBlock::canHurtEntities,
                LiveEntityView::setHurtsEntities);

        /* @doc tag
         *
         * @Name damage
         * @RawName <EntityTag.damage>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the base damage of an arrow or trident.
         *
         * @Implements EntityTag.damage
         */
        /* @doc mechanism
         *
         * @Name damage
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the base damage of an arrow-like projectile. The vanilla arrow default is 2.0,
         * multiplied by the projectile's speed on hit.
         *
         * @Implements EntityTag.damage
         */
        livingProperty("damage", AbstractArrow.class, PropertyTypes.DOUBLE,
                AbstractArrow::getDamage,
                LiveEntityView::setArrowDamage);

        /* @doc tag
         *
         * @Name pierceLevel
         * @RawName <EntityTag.pierceLevel>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how many entities an arrow can pass through.
         *
         * @Implements EntityTag.pierce_level
         */
        /* @doc mechanism
         *
         * @Name pierceLevel
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many entities an arrow pierces before stopping, from 0 to 127.
         *
         * @Implements EntityTag.pierce_level
         */
        livingProperty("pierceLevel", AbstractArrow.class, PropertyTypes.range(PropertyTypes.INTEGER, 0, 127),
                AbstractArrow::getPierceLevel,
                LiveEntityView::setPierceLevel);

        /* @doc tag
         *
         * @Name knockback
         * @RawName <EntityTag.knockback>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the knockback strength of an arrow.
         *
         * @Implements EntityTag.knockback
         */
        /* @doc mechanism
         *
         * @Name knockback
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets an arrow's knockback, equivalent to the Punch enchantment level.
         * Vanilla is moving this onto the shooting weapon's attributes, so on newer servers setting
         * the Punch enchantment on <@link tag EntityTag.item> is the more future-proof route.
         *
         * @Implements EntityTag.knockback
         */
        livingProperty("knockback", AbstractArrow.class, PropertyTypes.INTEGER,
                AbstractArrow::getKnockbackStrength,
                LiveEntityView::setKnockback);

        /* @doc tag
         *
         * @Name critical
         * @RawName <EntityTag.critical>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether an arrow is critical.
         *
         * @Implements EntityTag.critical
         */
        /* @doc mechanism
         *
         * @Name critical
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether an arrow is critical, which adds bonus damage and the particle trail.
         *
         * @Implements EntityTag.critical
         */
        livingProperty("critical", AbstractArrow.class, PropertyTypes.BOOLEAN,
                AbstractArrow::isCritical,
                LiveEntityView::setCritical);

        /* @doc tag
         *
         * @Name pickupStatus
         * @RawName <EntityTag.pickupStatus>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns who may pick an arrow back up.
         *
         * @Implements EntityTag.pickup_status
         */
        /* @doc mechanism
         *
         * @Name pickupStatus
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets who may pick an arrow up: DISALLOWED, ALLOWED, or CREATIVE_ONLY.
         *
         * @Implements EntityTag.pickup_status
         */
        livingProperty("pickupStatus", AbstractArrow.class,
                PropertyTypes.enumOf(AbstractArrow.PickupStatus.class),
                AbstractArrow::getPickupStatus,
                LiveEntityView::setPickupStatus);

        /* @doc tag
         *
         * @Name radius
         * @RawName <EntityTag.radius>
         * @Object EntityTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the radius of an area effect cloud.
         *
         * @Implements EntityTag.base_radius
         */
        /* @doc mechanism
         *
         * @Name radius
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the radius of an area effect cloud, in blocks. The lingering potion default is 3.
         *
         * @Implements EntityTag.base_radius
         */
        livingProperty("radius", AreaEffectCloud.class, PropertyTypes.FLOAT,
                AreaEffectCloud::getRadius,
                LiveEntityView::setCloudRadius);

        /* @doc tag
         *
         * @Name duration
         * @RawName <EntityTag.duration>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how long an area effect cloud lasts.
         *
         * @Implements EntityTag.duration
         */
        /* @doc mechanism
         *
         * @Name duration
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long an area effect cloud lingers before vanishing.
         *
         * @Implements EntityTag.duration
         */
        livingProperty("duration", AreaEffectCloud.class, PropertyTypes.TICKS,
                AreaEffectCloud::getDuration,
                LiveEntityView::setCloudDuration);

        /* @doc tag
         *
         * @Name pickupDelay
         * @RawName <EntityTag.pickupDelay>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how long until a dropped item can be picked up.
         *
         * @Implements EntityTag.pickup_delay
         */
        /* @doc mechanism
         *
         * @Name pickupDelay
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how long before a dropped item may be picked up. Values of 32767 or above never become pickupable.
         *
         * @Implements EntityTag.pickup_delay
         */
        livingProperty("pickupDelay", Item.class, PropertyTypes.TICKS,
                Item::getPickupDelay,
                LiveEntityView::setPickupDelay);

        /* @doc tag
         *
         * @Name experience
         * @RawName <EntityTag.experience>
         * @Object EntityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns how much experience an experience orb is worth.
         *
         * @Implements EntityTag.experience
         */
        /* @doc mechanism
         *
         * @Name experience
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how much experience an orb gives when collected.
         *
         * @Implements EntityTag.experience
         */
        livingProperty("experience", ExperienceOrb.class, PropertyTypes.INTEGER,
                ExperienceOrb::getExperience,
                LiveEntityView::setExperience);
    }
}

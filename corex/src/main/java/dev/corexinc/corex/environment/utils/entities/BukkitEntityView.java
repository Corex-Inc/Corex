package dev.corexinc.corex.environment.utils.entities;

import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import dev.corexinc.corex.environment.utils.adapters.EntityAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.DyeColor;
import org.bukkit.Rotation;
import org.bukkit.block.data.BlockData;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record BukkitEntityView(Entity entity) implements LiveEntityView {

    @Override public EntityType bukkitType() { return entity.getType(); }

    @Override public void setCustomName(Component name) { entity.customName(name); }

    @Override public void setCustomNameVisible(boolean visible) { entity.setCustomNameVisible(visible); }

    @Override public void setGlowing(boolean value) { entity.setGlowing(value); }

    @Override public void setGravity(boolean value) { entity.setGravity(value); }

    @Override public void setInvulnerable(boolean value) { entity.setInvulnerable(value); }

    @Override public void setSilent(boolean value) { entity.setSilent(value); }

    @Override public void setPersistent(boolean persistent) { entity.setPersistent(persistent); }

    @Override public void setFireTicks(int ticks) { entity.setFireTicks(ticks); }

    @Override public void setFreezeTicks(int ticks) { entity.setFreezeTicks(ticks); }

    @Override public void setFallDistance(float distance) { entity.setFallDistance(distance); }

    @Override public void setVelocity(Vector velocity) { entity.setVelocity(velocity); }

    @Override public void setRotation(float yaw, float pitch) { entity.setRotation(yaw, pitch); }

    @Override public void teleport(Location location) { entity.teleportAsync(location); }

    @Override public void remove() { entity.remove(); }

    @Override public void setNbt(MapTag nbt) {
        EntityAdapter nms = NMSHandler.get().get(EntityAdapter.class);
        if (nms != null) nms.applyNbt(entity, nbt);
    }

    @Override public void setRemainingAir(int ticks) {
        if (entity instanceof LivingEntity living) living.setRemainingAir(ticks);
    }

    @Override public void setMaxHealth(double health) {
        if (entity instanceof LivingEntity living) {
            AttributeInstance attr = living.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(health);
        }
    }

    @Override public void setHealth(double health) {
        if (entity instanceof LivingEntity living) {
            AttributeInstance attr = living.getAttribute(Attribute.MAX_HEALTH);
            double max = attr != null ? attr.getValue() : health;
            living.setHealth(Math.clamp(health, 0.0, max));
        }
    }

    @Override public void setAI(boolean value) {
        if (entity instanceof LivingEntity living) living.setAI(value);
    }

    @Override public void setInterpolationDuration(int duration) {
        if (entity instanceof Display display) display.setInterpolationDuration(duration);
    }

    @Override public void setInterpolationDelay(int delay) {
        if (entity instanceof Display display) display.setInterpolationDelay(delay);
    }

    @Override public void setTranslation(Vector3f translation) {
        if (entity instanceof Display display) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(translation, t.getLeftRotation(), t.getScale(), t.getRightRotation()));
        }
    }

    @Override public void setScale(Vector3f scale) {
        if (entity instanceof Display display) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(), scale, t.getRightRotation()));
        }
    }

    @Override public void setLeftRotation(Quaternionf rotation) {
        if (entity instanceof Display display) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(t.getTranslation(), rotation, t.getScale(), t.getRightRotation()));
        }
    }

    @Override public void setRightRotation(Quaternionf rotation) {
        if (entity instanceof Display display) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(), t.getScale(), rotation));
        }
    }

    @Override public void setViewRange(float range) {
        if (entity instanceof Display display) display.setViewRange(range);
    }

    @Override public void setBrightness(int block, int sky) {
        if (entity instanceof Display display) display.setBrightness(new Display.Brightness(block, sky));
    }

    @Override public void setBillboard(Display.Billboard billboard) {
        if (entity instanceof Display display) display.setBillboard(billboard);
    }

    @Override public void setTextShadowed(boolean shadowed) {
        if (entity instanceof TextDisplay display) display.setShadowed(shadowed);
    }

    @Override public void setBackgroundColor(Color color) {
        if (entity instanceof TextDisplay display) display.setBackgroundColor(color);
    }

    @Override public void setText(Component text) {
        if (entity instanceof TextDisplay display) display.text(text);
    }

    @Override public void setItem(ItemStack item) {
        if (entity instanceof ItemDisplay display) display.setItemStack(item);
    }

    @Override public void setInteractionWidth(float width) {
        if (entity instanceof Interaction interaction) interaction.setInteractionWidth(width);
    }

    @Override public void setInteractionHeight(float height) {
        if (entity instanceof Interaction interaction) interaction.setInteractionHeight(height);
    }

    @Override public void setInvisible(boolean invisible) {
        if (entity instanceof LivingEntity living) living.setInvisible(invisible);
    }

    @SuppressWarnings("deprecation")
    @Override public void setVisualFire(boolean visualFire) { entity.setVisualFire(visualFire); }

    @Override public void setTicksLived(int ticks) { entity.setTicksLived(Math.max(1, ticks)); }

    @Override public void setPortalCooldown(int ticks) { entity.setPortalCooldown(ticks); }

    @Override public void setVisibleByDefault(boolean visible) { entity.setVisibleByDefault(visible); }

    @Override public void setGliding(boolean gliding) {
        if (entity instanceof LivingEntity living) living.setGliding(gliding);
    }

    @SuppressWarnings("deprecation")
    @Override public void setSwimming(boolean swimming) {
        if (entity instanceof LivingEntity living) living.setSwimming(swimming);
    }

    @Override public void setCollidable(boolean collidable) {
        if (entity instanceof LivingEntity living) living.setCollidable(collidable);
    }

    @Override public void setArrowsInBody(int count) {
        if (entity instanceof LivingEntity living) living.setArrowsInBody(count);
    }

    @Override public void setBeeStingersInBody(int count) {
        if (entity instanceof LivingEntity living) living.setBeeStingersInBody(count);
    }

    @Override public void setAbsorptionAmount(double amount) {
        if (entity instanceof LivingEntity living) living.setAbsorptionAmount(amount);
    }

    @Override public void setNoDamageTicks(int ticks) {
        if (entity instanceof LivingEntity living) living.setNoDamageTicks(ticks);
    }

    @Override public void setMaxNoDamageTicks(int ticks) {
        if (entity instanceof LivingEntity living) living.setMaximumNoDamageTicks(ticks);
    }

    @Override public void setCanPickupItems(boolean value) {
        if (entity instanceof LivingEntity living) living.setCanPickupItems(value);
    }

    @Override public void setRemoveWhenFarAway(boolean value) {
        if (entity instanceof LivingEntity living) living.setRemoveWhenFarAway(value);
    }

    @Override public void setShadowRadius(float radius) {
        if (entity instanceof Display display) display.setShadowRadius(radius);
    }

    @Override public void setShadowStrength(float strength) {
        if (entity instanceof Display display) display.setShadowStrength(strength);
    }

    @Override public void setDisplayWidth(float width) {
        if (entity instanceof Display display) display.setDisplayWidth(width);
    }

    @Override public void setDisplayHeight(float height) {
        if (entity instanceof Display display) display.setDisplayHeight(height);
    }

    @Override public void setGlowColorOverride(Color color) {
        if (entity instanceof Display display) display.setGlowColorOverride(color);
    }

    @Override public void setTeleportDuration(int ticks) {
        if (entity instanceof Display display) display.setTeleportDuration(ticks);
    }

    @Override public void setLineWidth(int width) {
        if (entity instanceof TextDisplay display) display.setLineWidth(width);
    }

    @Override public void setTextOpacity(byte opacity) {
        if (entity instanceof TextDisplay display) display.setTextOpacity(opacity);
    }

    @Override public void setSeeThrough(boolean seeThrough) {
        if (entity instanceof TextDisplay display) display.setSeeThrough(seeThrough);
    }

    @Override public void setDefaultBackground(boolean value) {
        if (entity instanceof TextDisplay display) display.setDefaultBackground(value);
    }

    @Override public void setTextAlignment(TextDisplay.TextAlignment alignment) {
        if (entity instanceof TextDisplay display) display.setAlignment(alignment);
    }

    @Override public void setItemDisplayTransform(ItemDisplay.ItemDisplayTransform transform) {
        if (entity instanceof ItemDisplay display) display.setItemDisplayTransform(transform);
    }

    @Override public void setBlock(BlockData blockData) {
        if (entity instanceof BlockDisplay display) display.setBlock(blockData);
    }

    @Override public void setInteractionResponsive(boolean responsive) {
        if (entity instanceof Interaction interaction) interaction.setResponsive(responsive);
    }

    @Override public void setSheepColor(DyeColor value) {
        if (entity instanceof Sheep sheep) sheep.setColor(value);
    }

    @Override public void setShulkerColor(DyeColor value) {
        if (entity instanceof Shulker shulker) shulker.setColor(value);
    }

    @Override public void setFishBodyColor(DyeColor value) {
        if (entity instanceof TropicalFish fish) fish.setBodyColor(value);
    }

    @Override public void setFishPatternColor(DyeColor value) {
        if (entity instanceof TropicalFish fish) fish.setPatternColor(value);
    }

    @Override public void setFishPattern(TropicalFish.Pattern value) {
        if (entity instanceof TropicalFish fish) fish.setPattern(value);
    }

    @Override public void setHorseColor(Horse.Color value) {
        if (entity instanceof Horse horse) horse.setColor(value);
    }

    @Override public void setHorseStyle(Horse.Style value) {
        if (entity instanceof Horse horse) horse.setStyle(value);
    }

    @Override public void setLlamaColor(Llama.Color value) {
        if (entity instanceof Llama llama) llama.setColor(value);
    }

    @Override public void setLlamaStrength(int value) {
        if (entity instanceof Llama llama) llama.setStrength(value);
    }

    @Override public void setCollarColor(DyeColor value) {
        if (entity instanceof Wolf wolf) wolf.setCollarColor(value);
    }

    @Override public void setCatCollarColor(DyeColor value) {
        if (entity instanceof Cat cat) cat.setCollarColor(value);
    }

    @Override public void setSheared(boolean value) {
        if (entity instanceof Sheep sheep) sheep.setSheared(value);
    }

    @Override public void setCowVariant(Cow.Variant value) {
        if (entity instanceof Cow cow) cow.setVariant(value);
    }

    @Override public void setPigVariant(Pig.Variant value) {
        if (entity instanceof Pig pig) pig.setVariant(value);
    }

    @Override public void setChickenVariant(Chicken.Variant value) {
        if (entity instanceof Chicken chicken) chicken.setVariant(value);
    }

    @Override public void setWolfVariant(Wolf.Variant value) {
        if (entity instanceof Wolf wolf) wolf.setVariant(value);
    }

    @Override public void setFrogVariant(Frog.Variant value) {
        if (entity instanceof Frog frog) frog.setVariant(value);
    }

    @Override public void setCatVariant(Cat.Type value) {
        if (entity instanceof Cat cat) cat.setCatType(value);
    }

    @Override public void setSalmonVariant(Salmon.Variant value) {
        if (entity instanceof Salmon salmon) salmon.setVariant(value);
    }

    @Override public void setAxolotlVariant(Axolotl.Variant value) {
        if (entity instanceof Axolotl axolotl) axolotl.setVariant(value);
    }

    @Override public void setParrotVariant(Parrot.Variant value) {
        if (entity instanceof Parrot parrot) parrot.setVariant(value);
    }

    @Override public void setMooshroomVariant(MushroomCow.Variant value) {
        if (entity instanceof MushroomCow mooshroom) mooshroom.setVariant(value);
    }

    @Override public void setRabbitType(Rabbit.Type value) {
        if (entity instanceof Rabbit rabbit) rabbit.setRabbitType(value);
    }

    @Override public void setFoxType(Fox.Type value) {
        if (entity instanceof Fox fox) fox.setFoxType(value);
    }

    @Override public void setSlimeSize(int value) {
        if (entity instanceof Slime slime) slime.setSize(value);
    }

    @Override public void setPhantomSize(int value) {
        if (entity instanceof Phantom phantom) phantom.setSize(value);
    }

    @Override public void setPuffState(int value) {
        if (entity instanceof PufferFish fish) fish.setPuffState(value);
    }

    @Override public void setPowered(boolean value) {
        if (entity instanceof Creeper creeper) creeper.setPowered(value);
    }

    @Override public void setExplosionRadius(int value) {
        if (entity instanceof Creeper creeper) creeper.setExplosionRadius(value);
    }

    @Override public void setMaxFuseTicks(int value) {
        if (entity instanceof Creeper creeper) creeper.setMaxFuseTicks(value);
    }

    @Override public void setPeek(float value) {
        if (entity instanceof Shulker shulker) shulker.setPeek(value);
    }

    @Override public void setAngry(boolean value) {
        if (entity instanceof Wolf wolf) wolf.setAngry(value);
    }

    @Override public void setJumpStrength(double value) {
        if (entity instanceof AbstractHorse horse) horse.setJumpStrength(value);
    }

    @Override public void setTemper(int value) {
        if (entity instanceof AbstractHorse horse) horse.setDomestication(value);
    }

    @Override public void setMaxTemper(int value) {
        if (entity instanceof AbstractHorse horse) horse.setMaxDomestication(value);
    }

    @Override public void setMainGene(Panda.Gene value) {
        if (entity instanceof Panda panda) panda.setMainGene(value);
    }

    @Override public void setHiddenGene(Panda.Gene value) {
        if (entity instanceof Panda panda) panda.setHiddenGene(value);
    }

    @Override public void setCrouching(boolean value) {
        if (entity instanceof Fox fox) fox.setCrouching(value);
    }

    @Override public void setScreaming(boolean value) {
        if (entity instanceof Goat goat) goat.setScreaming(value);
    }

    @Override public void setLeftHorn(boolean value) {
        if (entity instanceof Goat goat) goat.setLeftHorn(value);
    }

    @Override public void setRightHorn(boolean value) {
        if (entity instanceof Goat goat) goat.setRightHorn(value);
    }

    @Override public void setBeeAnger(int value) {
        if (entity instanceof Bee bee) bee.setAnger(value);
    }

    @Override public void setHasNectar(boolean value) {
        if (entity instanceof Bee bee) bee.setHasNectar(value);
    }

    @Override public void setHasStung(boolean value) {
        if (entity instanceof Bee bee) bee.setHasStung(value);
    }

    @Override public void setCarriedBlock(BlockData value) {
        if (entity instanceof Enderman enderman) enderman.setCarriedBlock(value);
    }

    @Override public void setPlayerCreated(boolean value) {
        if (entity instanceof IronGolem golem) golem.setPlayerCreated(value);
    }

    @Override public void setDerp(boolean value) {
        if (entity instanceof Snowman snowman) snowman.setDerp(value);
    }

    @Override public void setZombificationImmune(boolean value) {
        if (entity instanceof Piglin piglin) piglin.setImmuneToZombification(value);
    }

    @Override public void setTrusting(boolean value) {
        if (entity instanceof Ocelot ocelot) ocelot.setTrusting(value);
    }

    @Override public void setCharging(boolean value) {
        if (entity instanceof Vex vex) vex.setCharging(value);
    }

    @Override public void setCanDuplicate(boolean value) {
        if (entity instanceof Allay allay) allay.setCanDuplicate(value);
    }

    @Override public void setDragonPhase(EnderDragon.Phase value) {
        if (entity instanceof EnderDragon dragon) dragon.setPhase(value);
    }

    @Override public void setProfession(Villager.Profession value) {
        if (entity instanceof Villager villager) villager.setProfession(value);
    }

    @Override public void setVillagerType(Villager.Type value) {
        if (entity instanceof Villager villager) villager.setVillagerType(value);
    }

    @Override public void setVillagerLevel(int value) {
        if (entity instanceof Villager villager) villager.setVillagerLevel(value);
    }

    @Override public void setAge(int value) {
        if (entity instanceof Ageable ageable) ageable.setAge(value);
    }

    @Override public void setBaby(boolean value) {
        if (entity instanceof Ageable ageable) applyBaby(ageable, value);
    }

    @Override public void setAgeLock(boolean value) {
        if (entity instanceof Breedable breedable) breedable.setAgeLock(value);
    }

    @Override public void setTamed(boolean value) {
        if (entity instanceof Tameable tameable) tameable.setTamed(value);
    }

    @Override public void setSitting(boolean value) {
        if (entity instanceof Sittable sittable) sittable.setSitting(value);
    }

    @Override public void setSaddled(boolean value) {
        if (entity instanceof Steerable steerable) steerable.setSaddle(value);
    }

    @Override public void setArms(boolean value) {
        if (entity instanceof ArmorStand armorStand) armorStand.setArms(value);
    }

    @Override public void setBasePlate(boolean value) {
        if (entity instanceof ArmorStand armorStand) armorStand.setBasePlate(value);
    }

    @Override public void setMarker(boolean value) {
        if (entity instanceof ArmorStand armorStand) armorStand.setMarker(value);
    }

    @Override public void setSmall(boolean value) {
        if (entity instanceof ArmorStand armorStand) armorStand.setSmall(value);
    }

    @Override public void setFrameItem(ItemStack value) {
        if (entity instanceof ItemFrame frame) frame.setItem(value);
    }

    @Override public void setFrameRotation(Rotation value) {
        if (entity instanceof ItemFrame frame) frame.setRotation(value);
    }

    @Override public void setFrameFixed(boolean value) {
        if (entity instanceof ItemFrame frame) frame.setFixed(value);
    }

    @Override public void setFuseTicks(int value) {
        if (entity instanceof TNTPrimed tnt) tnt.setFuseTicks(value);
    }

    @Override public void setFallingBlockData(BlockData value) {
        if (entity instanceof FallingBlock fallingBlock) fallingBlock.setBlockData(value);
    }

    @Override public void setDropsItem(boolean value) {
        if (entity instanceof FallingBlock fallingBlock) fallingBlock.setDropItem(value);
    }

    @Override public void setHurtsEntities(boolean value) {
        if (entity instanceof FallingBlock fallingBlock) fallingBlock.setHurtEntities(value);
    }

    @Override public void setArrowDamage(double value) {
        if (entity instanceof AbstractArrow arrow) arrow.setDamage(value);
    }

    @Override public void setPierceLevel(int value) {
        if (entity instanceof AbstractArrow arrow) arrow.setPierceLevel(value);
    }

    @Override public void setKnockback(int value) {
        if (entity instanceof AbstractArrow arrow) arrow.setKnockbackStrength(value);
    }

    @Override public void setCritical(boolean value) {
        if (entity instanceof AbstractArrow arrow) arrow.setCritical(value);
    }

    @Override public void setPickupStatus(AbstractArrow.PickupStatus value) {
        if (entity instanceof AbstractArrow arrow) arrow.setPickupStatus(value);
    }

    @Override public void setCloudRadius(float value) {
        if (entity instanceof AreaEffectCloud cloud) cloud.setRadius(value);
    }

    @Override public void setCloudDuration(int value) {
        if (entity instanceof AreaEffectCloud cloud) cloud.setDuration(value);
    }

    @Override public void setDroppedItem(ItemStack value) {
        if (entity instanceof Item item) item.setItemStack(value);
    }

    @Override public void setPickupDelay(int value) {
        if (entity instanceof Item item) item.setPickupDelay(value);
    }

    @Override public void setExperience(int value) {
        if (entity instanceof ExperienceOrb orb) orb.setExperience(value);
    }

    private static void applyBaby(Ageable ageable, boolean baby) {
        if (baby) ageable.setBaby();
        else ageable.setAdult();
    }
}

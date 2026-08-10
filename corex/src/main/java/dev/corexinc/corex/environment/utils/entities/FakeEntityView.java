package dev.corexinc.corex.environment.utils.entities;

import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import dev.corexinc.corex.environment.tags.core.MapTag;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import me.tofaa.entitylib.meta.mobs.BeeMeta;
import me.tofaa.entitylib.meta.mobs.monster.EndermanMeta;
import me.tofaa.entitylib.meta.mobs.FoxMeta;
import me.tofaa.entitylib.meta.mobs.GoatMeta;
import me.tofaa.entitylib.meta.mobs.OcelotMeta;
import me.tofaa.entitylib.meta.mobs.PandaMeta;
import me.tofaa.entitylib.meta.mobs.cuboid.SlimeMeta;
import me.tofaa.entitylib.meta.mobs.golem.ShulkerMeta;
import me.tofaa.entitylib.meta.mobs.horse.LlamaMeta;
import me.tofaa.entitylib.meta.mobs.monster.CreeperMeta;
import me.tofaa.entitylib.meta.mobs.monster.PhantomMeta;
import me.tofaa.entitylib.meta.mobs.passive.MooshroomMeta;
import me.tofaa.entitylib.meta.mobs.passive.RabbitMeta;
import me.tofaa.entitylib.meta.mobs.passive.SheepMeta;
import me.tofaa.entitylib.meta.mobs.tameable.CatMeta;
import me.tofaa.entitylib.meta.mobs.tameable.WolfMeta;
import me.tofaa.entitylib.meta.mobs.water.AxolotlMeta;
import me.tofaa.entitylib.meta.other.ArmorStandMeta;
import me.tofaa.entitylib.meta.other.FallingBlockMeta;
import me.tofaa.entitylib.meta.other.ItemFrameMeta;
import me.tofaa.entitylib.meta.projectile.BaseArrowMeta;
import me.tofaa.entitylib.meta.types.AgeableMeta;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.BlockDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.meta.other.InteractionMeta;
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Rotation;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record FakeEntityView(WrapperEntity wrapperEntity) implements LiveEntityView {

    @Override public org.bukkit.entity.EntityType bukkitType() {
        return SpigotConversionUtil.toBukkitEntityType(wrapperEntity.getEntityType());
    }

    @Override public void setCustomName(Component name) { wrapperEntity.getEntityMeta().setCustomName(name); }

    @Override public void setCustomNameVisible(boolean visible) { wrapperEntity.getEntityMeta().setCustomNameVisible(visible); }

    @Override public void setGlowing(boolean value) { wrapperEntity.getEntityMeta().setGlowing(value); }

    @Override public void setGravity(boolean value) { wrapperEntity.setHasNoGravity(!value); }

    @Override public void setSilent(boolean value) { wrapperEntity.getEntityMeta().setSilent(value); }

    @Override public void setRotation(float yaw, float pitch) { wrapperEntity.rotateHead(yaw, pitch); }

    @Override public void setFireTicks(int ticks) { wrapperEntity.getEntityMeta().setOnFire(ticks > 0); }

    @Override public void setFreezeTicks(int ticks) { wrapperEntity.getEntityMeta().setTicksFrozenInPowderedSnow(ticks); }

    @Override public void setVelocity(Vector velocity) {
        wrapperEntity.setVelocity(new Vector3d(velocity.getX(), velocity.getY(), velocity.getZ()));
    }

    @Override public void setInvulnerable(boolean value) {}

    @Override public void setPersistent(boolean persistent) {}

    @Override public void setNbt(MapTag nbt) {}

    @Override public void setRemainingAir(int ticks) {}

    @Override public void setFallDistance(float distance) {}

    @Override public void setMaxHealth(double health) {}

    @Override public void setHealth(double health) {}

    @Override public void setAI(boolean value) {}

    @Override public void setInterpolationDuration(int duration) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setTransformationInterpolationDuration(duration);
    }

    @Override public void setInterpolationDelay(int delay) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setInterpolationDelay(delay);
    }

    @Override public void setTranslation(Vector3f translation) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(translation.x, translation.y, translation.z));
        }
    }

    @Override public void setScale(Vector3f scale) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setScale(new com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z));
        }
    }

    @Override public void setLeftRotation(Quaternionf rotation) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setLeftRotation(new Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w));
        }
    }

    @Override public void setRightRotation(Quaternionf rotation) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setRightRotation(new Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w));
        }
    }

    @Override public void setViewRange(float range) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setViewRange(range);
    }

    @Override public void setBrightness(int block, int sky) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setBrightnessOverride((block << 4) | (sky << 20));
        }
    }

    @Override public void setBillboard(Display.Billboard billboard) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.valueOf(billboard.name()));
        }
    }

    @Override public void setTextShadowed(boolean shadowed) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setShadow(shadowed);
    }

    @Override public void setBackgroundColor(Color color) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setBackgroundColor(color.asARGB());
    }

    @Override public void setText(Component text) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setText(text);
    }

    @Override public void setItem(ItemStack item) {
        if (wrapperEntity.getEntityMeta() instanceof ItemDisplayMeta itemMeta) {
            itemMeta.setItem(SpigotConversionUtil.fromBukkitItemStack(item));
        }
    }

    @Override public void setInteractionWidth(float width) {
        if (wrapperEntity.getEntityMeta() instanceof InteractionMeta interactionMeta) interactionMeta.setWidth(width);
    }

    @Override public void setInteractionHeight(float height) {
        if (wrapperEntity.getEntityMeta() instanceof InteractionMeta interactionMeta) interactionMeta.setHeight(height);
    }

    @Override public void setInvisible(boolean invisible) { wrapperEntity.getEntityMeta().setInvisible(invisible); }

    @Override public void setVisualFire(boolean visualFire) { wrapperEntity.getEntityMeta().setOnFire(visualFire); }

    @Override public void setTicksLived(int ticks) {}

    @Override public void setPortalCooldown(int ticks) {}

    @Override public void setVisibleByDefault(boolean visible) {}

    @Override public void setGliding(boolean gliding) { wrapperEntity.getEntityMeta().setFlyingWithElytra(gliding); }

    @Override public void setSwimming(boolean swimming) { wrapperEntity.getEntityMeta().setSwimming(swimming); }

    @Override public void setCollidable(boolean collidable) {}

    @Override public void setArrowsInBody(int count) {
        if (wrapperEntity.getEntityMeta() instanceof LivingEntityMeta livingMeta) livingMeta.setArrowCount(count);
    }

    @Override public void setBeeStingersInBody(int count) {
        if (wrapperEntity.getEntityMeta() instanceof LivingEntityMeta livingMeta) livingMeta.setBeeStingerCount(count);
    }

    @Override public void setAbsorptionAmount(double amount) {}

    @Override public void setNoDamageTicks(int ticks) {}

    @Override public void setMaxNoDamageTicks(int ticks) {}

    @Override public void setCanPickupItems(boolean value) {}

    @Override public void setRemoveWhenFarAway(boolean value) {}

    @Override public void setShadowRadius(float radius) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setShadowRadius(radius);
    }

    @Override public void setShadowStrength(float strength) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setShadowStrength(strength);
    }

    @Override public void setDisplayWidth(float width) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setWidth(width);
    }

    @Override public void setDisplayHeight(float height) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) displayMeta.setHeight(height);
    }

    @Override public void setGlowColorOverride(Color color) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setGlowColorOverride(color.asARGB());
        }
    }

    @Override public void setTeleportDuration(int ticks) {
        if (wrapperEntity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
            displayMeta.setPositionRotationInterpolationDuration(ticks);
        }
    }

    @Override public void setLineWidth(int width) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setLineWidth(width);
    }

    @Override public void setTextOpacity(byte opacity) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setTextOpacity(opacity);
    }

    @Override public void setSeeThrough(boolean seeThrough) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setSeeThrough(seeThrough);
    }

    @Override public void setDefaultBackground(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) textMeta.setUseDefaultBackground(value);
    }

    @Override public void setTextAlignment(TextDisplay.TextAlignment alignment) {
        if (wrapperEntity.getEntityMeta() instanceof TextDisplayMeta textMeta) {
            textMeta.setAlignLeft(alignment == TextDisplay.TextAlignment.LEFT);
            textMeta.setAlignRight(alignment == TextDisplay.TextAlignment.RIGHT);
        }
    }

    @Override public void setItemDisplayTransform(ItemDisplay.ItemDisplayTransform transform) {
        if (wrapperEntity.getEntityMeta() instanceof ItemDisplayMeta itemMeta) {
            ItemDisplayMeta.DisplayType[] types = ItemDisplayMeta.DisplayType.values();
            int ordinal = transform.ordinal();
            if (ordinal < types.length) itemMeta.setDisplayType(types[ordinal]);
        }
    }

    @Override public void setBlock(BlockData blockData) {
        if (wrapperEntity.getEntityMeta() instanceof BlockDisplayMeta blockMeta) {
            blockMeta.setBlockState(SpigotConversionUtil.fromBukkitBlockData(blockData));
        }
    }

    @Override public void setInteractionResponsive(boolean responsive) {
        if (wrapperEntity.getEntityMeta() instanceof InteractionMeta interactionMeta) interactionMeta.setResponsive(responsive);
    }

    @Override public void setSheepColor(DyeColor value) {
        if (wrapperEntity.getEntityMeta() instanceof SheepMeta meta) meta.setColor((byte) value.ordinal());
        else unsupported("setSheepColor");
    }

    @Override public void setShulkerColor(DyeColor value) {
        if (wrapperEntity.getEntityMeta() instanceof ShulkerMeta meta) meta.setColor(value == null ? (byte) 16 : (byte) value.ordinal());
        else unsupported("setShulkerColor");
    }

    @Override public void setFishBodyColor(DyeColor value) { unsupported("setFishBodyColor"); }

    @Override public void setFishPatternColor(DyeColor value) { unsupported("setFishPatternColor"); }

    @Override public void setFishPattern(TropicalFish.Pattern value) { unsupported("setFishPattern"); }

    @Override public void setHorseColor(Horse.Color value) { unsupported("setHorseColor"); }

    @Override public void setHorseStyle(Horse.Style value) { unsupported("setHorseStyle"); }

    @Override public void setLlamaColor(Llama.Color value) {
        if (wrapperEntity.getEntityMeta() instanceof LlamaMeta meta) applyNamed(LlamaMeta.Variant.class, value, meta::setVariant);
        else unsupported("setLlamaColor");
    }

    @Override public void setLlamaStrength(int value) {
        if (wrapperEntity.getEntityMeta() instanceof LlamaMeta meta) meta.setStrength(value);
        else unsupported("setLlamaStrength");
    }

    @Override public void setCollarColor(DyeColor value) {
        if (wrapperEntity.getEntityMeta() instanceof WolfMeta meta) meta.setCollarColor(value.ordinal());
        else unsupported("setCollarColor");
    }

    @Override public void setCatCollarColor(DyeColor value) {
        if (wrapperEntity.getEntityMeta() instanceof CatMeta meta) applyNamed(me.tofaa.entitylib.extras.DyeColor.class, value, meta::setCollarColor);
        else unsupported("setCatCollarColor");
    }

    @Override public void setSheared(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof SheepMeta meta) meta.setSheared(value);
        else unsupported("setSheared");
    }

    @Override public void setCowVariant(Cow.Variant value) { unsupported("setCowVariant"); }

    @Override public void setPigVariant(Pig.Variant value) { unsupported("setPigVariant"); }

    @Override public void setChickenVariant(Chicken.Variant value) { unsupported("setChickenVariant"); }

    @Override public void setWolfVariant(Wolf.Variant value) { unsupported("setWolfVariant"); }

    @Override public void setFrogVariant(Frog.Variant value) { unsupported("setFrogVariant"); }

    @Override public void setCatVariant(Cat.Type value) {
        if (wrapperEntity.getEntityMeta() instanceof CatMeta meta) applyKeyed(CatMeta.Variant.class, value, meta::setVariant);
        else unsupported("setCatVariant");
    }

    @Override public void setSalmonVariant(Salmon.Variant value) { unsupported("setSalmonVariant"); }

    @Override public void setAxolotlVariant(Axolotl.Variant value) {
        if (wrapperEntity.getEntityMeta() instanceof AxolotlMeta meta) applyNamed(AxolotlMeta.Variant.class, value, meta::setVariant);
        else unsupported("setAxolotlVariant");
    }

    @Override public void setParrotVariant(Parrot.Variant value) { unsupported("setParrotVariant"); }

    @Override public void setMooshroomVariant(MushroomCow.Variant value) {
        if (wrapperEntity.getEntityMeta() instanceof MooshroomMeta meta) applyNamed(MooshroomMeta.Variant.class, value, meta::setVariant);
        else unsupported("setMooshroomVariant");
    }

    @Override public void setRabbitType(Rabbit.Type value) {
        if (wrapperEntity.getEntityMeta() instanceof RabbitMeta meta) applyNamed(RabbitMeta.Type.class, value, meta::setType);
        else unsupported("setRabbitType");
    }

    @Override public void setFoxType(Fox.Type value) {
        if (wrapperEntity.getEntityMeta() instanceof FoxMeta meta) applyNamed(FoxMeta.Type.class, value, meta::setType);
        else unsupported("setFoxType");
    }

    @Override public void setSlimeSize(int value) {
        if (wrapperEntity.getEntityMeta() instanceof SlimeMeta meta) meta.setSize(value);
        else unsupported("setSlimeSize");
    }

    @Override public void setPhantomSize(int value) {
        if (wrapperEntity.getEntityMeta() instanceof PhantomMeta meta) meta.setSize(value);
        else unsupported("setPhantomSize");
    }

    @Override public void setPuffState(int value) { unsupported("setPuffState"); }

    @Override public void setPowered(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof CreeperMeta meta) meta.setCharged(value);
        else unsupported("setPowered");
    }

    @Override public void setExplosionRadius(int value) { unsupported("setExplosionRadius"); }

    @Override public void setMaxFuseTicks(int value) { unsupported("setMaxFuseTicks"); }

    @Override public void setPeek(float value) {
        if (wrapperEntity.getEntityMeta() instanceof ShulkerMeta meta) meta.setShieldHeight((byte) (value * 100));
        else unsupported("setPeek");
    }

    @Override public void setAngry(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof WolfMeta meta) meta.setAngerTime(value ? 200 : 0);
        else unsupported("setAngry");
    }

    @Override public void setJumpStrength(double value) { unsupported("setJumpStrength"); }

    @Override public void setTemper(int value) { unsupported("setTemper"); }

    @Override public void setMaxTemper(int value) { unsupported("setMaxTemper"); }

    @Override public void setMainGene(Panda.Gene value) {
        if (wrapperEntity.getEntityMeta() instanceof PandaMeta meta) applyNamed(PandaMeta.Gene.class, value, meta::setMainGene);
        else unsupported("setMainGene");
    }

    @Override public void setHiddenGene(Panda.Gene value) {
        if (wrapperEntity.getEntityMeta() instanceof PandaMeta meta) applyNamed(PandaMeta.Gene.class, value, meta::setHiddenGene);
        else unsupported("setHiddenGene");
    }

    @Override public void setCrouching(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof FoxMeta meta) meta.setFoxSneaking(value);
        else unsupported("setCrouching");
    }

    @Override public void setScreaming(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof GoatMeta meta) meta.setScreaming(value);
        else unsupported("setScreaming");
    }

    @Override public void setLeftHorn(boolean value) { unsupported("setLeftHorn"); }

    @Override public void setRightHorn(boolean value) { unsupported("setRightHorn"); }

    @Override public void setBeeAnger(int value) {
        if (wrapperEntity.getEntityMeta() instanceof BeeMeta meta) meta.setAngerTicks(value);
        else unsupported("setBeeAnger");
    }

    @Override public void setHasNectar(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof BeeMeta meta) meta.setHasNectar(value);
        else unsupported("setHasNectar");
    }

    @Override public void setHasStung(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof BeeMeta meta) meta.setHasStung(value);
        else unsupported("setHasStung");
    }

    @Override public void setCarriedBlock(BlockData value) {
        if (wrapperEntity.getEntityMeta() instanceof EndermanMeta meta) meta.setCarriedBlockState(SpigotConversionUtil.fromBukkitBlockData(value));
        else unsupported("setCarriedBlock");
    }

    @Override public void setPlayerCreated(boolean value) { unsupported("setPlayerCreated"); }

    @Override public void setDerp(boolean value) { unsupported("setDerp"); }

    @Override public void setZombificationImmune(boolean value) { unsupported("setZombificationImmune"); }

    @Override public void setTrusting(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof OcelotMeta meta) meta.setTrusting(value);
        else unsupported("setTrusting");
    }

    @Override public void setCharging(boolean value) { unsupported("setCharging"); }

    @Override public void setCanDuplicate(boolean value) { unsupported("setCanDuplicate"); }

    @Override public void setDragonPhase(EnderDragon.Phase value) { unsupported("setDragonPhase"); }

    @Override public void setProfession(Villager.Profession value) { unsupported("setProfession"); }

    @Override public void setVillagerType(Villager.Type value) { unsupported("setVillagerType"); }

    @Override public void setVillagerLevel(int value) { unsupported("setVillagerLevel"); }

    @Override public void setAge(int value) { unsupported("setAge"); }

    @Override public void setBaby(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof AgeableMeta meta) meta.setBaby(value);
        else unsupported("setBaby");
    }

    @Override public void setAgeLock(boolean value) { unsupported("setAgeLock"); }

    @Override public void setTamed(boolean value) { unsupported("setTamed"); }

    @Override public void setSitting(boolean value) { unsupported("setSitting"); }

    @Override public void setSaddled(boolean value) { unsupported("setSaddled"); }

    @Override public void setArms(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof ArmorStandMeta meta) meta.setHasArms(value);
        else unsupported("setArms");
    }

    @Override public void setBasePlate(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof ArmorStandMeta meta) meta.setHasNoBasePlate(!value);
        else unsupported("setBasePlate");
    }

    @Override public void setMarker(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof ArmorStandMeta meta) meta.setMarker(value);
        else unsupported("setMarker");
    }

    @Override public void setSmall(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof ArmorStandMeta meta) meta.setSmall(value);
        else unsupported("setSmall");
    }

    @Override public void setFrameItem(ItemStack value) {
        if (wrapperEntity.getEntityMeta() instanceof ItemFrameMeta meta) meta.setItem(SpigotConversionUtil.fromBukkitItemStack(value));
        else unsupported("setFrameItem");
    }

    @Override public void setFrameRotation(Rotation value) {
        if (wrapperEntity.getEntityMeta() instanceof ItemFrameMeta meta) applyNamed(me.tofaa.entitylib.extras.Rotation.class, value, meta::setRotation);
        else unsupported("setFrameRotation");
    }

    @Override public void setFrameFixed(boolean value) { unsupported("setFrameFixed"); }

    @Override public void setFuseTicks(int value) { unsupported("setFuseTicks"); }

    @Override public void setFallingBlockData(BlockData value) {
        if (wrapperEntity.getEntityMeta() instanceof FallingBlockMeta meta) meta.setBlockState(SpigotConversionUtil.fromBukkitBlockData(value));
        else unsupported("setFallingBlockData");
    }

    @Override public void setDropsItem(boolean value) { unsupported("setDropsItem"); }

    @Override public void setHurtsEntities(boolean value) { unsupported("setHurtsEntities"); }

    @Override public void setArrowDamage(double value) { unsupported("setArrowDamage"); }

    @Override public void setPierceLevel(int value) {
        if (wrapperEntity.getEntityMeta() instanceof BaseArrowMeta meta) meta.setPierceLevel(value);
        else unsupported("setPierceLevel");
    }

    @Override public void setKnockback(int value) { unsupported("setKnockback"); }

    @Override public void setCritical(boolean value) {
        if (wrapperEntity.getEntityMeta() instanceof BaseArrowMeta meta) meta.setCritical(value);
        else unsupported("setCritical");
    }

    @Override public void setPickupStatus(AbstractArrow.PickupStatus value) { unsupported("setPickupStatus"); }

    @Override public void setCloudRadius(float value) { unsupported("setCloudRadius"); }

    @Override public void setCloudDuration(int value) { unsupported("setCloudDuration"); }

    @Override public void setDroppedItem(ItemStack value) { unsupported("setDroppedItem"); }

    @Override public void setPickupDelay(int value) { unsupported("setPickupDelay"); }

    @Override public void setExperience(int value) { unsupported("setExperience"); }

    private void unsupported(String setter) {
        Debugger.echoError(null, "A fake entity has no packet equivalent for '" + setter
                + "' - spawn a real entity to use it.");
    }

    private static <T extends Enum<T>> void applyNamed(Class<T> target, Enum<?> source, java.util.function.Consumer<T> writer) {
        try {
            writer.accept(Enum.valueOf(target, source.name()));
        } catch (IllegalArgumentException mismatch) {
            Debugger.echoError(null, "EntityLib has no " + target.getSimpleName() + " named " + source.name() + ".");
        }
    }

    private static <T extends Enum<T>> void applyKeyed(Class<T> target, org.bukkit.Keyed source, java.util.function.Consumer<T> writer) {
        try {
            writer.accept(Enum.valueOf(target, source.getKey().getKey().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException mismatch) {
            Debugger.echoError(null, "EntityLib has no " + target.getSimpleName() + " named " + source.getKey().getKey() + ".");
        }
    }
}

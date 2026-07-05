package dev.corexinc.corex.nms.v1_21_11;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ColorTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.ReflectionHelper;
import dev.corexinc.corex.environment.utils.adapters.BiomeAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BiomeAdapterImpl implements BiomeAdapter {

    private Biome getNmsBiome(World world, NamespacedKey key) {
        if (world == null || key == null) return null;
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();

        Identifier nmsLocation = Identifier.fromNamespaceAndPath(key.getNamespace(), key.getKey());
        ResourceKey<@NotNull Biome> resourceKey = ResourceKey.create(Registries.BIOME, nmsLocation);
        return serverLevel.registryAccess().lookupOrThrow(Registries.BIOME)
                .get(resourceKey)
                .map(net.minecraft.core.Holder::value)
                .orElse(null);
    }

    @Override
    public List<NamespacedKey> getAllBiomeKeys(World world) {
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Registry<Biome> biomeRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.BIOME);

        return biomeRegistry.listElementIds()
                .map(resourceKey -> new NamespacedKey(resourceKey.identifier().getNamespace(), resourceKey.identifier().getPath()))
                .toList();
    }

    public <T> T getEnvAttr(Biome biome, EnvironmentAttribute<@NotNull T> attr) {
        return biome.getAttributes().applyModifier(attr, attr.defaultValue());
    }

    public <T> void setEnvAttr(World world, NamespacedKey key, EnvironmentAttribute<@NotNull T> attr, T value) {
        Biome nmsBiome = getNmsBiome(world, key);
        if (nmsBiome == null) return;

        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        builder.putAll(nmsBiome.getAttributes());

        builder.set(attr, value);

        EnvironmentAttributeMap newMap = builder.build();

        ReflectionHelper.setFinalFieldByType(nmsBiome, EnvironmentAttributeMap.class, newMap);

        syncBiome(world, key);
    }

    @SuppressWarnings("unchecked")
    private void syncBiome(World world, NamespacedKey key) {
        try {
            ServerLevel serverLevel = ((CraftWorld) world).getHandle();

            MappedRegistry<@NotNull Biome> mappedRegistry = (MappedRegistry<@NotNull Biome>) serverLevel.registryAccess().lookupOrThrow(Registries.BIOME);

            Identifier nmsLocation = Identifier.parse(key.toString());

            ResourceKey<@NotNull Biome> nmsKey = ResourceKey.create(Registries.BIOME, nmsLocation);

            Map<ResourceKey<Biome>, RegistrationInfo> registrationInfos = (Map<ResourceKey<Biome>, RegistrationInfo>) ReflectionHelper.getFieldValue(
                    MappedRegistry.class, "registrationInfos", mappedRegistry
            );

            if (registrationInfos != null) {
                registrationInfos.put(nmsKey, RegistrationInfo.BUILT_IN);
            }
        } catch (Throwable ignored) {}
    }

    private void modifyClimate(Biome nmsBiome, Float temperature, Float humidity, Boolean hasDownfall) {
        Biome.ClimateSettings oldClimate = nmsBiome.climateSettings;

        boolean resolvedHasDownfall = hasDownfall != null ? hasDownfall : oldClimate.hasPrecipitation();
        float resolvedTemperature = temperature != null ? temperature : oldClimate.temperature();
        float resolvedHumidity = humidity != null ? humidity : oldClimate.downfall();

        Biome.ClimateSettings newClimate = new Biome.ClimateSettings(
                resolvedHasDownfall, resolvedTemperature, oldClimate.temperatureModifier(), resolvedHumidity
        );

        ReflectionHelper.setFinalField(nmsBiome, "climateSettings", newClimate);
    }

    @Override
    public float getTemperature(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        return nmsBiome != null ? nmsBiome.getBaseTemperature() : 0.5f;
    }

    @Override
    public float getDownfall(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        return nmsBiome != null ? nmsBiome.climateSettings.downfall() : 0.5f;
    }

    @Override
    public Optional<Integer> getWaterColor(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return Optional.empty();
        return Optional.of(nmsBiome.getSpecialEffects().waterColor());
    }

    @Override
    public Optional<Integer> getFogColor(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return Optional.empty();
        return Optional.of(getEnvAttr(nmsBiome, EnvironmentAttributes.FOG_COLOR));
    }

    @Override
    public List<EntityType> getSpawns(World world, NamespacedKey biomeKey, SpawnCategory category) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        List<EntityType> entities = new ArrayList<>();
        if (nmsBiome == null) return entities;

        MobCategory nmsCategory = MobCategory.valueOf(category.name());

        MobSpawnSettings spawnSettings = nmsBiome.getMobSettings();
        for (Weighted<MobSpawnSettings.@NotNull SpawnerData> wrapper : spawnSettings.getMobs(nmsCategory).unwrap()) {
            MobSpawnSettings.SpawnerData spawnerData = wrapper.value();
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(spawnerData.type()).getPath();
            try {
                entities.add(EntityType.valueOf(entityId.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return entities;
    }

    @Override
    public void fillBiome(Location min, Location max, NamespacedKey biomeKey) {
        World world = min.getWorld();
        if (world == null) return;

        @SuppressWarnings("deprecation")
        org.bukkit.block.Biome bukkitBiome = org.bukkit.Registry.BIOME.get(biomeKey);
        if (bukkitBiome == null) return;

        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());

        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        minY = Math.max(minY, world.getMinHeight());
        maxY = Math.min(maxY, world.getMaxHeight() - 1);

        int startX = (minX >> 2) << 2;
        int endX = (maxX >> 2) << 2;
        int startY = (minY >> 2) << 2;
        int endY = (maxY >> 2) << 2;
        int startZ = (minZ >> 2) << 2;
        int endZ = (maxZ >> 2) << 2;

        Set<Long> updatedChunks = new HashSet<>();

        for (int x = startX; x <= endX; x += 4) {
            for (int y = startY; y <= endY; y += 4) {
                for (int z = startZ; z <= endZ; z += 4) {
                    world.setBiome(x, y, z, bukkitBiome);

                    updatedChunks.add(((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL));
                }
            }
        }

        ServerLevel serverLevel = ((CraftWorld) world).getHandle();

        for (long chunkHash : updatedChunks) {
            int cx = (int) (chunkHash >> 32);
            int cz = (int) chunkHash;

            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) continue;

            chunk.markUnsaved();

            ClientboundChunksBiomesPacket.ChunkBiomeData biomeData = new ClientboundChunksBiomesPacket.ChunkBiomeData(chunk);

            ClientboundChunksBiomesPacket packet = new ClientboundChunksBiomesPacket(List.of(biomeData));

            serverLevel.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)
                    .forEach(player -> player.connection.send(packet));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getTemperatureAt(Location location) {
        World world = location.getWorld();
        if (world == null) return 0.5f;

        ServerLevel serverLevel = ((CraftWorld) world).getHandle();

        BlockPos pos = new BlockPos(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()
        );

        int seaLevel = serverLevel.getSeaLevel();

        return serverLevel.getBiome(pos).value().getTemperature(pos, seaLevel);
    }

    @Override
    public boolean hasDownfall(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        return nmsBiome != null && nmsBiome.hasPrecipitation();
    }

    @Override
    public Optional<Integer> getFoliageColor(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return Optional.empty();

        return nmsBiome.getSpecialEffects().foliageColorOverride();
    }

    @Override
    public Optional<Integer> getWaterFogColor(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return Optional.empty();

        return Optional.of(getEnvAttr(nmsBiome, EnvironmentAttributes.WATER_FOG_COLOR));
    }

    @Override
    public void setFoliageColor(World world, NamespacedKey biomeKey, int color) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return;

        BiomeSpecialEffects old = nmsBiome.getSpecialEffects();
        BiomeSpecialEffects newEffects = new BiomeSpecialEffects(
                old.waterColor(),
                Optional.of(color),
                old.dryFoliageColorOverride(),
                old.grassColorOverride(),
                old.grassColorModifier()
        );

        applySpecialEffects(world, biomeKey, nmsBiome, newEffects);
    }

    @Override
    public void setFogColor(World world, NamespacedKey biomeKey, int color) {
        setEnvAttr(world, biomeKey, EnvironmentAttributes.FOG_COLOR, color);
    }

    @Override
    public void setWaterColor(World world, NamespacedKey biomeKey, int color) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return;

        BiomeSpecialEffects old = nmsBiome.getSpecialEffects();
        BiomeSpecialEffects newEffects = new BiomeSpecialEffects(
                color,
                old.foliageColorOverride(),
                old.dryFoliageColorOverride(),
                old.grassColorOverride(),
                old.grassColorModifier()
        );

        applySpecialEffects(world, biomeKey, nmsBiome, newEffects);
    }

    private void applySpecialEffects(World world, NamespacedKey biomeKey, Biome nmsBiome, BiomeSpecialEffects newEffects) {
        ReflectionHelper.setFinalFieldByType(nmsBiome, BiomeSpecialEffects.class, newEffects);
        syncBiome(world, biomeKey);
    }

    @Override
    public void setWaterFogColor(World world, NamespacedKey biomeKey, int color) {
        setEnvAttr(world, biomeKey, EnvironmentAttributes.WATER_FOG_COLOR, color);
    }

    @Override
    public void setBaseTemperature(World world, NamespacedKey biomeKey, float temperature) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome != null) modifyClimate(nmsBiome, temperature, null, null);
    }

    @Override
    public void setHumidity(World world, NamespacedKey biomeKey, float humidity) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome != null) modifyClimate(nmsBiome, null, humidity, null);
    }

    @Override
    public void setHasDownfall(World world, NamespacedKey biomeKey, boolean hasDownfall) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome != null) modifyClimate(nmsBiome, null, null, hasDownfall);
    }

    @SuppressWarnings("unchecked")
    private <T> Object applyDynamic(Biome biome, Holder.Reference<EnvironmentAttribute<?>> holder) {
        EnvironmentAttribute<T> attr = (EnvironmentAttribute<T>) holder.value();
        return biome.getAttributes().applyModifier(attr, attr.defaultValue());
    }

    private AbstractTag normalize(Object value) {
        return switch (value) {
            case null -> null;
            case Holder<?> holder -> normalize(holder.value());
            case Optional<?> optional -> optional.map(this::normalize).orElse(null);
            case Identifier identifier -> new ElementTag(identifier.toString());
            case Integer color -> new ColorTag(
                    (color >> 16) & 0xFF,
                    (color >> 8) & 0xFF,
                    color & 0xFF
            );
            case List<?> list -> {
                List<Object> normalized = new ArrayList<>();
                for (Object element : list) {
                    normalized.add(normalize(element));
                }
                yield new ListTag(normalized);
            }
            case AmbientSounds sounds -> normalizeAmbientSounds(sounds);
            default -> new ElementTag(value.toString());
        };
    }

    private MapTag normalizeAmbientSounds(AmbientSounds sounds) {
        MapTag map = new MapTag();

        sounds.loop().ifPresent(loop ->
                map.putObject("loop", normalize(loop.value().location()))
        );

        sounds.mood().ifPresent(mood -> {
            MapTag moodMap = new MapTag();
            moodMap.putObject("sound", normalize(mood.soundEvent().value().location()));
            moodMap.putObject("delay", new ElementTag(mood.tickDelay()));
            moodMap.putObject("offset", new ElementTag(mood.soundPositionOffset()));
            map.putObject("mood", moodMap);
        });

        ListTag additions = new ListTag();
        for (AmbientAdditionsSettings addition : sounds.additions()) {
            MapTag additionMap = new MapTag();
            additionMap.putObject("sound", normalize(addition.soundEvent().value().location()));
            additionMap.putObject("chance", new ElementTag(addition.tickChance()));
            additions.addObject(additionMap);
        }

        map.putObject("additions", additions);
        return map;
    }

    @Override
    public Object getDynamicAttribute(World world, NamespacedKey biomeKey, String attrName) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return null;

        Optional<Holder.Reference<EnvironmentAttribute<?>>> attributeHolder = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.get(Identifier.parse(attrName));
        Holder.Reference<EnvironmentAttribute<?>> holder = attributeHolder.orElse(null);
        if (holder == null) return null;

        Object raw = applyDynamic(nmsBiome, holder);

        return normalize(raw);
    }

    @Override
    public void setDynamicAttribute(World world, NamespacedKey biomeKey, String attrName, Object value) {
        Optional<Holder.Reference<EnvironmentAttribute<?>>> attributeHolder = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.get(Identifier.parse(attrName));

        Holder.Reference<EnvironmentAttribute<?>> holder = attributeHolder.orElse(null);
        if (holder == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        EnvironmentAttribute<@NotNull Object> attribute = (EnvironmentAttribute<@NotNull Object>) holder.value();

        setEnvAttr(world, biomeKey, attribute, value);
    }

    @Override
    public List<String> getAttributes() {
        return BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.keySet().stream().map(Object::toString).toList();
    }
}
package dev.corexinc.corex.nms.v1_21_9;

import dev.corexinc.corex.environment.utils.ReflectionHelper;
import dev.corexinc.corex.environment.utils.adapters.BiomeAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
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

import java.util.*;

public class BiomeAdapterImpl implements BiomeAdapter {

    private Biome getNmsBiome(World world, NamespacedKey key) {
        if (world == null || key == null) return null;
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();

        ResourceLocation nmsLocation = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey());
        ResourceKey<Biome> resourceKey = ResourceKey.create(Registries.BIOME, nmsLocation);
        return serverLevel.registryAccess().lookupOrThrow(Registries.BIOME)
                .get(resourceKey)
                .map(net.minecraft.core.Holder::value)
                .orElse(null);
    }

    @Override
    public List<NamespacedKey> getAllBiomeKeys(World world) {
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        var registry = serverLevel.registryAccess().lookupOrThrow(Registries.BIOME);

        return registry.listElementIds()
                .map(resourceKey -> new NamespacedKey(resourceKey.location().getNamespace(), resourceKey.location().getPath()))
                .toList();
    }

    private BiomeSpecialEffects modifySpecialEffects(Biome nmsBiome, Integer foliageColor, Integer fogColor, Integer waterColor, Integer waterFogColor) {
        BiomeSpecialEffects old = nmsBiome.getSpecialEffects();

        BiomeSpecialEffects.Builder builder = new BiomeSpecialEffects.Builder()
                .fogColor(fogColor != null ? fogColor : old.getFogColor())
                .waterColor(waterColor != null ? waterColor : old.getWaterColor())
                .waterFogColor(waterFogColor != null ? waterFogColor : old.getWaterFogColor())
                .skyColor(old.getSkyColor());

        if (foliageColor != null) {
            builder.foliageColorOverride(foliageColor);
        } else {
            old.getFoliageColorOverride().ifPresent(builder::foliageColorOverride);
        }

        old.getGrassColorOverride().ifPresent(builder::grassColorOverride);
        builder.grassColorModifier(old.getGrassColorModifier());
        old.getAmbientParticleSettings().ifPresent(builder::ambientParticle);
        old.getAmbientLoopSoundEvent().ifPresent(builder::ambientLoopSound);
        old.getAmbientMoodSettings().ifPresent(builder::ambientMoodSound);
        old.getAmbientAdditionsSettings().ifPresent(builder::ambientAdditionsSound);
        old.getBackgroundMusic().ifPresent(builder::backgroundMusic);

        return builder.build();
    }

    private void modifyClimate(Biome nmsBiome, Float temperature, Float humidity, Boolean hasDownfall) {
        Biome.ClimateSettings old = nmsBiome.climateSettings;

        boolean down = hasDownfall != null ? hasDownfall : old.hasPrecipitation();
        float temp = temperature != null ? temperature : old.temperature();
        float hum = humidity != null ? humidity : old.downfall();

        Biome.ClimateSettings newClimate = new Biome.ClimateSettings(down, temp, old.temperatureModifier(), hum);

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
        return Optional.of(nmsBiome.getSpecialEffects().getWaterColor());
    }

    @Override
    public Optional<Integer> getFogColor(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return Optional.empty();
        return Optional.of(nmsBiome.getSpecialEffects().getFogColor());
    }

    @Override
    public List<EntityType> getSpawns(World world, NamespacedKey biomeKey, SpawnCategory category) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        List<EntityType> entities = new ArrayList<>();
        if (nmsBiome == null) return entities;

        MobCategory nmsCategory = MobCategory.valueOf(category.name());

        MobSpawnSettings spawnSettings = nmsBiome.getMobSettings();
        for (Weighted<MobSpawnSettings.SpawnerData> wrapper : spawnSettings.getMobs(nmsCategory).unwrap()) {
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

            var biomeData = new ClientboundChunksBiomesPacket.ChunkBiomeData(chunk);

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

        return nmsBiome.getSpecialEffects().getFoliageColorOverride();
    }

    @Override
    public Optional<Integer> getWaterFogColor(World world, NamespacedKey biomeKey) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return Optional.empty();

        return Optional.of(nmsBiome.getSpecialEffects().getWaterFogColor());
    }

    @Override
    public void setFoliageColor(World world, NamespacedKey biomeKey, int color) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return;
        BiomeSpecialEffects newEffects = modifySpecialEffects(nmsBiome, color, null, null, null);
        ReflectionHelper.setFinalField(nmsBiome, "specialEffects", newEffects);
    }

    @Override
    public void setFogColor(World world, NamespacedKey biomeKey, int color) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return;
        BiomeSpecialEffects newEffects = modifySpecialEffects(nmsBiome, null, color, null, null);
        ReflectionHelper.setFinalField(nmsBiome, "specialEffects", newEffects);
    }

    @Override
    public void setWaterColor(World world, NamespacedKey biomeKey, int color) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return;
        BiomeSpecialEffects newEffects = modifySpecialEffects(nmsBiome, null, null, color, null);
        ReflectionHelper.setFinalField(nmsBiome, "specialEffects", newEffects);
    }

    @Override
    public void setWaterFogColor(World world, NamespacedKey biomeKey, int color) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return;
        BiomeSpecialEffects newEffects = modifySpecialEffects(nmsBiome, null, null, null, color);
        ReflectionHelper.setFinalField(nmsBiome, "specialEffects", newEffects);
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
}
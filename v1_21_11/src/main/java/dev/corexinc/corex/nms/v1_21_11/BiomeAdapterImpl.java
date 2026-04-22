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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
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

import java.util.*;

public class BiomeAdapterImpl implements BiomeAdapter {

    private Biome getNmsBiome(World world, NamespacedKey key) {
        if (world == null || key == null) return null;
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();

        Identifier nmsLocation = Identifier.fromNamespaceAndPath(key.getNamespace(), key.getKey());
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
                .map(resourceKey -> new NamespacedKey(resourceKey.identifier().getNamespace(), resourceKey.identifier().getPath()))
                .toList();
    }

    public <T> T getEnvAttr(Biome biome, EnvironmentAttribute<T> attr) {
        return biome.getAttributes().applyModifier(attr, attr.defaultValue());
    }

    public <T> void setEnvAttr(World world, NamespacedKey key, EnvironmentAttribute<T> attr, T value) {
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

            var registry = (MappedRegistry<Biome>) serverLevel.registryAccess().lookupOrThrow(Registries.BIOME);

            Identifier nmsLocation = Identifier.parse(key.toString());

            ResourceKey<Biome> nmsKey = ResourceKey.create(Registries.BIOME, nmsLocation);

            Map infos = (Map) ReflectionHelper.getFieldValue(
                    MappedRegistry.class, "registrationInfos", registry
            );

            if (infos != null) {
                infos.put(nmsKey, RegistrationInfo.BUILT_IN);
            }
        } catch (Throwable ignored) {}
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
        setEnvAttr(world, biomeKey, EnvironmentAttributes.WATER_FOG_COLOR, color);
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
        switch (value) {
            case null -> {
                return null;
            }
            case Holder<?> holder -> {
                return normalize(holder.value());
            }
            case Optional<?> opt -> {
                return opt.map(this::normalize).orElse(null);
            }
            case Identifier id -> {
                return new ElementTag(id.toString());
            }
            case Integer color -> {
                return new ColorTag(
                        (color >> 16) & 0xFF,
                        (color >> 8) & 0xFF,
                        color & 0xFF
                );
            }
            case List<?> list -> {
                List<Object> out = new ArrayList<>();
                for (Object o : list) {
                    out.add(normalize(o));
                }
                return new ListTag(out);
            }
            case AmbientSounds sounds -> {
                MapTag map = new MapTag();

                sounds.loop().ifPresent(loop ->
                        map.putObject("loop", normalize(loop.value().location()))
                );

                sounds.mood().ifPresent(mood -> {
                    MapTag m = new MapTag();
                    m.putObject("sound", normalize(mood.soundEvent().value().location()));
                    m.putObject("delay", new ElementTag(mood.tickDelay()));
                    m.putObject("offset", new ElementTag(mood.soundPositionOffset()));
                    map.putObject("mood", m);
                });

                ListTag additions = new ListTag();
                for (var add : sounds.additions()) {
                    MapTag m = new MapTag();
                    m.putObject("sound", normalize(add.soundEvent().value().location()));
                    m.putObject("chance", new ElementTag(add.tickChance()));
                    additions.addObject(m);
                }

                map.putObject("additions", additions);

                return map;
            }
            default -> {
            }
        }

        return new ElementTag(value.toString());
    }

    @Override
    public Object getDynamicAttribute(World world, NamespacedKey biomeKey, String attrName) {
        Biome nmsBiome = getNmsBiome(world, biomeKey);
        if (nmsBiome == null) return null;

        var optional = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.get(Identifier.parse(attrName));
        var holder = optional.orElse(null);
        if (holder == null) return null;

        Object raw = applyDynamic(nmsBiome, holder);

        return normalize(raw);
    }

    @Override
    public void setDynamicAttribute(World world, NamespacedKey biomeKey, String attrName, Object value) {
        var optional = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.get(Identifier.parse(attrName));

        var holder = optional.orElse(null);
        if (holder == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        EnvironmentAttribute<Object> attribute = (EnvironmentAttribute<Object>) holder.value();

        setEnvAttr(world, biomeKey, attribute, value);
    }

    @Override
    public List<String> getAttributes() {
        return BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.keySet().stream().map(Object::toString).toList();
    }
}
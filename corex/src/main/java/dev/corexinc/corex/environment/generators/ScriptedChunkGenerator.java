package dev.corexinc.corex.environment.generators;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.environment.containers.GeneratorContainer;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.world.BiomeTag;
import dev.corexinc.corex.environment.tags.world.ChunkTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import dev.corexinc.corex.environment.tags.world.WorldTag;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ScriptedChunkGenerator extends ChunkGenerator {

    public static final String TEMP_LIMITED_REGION = "__limitedRegion";

    private final String containerName;
    private final MapTag instanceDefs;
    private volatile Boolean cachedCanSpawn = null;

    public ScriptedChunkGenerator(@NotNull String containerName, @NotNull MapTag instanceDefs) {
        this.containerName = containerName;
        this.instanceDefs = instanceDefs;
    }

    @Nullable
    private GeneratorContainer container() {
        Object raw = ScriptManager.getContainer(containerName);
        return raw instanceof GeneratorContainer gc ? gc : null;
    }

    @Override
    public boolean shouldGenerateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_NOISE)) return true;
        return gc.isVanillaFirst(GeneratorContainer.SECTION_NOISE);
    }

    @Override
    public boolean shouldGenerateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_SURFACE)) return true;
        return gc.isVanillaFirst(GeneratorContainer.SECTION_SURFACE);
    }

    @Override
    public boolean shouldGenerateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_CAVES)) return true;
        return gc.isVanillaFirst(GeneratorContainer.SECTION_CAVES);
    }

    @Override
    public boolean shouldGenerateDecorations(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_POPULATORS)) return true;
        return gc.isVanillaFirst(GeneratorContainer.SECTION_POPULATORS);
    }

    @Override
    public boolean shouldGenerateMobs(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        if (cachedCanSpawn != null) return cachedCanSpawn;

        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_CAN_SPAWN)) {
            cachedCanSpawn = true;
            return true;
        }

        ContextTag ctx = new ContextTag();
        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_CAN_SPAWN, ctx, instanceDefs);
        if (queue == null) {
            cachedCanSpawn = true;
            return true;
        }

        List<AbstractTag> returns = queue.getReturns();
        boolean result = !returns.isEmpty() && returns.getFirst() instanceof ElementTag el && el.asBoolean();

        cachedCanSpawn = result;
        return result;
    }

    @Override
    public boolean shouldGenerateStructures(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        runGenerationSection(GeneratorContainer.SECTION_NOISE, worldInfo, chunkX, chunkZ, chunkData);
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        runGenerationSection(GeneratorContainer.SECTION_SURFACE, worldInfo, chunkX, chunkZ, chunkData);
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        runGenerationSection(GeneratorContainer.SECTION_BEDROCK, worldInfo, chunkX, chunkZ, chunkData);
    }

    @Override
    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        runGenerationSection(GeneratorContainer.SECTION_CAVES, worldInfo, chunkX, chunkZ, chunkData);
    }

    @Override
    @Nullable
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_BIOME)) return null;
        return new ScriptedBiomeProvider(containerName, instanceDefs);
    }

    @Override
    @NotNull
    public List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_POPULATORS)) {
            return Collections.emptyList();
        }
        return List.of(new ScriptedBlockPopulator(containerName, instanceDefs));
    }

    @Override
    @Nullable
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_SPAWN_LOCATION)) return null;

        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_SPAWN_LOCATION, buildSpawnContext(world), instanceDefs);
        if (queue == null) return null;

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return null;

        AbstractTag ret = returns.getFirst();
        Location loc = ret instanceof LocationTag lt ? lt.getLocation() : new LocationTag(ret.identify()).getLocation();

        if (loc.getWorld() == null) loc.setWorld(world);
        return loc;
    }

    private ContextTag buildChunkContext(@NotNull WorldInfo worldInfo, int chunkX, int chunkZ) {
        World world = Bukkit.getWorld(worldInfo.getName());
        ContextTag ctx = new ContextTag()
                .put("baseHeight", new ElementTag(resolveBaseHeight(worldInfo, chunkX, chunkZ)))
                .put("worldName", new ElementTag(worldInfo.getName()))
                .put("minHeight", new ElementTag(worldInfo.getMinHeight()));

        addChunkContext(ctx, world, chunkX, chunkZ);
        return ctx;
    }

    private ContextTag buildSpawnContext(@NotNull World world) {
        int chunkX = 0;
        int chunkZ = 0;
        ContextTag ctx = new ContextTag()
                .put("baseHeight", new ElementTag(resolveBaseHeight(containerName, world, chunkX, chunkZ, instanceDefs)))
                .put("worldName", new ElementTag(world.getName()))
                .put("minHeight", new ElementTag(world.getMinHeight()));

        addChunkContext(ctx, world, chunkX, chunkZ);
        return ctx;
    }

    private int resolveBaseHeight(@NotNull WorldInfo worldInfo, int chunkX, int chunkZ) {
        return resolveBaseHeight(containerName, worldInfo, chunkX, chunkZ, instanceDefs);
    }

    private static int resolveBaseHeight(@NotNull String containerName, @NotNull WorldInfo worldInfo, int chunkX, int chunkZ) {
        return resolveBaseHeight(containerName, worldInfo, chunkX, chunkZ, new MapTag());
    }

    private static int resolveBaseHeight(@NotNull String containerName, @NotNull WorldInfo worldInfo, int chunkX, int chunkZ, @NotNull MapTag instanceDefs) {
        Object raw = ScriptManager.getContainer(containerName);
        if (!(raw instanceof GeneratorContainer gc)) return 64;
        if (!gc.hasSection(GeneratorContainer.SECTION_BASE_HEIGHT)) return 64;

        World world = Bukkit.getWorld(worldInfo.getName());
        ContextTag ctx = new ContextTag();
        addChunkContext(ctx, world, chunkX, chunkZ);

        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_BASE_HEIGHT, ctx, instanceDefs);
        if (queue == null) return 64;

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return 64;

        AbstractTag ret = returns.getFirst();
        return (ret instanceof ElementTag el && el.isInt()) ? el.asInt() : 64;
    }

    private static int resolveBaseHeight(@NotNull String containerName, @NotNull World world, int chunkX, int chunkZ) {
        return resolveBaseHeight(containerName, world, chunkX, chunkZ, new MapTag());
    }

    private static int resolveBaseHeight(@NotNull String containerName, @NotNull World world, int chunkX, int chunkZ, @NotNull MapTag instanceDefs) {
        Object raw = ScriptManager.getContainer(containerName);
        if (!(raw instanceof GeneratorContainer gc)) return 64;
        if (!gc.hasSection(GeneratorContainer.SECTION_BASE_HEIGHT)) return 64;

        ContextTag ctx = new ContextTag();
        addChunkContext(ctx, world, chunkX, chunkZ);

        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_BASE_HEIGHT, ctx, instanceDefs);
        if (queue == null) return 64;

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return 64;

        AbstractTag ret = returns.getFirst();
        return (ret instanceof ElementTag el && el.isInt()) ? el.asInt() : 64;
    }

    private void runGenerationSection(@NotNull String section, @NotNull WorldInfo worldInfo, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(section)) return;

        ScriptQueue queue = gc.createQueue(section, buildChunkContext(worldInfo, chunkX, chunkZ), instanceDefs);
        if (queue == null) return;

        queue.start();

        for (AbstractTag ret : queue.getReturns()) {
            if (!(ret instanceof MapTag mapTag)) continue;

            for (String key : mapTag.keySet()) {
                try {
                    LocationTag loc = new LocationTag(key);
                    Location l = loc.getLocation();

                    AbstractTag val = mapTag.getObject(key);
                    MaterialTag mat = val instanceof MaterialTag m ? m : new MaterialTag(val.identify());

                    if (mat.getBlockData() == null) continue;

                    chunkData.setBlock(l.getBlockX() & 15, l.getBlockY(), l.getBlockZ() & 15, mat.getBlockData());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static class ScriptedBiomeProvider extends BiomeProvider {

        private final String containerName;
        private final MapTag instanceDefs;

        ScriptedBiomeProvider(@NotNull String containerName, @NotNull MapTag instanceDefs) {
            this.containerName = containerName;
            this.instanceDefs = instanceDefs;
        }

        @Override
        public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
            Object raw = ScriptManager.getContainer(containerName);
            if (!(raw instanceof GeneratorContainer gc)) return Biome.PLAINS;

            World world = Bukkit.getWorld(info.getName());
            ContextTag ctx = new ContextTag()
                    .put("location", new LocationTag(new Location(world, x, y, z)));
            if (world != null) {
                ctx.put("world", new WorldTag(world));
            }
            ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_BIOME, ctx, instanceDefs);
            if (queue == null) return Biome.PLAINS;

            List<AbstractTag> returns = queue.getReturns();
            if (returns.isEmpty()) return Biome.PLAINS;

            AbstractTag ret = returns.getFirst();
            BiomeTag biomeTag = ret instanceof BiomeTag bt ? bt : new BiomeTag(ret.identify());
            if (biomeTag.getBiomeKey() == null) return Biome.PLAINS;

            Biome resolved = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(biomeTag.getBiomeKey());
            return resolved != null ? resolved : Biome.PLAINS;
        }

        @Override
        public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
            return List.copyOf(RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream().toList());
        }
    }

    private static class ScriptedBlockPopulator extends BlockPopulator {

        private final String containerName;
        private final MapTag instanceDefs;

        ScriptedBlockPopulator(@NotNull String containerName, @NotNull MapTag instanceDefs) {
            this.containerName = containerName;
            this.instanceDefs = instanceDefs;
        }

        @Override
        public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
            Object raw = ScriptManager.getContainer(containerName);
            if (!(raw instanceof GeneratorContainer gc)) return;
            if (!gc.hasSection(GeneratorContainer.SECTION_POPULATORS)) return;

            World world = Bukkit.getWorld(worldInfo.getName());
            ContextTag ctx = new ContextTag()
                    .put("baseHeight", new ElementTag(resolveBaseHeight(containerName, worldInfo, chunkX, chunkZ, instanceDefs)));

            addChunkContext(ctx, world, chunkX, chunkZ);

            ScriptQueue queue = gc.createQueue(GeneratorContainer.SECTION_POPULATORS, ctx, instanceDefs);
            if (queue == null) return;

            queue.setTempData(TEMP_LIMITED_REGION, limitedRegion);
            queue.start();
        }
    }

    private static void addChunkContext(@NotNull ContextTag ctx, @Nullable World world, int chunkX, int chunkZ) {
        ctx.put("chunkX", new ElementTag(chunkX))
                .put("chunkZ", new ElementTag(chunkZ));

        if (world != null) {
            ctx.put("world", new WorldTag(world))
                    .put("chunk", new ChunkTag(world, chunkX, chunkZ));
        }
    }
}
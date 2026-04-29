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
import dev.corexinc.corex.environment.tags.world.WorldTag;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
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
    public static final String TEMP_CHUNK_DATA = "__chunkData";

    private final String containerName;
    private final MapTag instanceDefs;

    private volatile Boolean cachedCanSpawn;

    public ScriptedChunkGenerator(@NotNull String containerName, @NotNull MapTag instanceDefs) {
        this.containerName = containerName;
        this.instanceDefs = instanceDefs;
    }

    @Nullable
    private GeneratorContainer container() {
        Object raw = ScriptManager.getContainer(containerName);
        return raw instanceof GeneratorContainer gc ? gc : null;
    }

    private boolean vanillaFirst(@NotNull String section, boolean def) {
        GeneratorContainer gc = container();
        return gc == null || !gc.hasSection(section) ? def : gc.isVanillaFirst(section);
    }

    @Override
    public boolean shouldGenerateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ) { return vanillaFirst(GeneratorContainer.SECTION_NOISE, true); }

    @Override
    public boolean shouldGenerateSurface(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ) { return vanillaFirst(GeneratorContainer.SECTION_SURFACE, true); }

    @Override
    public boolean shouldGenerateCaves(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ) { return vanillaFirst(GeneratorContainer.SECTION_CAVES, true); }

    @Override
    public boolean shouldGenerateDecorations(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ) { return vanillaFirst(GeneratorContainer.SECTION_POPULATORS, true); }

    @Override
    public boolean shouldGenerateMobs(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ) {
        if (cachedCanSpawn != null) return cachedCanSpawn;
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_CAN_SPAWN)) return cachedCanSpawn = true;

        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_CAN_SPAWN, buildChunkContext(info, chunkX, chunkZ), instanceDefs);
        boolean result = queue != null && !queue.getReturns().isEmpty() && queue.getReturns().getFirst() instanceof ElementTag el && el.asBoolean();
        return cachedCanSpawn = result;
    }

    @Override
    public boolean shouldGenerateStructures(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ) { return false; }

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) { runSection(GeneratorContainer.SECTION_NOISE, info, chunkX, chunkZ, chunkData); }

    @Override
    public void generateSurface(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) { runSection(GeneratorContainer.SECTION_SURFACE, info, chunkX, chunkZ, chunkData); }

    @Override
    public void generateBedrock(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) { runSection(GeneratorContainer.SECTION_BEDROCK, info, chunkX, chunkZ, chunkData); }

    @Override
    public void generateCaves(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) { runSection(GeneratorContainer.SECTION_CAVES, info, chunkX, chunkZ, chunkData); }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo info) {
        GeneratorContainer gc = container();
        return (gc == null || !gc.hasSection(GeneratorContainer.SECTION_BIOME)) ? null : new ScriptedBiomeProvider(containerName, instanceDefs);
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        GeneratorContainer gc = container();
        return (gc == null || !gc.hasSection(GeneratorContainer.SECTION_POPULATORS)) ? Collections.emptyList() : List.of(new ScriptedBlockPopulator(containerName, instanceDefs));
    }

    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_SPAWN_LOCATION)) return null;

        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_SPAWN_LOCATION, buildSpawnContext(world), instanceDefs);
        if (queue == null || queue.getReturns().isEmpty()) return null;

        AbstractTag ret = queue.getReturns().getFirst();
        Location loc = ret instanceof LocationTag lt ? lt.getLocation() : new LocationTag(ret.identify()).getLocation();
        if (loc.getWorld() == null) loc.setWorld(world);
        return loc;
    }

    private static ContextTag buildChunkContext(@NotNull WorldInfo info, int chunkX, int chunkZ) {
        return new ContextTag()
                .put("world", new WorldTag(info))
                .put("chunk", new ChunkTag(info, chunkX, chunkZ));
    }

    private ContextTag buildSpawnContext(@NotNull World world) {
        return new ContextTag()
                .put("world", new WorldTag(world))
                .put("chunk", new ChunkTag(world, 0, 0));
    }

    private int resolveBaseHeight(@NotNull WorldInfo info, int chunkX, int chunkZ) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(GeneratorContainer.SECTION_BASE_HEIGHT)) return 64;

        ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_BASE_HEIGHT, buildChunkContext(info, chunkX, chunkZ), instanceDefs);
        return (queue != null && !queue.getReturns().isEmpty() && queue.getReturns().getFirst() instanceof ElementTag el && el.isInt()) ? el.asInt() : 64;
    }

    private int resolveBaseHeight(@NotNull World world, int chunkX, int chunkZ) { return resolveBaseHeight((WorldInfo) world, chunkX, chunkZ); }

    private void runSection(@NotNull String section, @NotNull WorldInfo info, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        GeneratorContainer gc = container();
        if (gc == null || !gc.hasSection(section)) return;

        ScriptQueue queue = gc.createQueue(section, buildChunkContext(info, chunkX, chunkZ), instanceDefs);
        if (queue != null) {
            queue.setTempData(TEMP_CHUNK_DATA, chunkData);
            queue.start();
        }
    }

    private static class ScriptedBiomeProvider extends BiomeProvider {
        private final String containerName;
        private final MapTag instanceDefs;

        public ScriptedBiomeProvider(String containerName, MapTag instanceDefs) {
            this.containerName = containerName;
            this.instanceDefs = instanceDefs;
        }

        @Override
        public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
            Object raw = ScriptManager.getContainer(containerName);
            if (!(raw instanceof GeneratorContainer gc)) return Biome.PLAINS;

            ScriptQueue queue = gc.runSection(GeneratorContainer.SECTION_BIOME,
                    new ContextTag().put("world", new WorldTag(info)).put("location", new LocationTag(info.getName() + "," + x + "," + y + "," + z)), instanceDefs);

            if (queue == null || queue.getReturns().isEmpty()) return Biome.PLAINS;
            BiomeTag bt = (queue.getReturns().getFirst() instanceof BiomeTag) ? (BiomeTag) queue.getReturns().getFirst() : new BiomeTag(queue.getReturns().getFirst().identify());
            Biome b = (bt.getBiomeKey() == null) ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(bt.getBiomeKey());
            return b != null ? b : Biome.PLAINS;
        }

        @Override public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) { return List.copyOf(RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream().toList()); }
    }

    private static class ScriptedBlockPopulator extends BlockPopulator {
        private final String containerName;
        private final MapTag instanceDefs;

        public ScriptedBlockPopulator(String containerName, MapTag instanceDefs) {
            this.containerName = containerName;
            this.instanceDefs = instanceDefs;
        }

        @Override
        public void populate(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion region) {
            Object raw = ScriptManager.getContainer(containerName);
            if (!(raw instanceof GeneratorContainer gc)) return;

            ScriptQueue queue = gc.createQueue(GeneratorContainer.SECTION_POPULATORS, buildChunkContext(info, chunkX, chunkZ), instanceDefs);
            if (queue != null) {
                queue.setTempData(TEMP_LIMITED_REGION, region);
                queue.start();
            }
        }
    }
}
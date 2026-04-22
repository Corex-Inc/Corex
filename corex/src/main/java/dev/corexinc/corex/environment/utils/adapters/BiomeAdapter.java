package dev.corexinc.corex.environment.utils.adapters;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Optional;

public interface BiomeAdapter {

    float getTemperature(World world, NamespacedKey biomeKey);

    float getDownfall(World world, NamespacedKey biomeKey);

    Optional<Integer> getWaterColor(World world, NamespacedKey biomeKey);

    Optional<Integer> getFogColor(World world, NamespacedKey biomeKey);

    List<EntityType> getSpawns(World world, NamespacedKey biomeKey, SpawnCategory category);

    void fillBiome(Location min, Location max, NamespacedKey biomeKey);

    enum SpawnCategory {
        MONSTER, CREATURE, AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, WATER_AMBIENT, MISC
    }

    float getTemperatureAt(Location location);

    boolean hasDownfall(World world, NamespacedKey biomeKey);

    Optional<Integer> getFoliageColor(World world, NamespacedKey biomeKey);

    Optional<Integer> getWaterFogColor(World world, NamespacedKey biomeKey);

    List<NamespacedKey> getAllBiomeKeys(World world);

    default Object getDynamicAttribute(World world, NamespacedKey biomeKey, String attrName) {
        return null;
    };

    default void setDynamicAttribute(World world, NamespacedKey biomeKey, String attrName, Object value) {};

    default List<String> getAttributes() {
        return null;
    };

    void setFoliageColor(World world, NamespacedKey biomeKey, int color);

    void setFogColor(World world, NamespacedKey biomeKey, int color);

    void setWaterColor(World world, NamespacedKey biomeKey, int color);

    void setWaterFogColor(World world, NamespacedKey biomeKey, int color);

    void setBaseTemperature(World world, NamespacedKey biomeKey, float temperature);

    void setHumidity(World world, NamespacedKey biomeKey, float humidity);

    void setHasDownfall(World world, NamespacedKey biomeKey, boolean hasDownfall);
}
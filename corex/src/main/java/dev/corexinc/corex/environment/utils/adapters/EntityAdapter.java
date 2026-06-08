package dev.corexinc.corex.environment.utils.adapters;

import dev.corexinc.corex.environment.tags.core.MapTag;
import org.bukkit.entity.Entity;

public interface EntityAdapter {

    MapTag readNbt(Entity entity);
}

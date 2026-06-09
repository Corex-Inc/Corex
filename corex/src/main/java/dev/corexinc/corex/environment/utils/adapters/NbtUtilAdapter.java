package dev.corexinc.corex.environment.utils.adapters;

import dev.corexinc.corex.environment.tags.core.MapTag;

public interface NbtUtilAdapter {

    MapTag toMap(Object compound);

    Object toNbt(MapTag map);
}

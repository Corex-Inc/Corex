package dev.corexinc.corex.nms.v1_21_4;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.adapters.NbtUtilAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

public class NbtUtilAdapterImpl implements NbtUtilAdapter {

    @Override
    public MapTag toMap(Object compound) {
        if (!(compound instanceof CompoundTag tag)) return new MapTag();

        MapTag map = new MapTag();
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            if (value != null) map.putObject(key, new ElementTag(value.toString()));
        }
        return map;
    }

    @Override
    public CompoundTag toNbt(MapTag map) {
        StringBuilder snbt = new StringBuilder("{");
        boolean first = true;
        for (String key : map.keySet()) {
            if (!first) snbt.append(',');
            snbt.append(key).append(':').append(map.getObject(key).identify());
            first = false;
        }
        snbt.append('}');

        try {
            return TagParser.parseTag(snbt.toString());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Invalid NBT: " + snbt, e);
        }
    }
}

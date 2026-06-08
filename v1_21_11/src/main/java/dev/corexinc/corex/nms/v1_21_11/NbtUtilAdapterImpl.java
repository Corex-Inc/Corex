package dev.corexinc.corex.nms.v1_21_11;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.adapters.NbtUtilAdapter;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NbtUtilAdapterImpl implements NbtUtilAdapter {

    @Override
    public MapTag toMap(Object compound) {
        if (!(compound instanceof CompoundTag tag)) return new MapTag();
        return convert(tag);
    }

    private MapTag convert(CompoundTag compound) {
        MapTag map = new MapTag();
        for (String key : compound.keySet()) {
            AbstractTag value = toTag(compound.get(key));
            if (value != null) map.putObject(key, value);
        }
        return map;
    }

    private AbstractTag toTag(Tag nbt) {
        if (nbt == null) return null;
        return switch (nbt) {
            case CompoundTag compound -> convert(compound);
            case net.minecraft.nbt.ListTag list -> toList(list);
            case ByteArrayTag bytes -> {
                ListTag result = new ListTag();
                for (byte value : bytes.getAsByteArray()) result.addObject(new ElementTag(value));
                yield result;
            }
            case IntArrayTag ints -> {
                ListTag result = new ListTag();
                for (int value : ints.getAsIntArray()) result.addObject(new ElementTag(value));
                yield result;
            }
            case LongArrayTag longs -> {
                ListTag result = new ListTag();
                for (long value : longs.getAsLongArray()) result.addObject(new ElementTag(String.valueOf(value)));
                yield result;
            }
            case FloatTag value -> new ElementTag(String.valueOf(value.floatValue()));
            case DoubleTag value -> new ElementTag(String.valueOf(value.doubleValue()));
            case NumericTag number -> new ElementTag(String.valueOf(number.longValue()));
            case StringTag string -> new ElementTag(string.value());
            default -> new ElementTag(nbt.toString());
        };
    }

    private ListTag toList(net.minecraft.nbt.ListTag list) {
        ListTag result = new ListTag();
        for (Tag tag : list) {
            AbstractTag value = toTag(tag);
            if (value != null) result.addObject(value);
        }
        return result;
    }
}

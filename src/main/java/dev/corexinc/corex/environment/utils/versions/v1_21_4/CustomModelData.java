package dev.corexinc.corex.environment.utils.versions.v1_21_4;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.versions.adapters.CustomModelDataAdapter;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class CustomModelData implements CustomModelDataAdapter {

    @Override
    public Object getCustomModelData(ItemStack item) {
        if (item == null) return null;

        var cmd = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (cmd == null) return null;

        Map<String, Object> map = new HashMap<>();
        if (!cmd.strings().isEmpty()) map.put("strings", cmd.strings());
        if (!cmd.colors().isEmpty()) map.put("colors", cmd.colors());
        if (!cmd.flags().isEmpty()) map.put("flags", cmd.flags());
        if (!cmd.floats().isEmpty()) map.put("floats", cmd.floats());

        return map;
    }

    @Override
    public void applyCustomModelData(ItemStack item, AbstractTag tag) {
        if (item == null || tag == null) return;
        if (!(tag instanceof MapTag mapTag)) return;

        var builder = io.papermc.paper.datacomponent.item.CustomModelData.customModelData();

        toList(mapTag.getObject("floats")).forEach(t -> {
            try { builder.addFloat(Float.parseFloat(t.identify())); }
            catch (NumberFormatException ignored) {}
        });

        toList(mapTag.getObject("colors")).forEach(t -> {
            try { builder.addColor(org.bukkit.Color.fromRGB(Integer.parseInt(t.identify()))); }
            catch (NumberFormatException ignored) {}
        });

        toList(mapTag.getObject("flags")).forEach(t ->
                builder.addFlag(Boolean.parseBoolean(t.identify()))
        );

        toList(mapTag.getObject("strings")).forEach(t ->
                builder.addString(t.identify())
        );

        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, builder.build());
    }

    private List<AbstractTag> toList(AbstractTag tag) {
        if (tag instanceof ListTag listTag) return listTag.getList();
        if (tag instanceof ElementTag) return List.of(tag);
        return List.of();
    }
}

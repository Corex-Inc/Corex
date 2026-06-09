package dev.corexinc.corex.nms.v1_21_11;

import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.adapters.EntityAdapter;
import dev.corexinc.corex.environment.utils.adapters.NbtUtilAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class EntityAdapterImpl implements EntityAdapter {

    private final NbtUtilAdapter nbt = NMSHandler.get().get(NbtUtilAdapter.class);

    @Override
    public MapTag readNbt(Entity entity) {
        if (entity == null || nbt == null) return new MapTag();
        return nbt.toMap(save(((CraftEntity) entity).getHandle()));
    }

    @Override
    public void applyNbt(Entity entity, MapTag data) {
        if (entity == null || nbt == null || data == null) return;

        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        CompoundTag current = save(handle);
        current.merge((CompoundTag) nbt.toNbt(data));
        handle.load(TagValueInput.create(ProblemReporter.DISCARDING, handle.registryAccess(), current));
    }

    private CompoundTag save(net.minecraft.world.entity.Entity handle) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, handle.registryAccess());
        handle.saveWithoutId(output);
        return output.buildResult();
    }
}

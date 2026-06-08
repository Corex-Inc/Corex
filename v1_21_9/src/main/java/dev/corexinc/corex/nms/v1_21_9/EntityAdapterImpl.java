package dev.corexinc.corex.nms.v1_21_9;

import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.adapters.EntityAdapter;
import dev.corexinc.corex.environment.utils.adapters.NbtUtilAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class EntityAdapterImpl implements EntityAdapter {

    private final NbtUtilAdapter nbt = NMSHandler.get().get(NbtUtilAdapter.class);

    @Override
    public MapTag readNbt(Entity entity) {
        if (entity == null || nbt == null) return new MapTag();

        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, handle.registryAccess());
        handle.saveWithoutId(output);
        CompoundTag compound = output.buildResult();

        return nbt.toMap(compound);
    }
}

package dev.corexinc.corex.environment.utils.entities;

import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeEntityRegistry {

    private static final Map<Integer, WrapperEntity> BY_ID = new ConcurrentHashMap<>();
    private static final Map<UUID, WrapperEntity> BY_UUID = new ConcurrentHashMap<>();

    private FakeEntityRegistry() {}

    public static void register(WrapperEntity wrapperEntity, int durationTicks) {
        BY_ID.put(wrapperEntity.getEntityId(), wrapperEntity);
        BY_UUID.put(wrapperEntity.getUuid(), wrapperEntity);
        if (durationTicks > 0) {
            SchedulerAdapter.get().runLater(() -> remove(wrapperEntity.getEntityId()), durationTicks);
        }
    }

    public static void remove(int entityId) {
        WrapperEntity wrapperEntity = BY_ID.remove(entityId);
        if (wrapperEntity != null) {
            BY_UUID.remove(wrapperEntity.getUuid());
            wrapperEntity.despawn();
        }
    }

    public static WrapperEntity getById(int entityId) {
        return BY_ID.get(entityId);
    }

    public static WrapperEntity getByUuid(UUID uuid) {
        return BY_UUID.get(uuid);
    }

    public static Collection<WrapperEntity> all() {
        return BY_ID.values();
    }

    /**
     * Returns every registered fake entity that the given player can currently see.
     */
    public static List<WrapperEntity> visibleTo(UUID viewer) {
        List<WrapperEntity> visible = new ArrayList<>();
        for (WrapperEntity wrapperEntity : BY_ID.values()) {
            if (wrapperEntity.getViewers().contains(viewer)) visible.add(wrapperEntity);
        }
        return visible;
    }
}

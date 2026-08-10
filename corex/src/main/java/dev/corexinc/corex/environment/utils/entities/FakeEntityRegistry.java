package dev.corexinc.corex.environment.utils.entities;

import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeEntityRegistry {

    private static final Map<Integer, WrapperEntity> ACTIVE = new ConcurrentHashMap<>();

    private FakeEntityRegistry() {}

    public static void register(WrapperEntity wrapperEntity, int durationTicks) {
        ACTIVE.put(wrapperEntity.getEntityId(), wrapperEntity);
        if (durationTicks > 0) {
            SchedulerAdapter.get().runLater(() -> remove(wrapperEntity.getEntityId()), durationTicks);
        }
    }

    public static void remove(int entityId) {
        WrapperEntity wrapperEntity = ACTIVE.remove(entityId);
        if (wrapperEntity != null) {
            wrapperEntity.despawn();
        }
    }

    public static WrapperEntity getById(int entityId) {
        return ACTIVE.get(entityId);
    }
}
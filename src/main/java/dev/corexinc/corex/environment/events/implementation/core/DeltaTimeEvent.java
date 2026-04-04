package dev.corexinc.corex.environment.events.implementation.core;

import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DeltaTimeEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private long secondsPassed = 0;

    private int currentSessionId = 0;

    private final Set<EventData> secondly = new LinkedHashSet<>();
    private final Set<EventData> minutely = new LinkedHashSet<>();
    private final Set<EventData> hourly   = new LinkedHashSet<>();

    @Override public @NonNull String getName() { return "DeltaTime"; }
    @Override public @NonNull String getSyntax() { return "delta time"; }

    @Override
    public void addScript(@NonNull EventData data) {
        String raw = data.rawLine;
        if (raw.contains("secondly")) secondly.add(data);
        else if (raw.contains("minutely")) minutely.add(data);
        else if (raw.contains("hourly")) hourly.add(data);
    }

    @Override
    public void initListener() {
        if (isRegistered) return;
        if (secondly.isEmpty() && minutely.isEmpty() && hourly.isEmpty()) return;

        isRegistered = true;

        currentSessionId++;
        final int mySession = currentSessionId;

        SchedulerAdapter.runRepeating(() -> {
            if (!isRegistered || mySession != currentSessionId) return;

            tick();
        }, 20L, 20L);
    }

    @Override
    public void reset() {
        isRegistered = false;
        currentSessionId++;
        secondly.clear();
        minutely.clear();
        hourly.clear();
    }

    private void tick() {
        secondsPassed++;

        ContextTag context = new ContextTag();
        context.put("second", new ElementTag(secondsPassed));

        checkAndFire(secondly, secondsPassed, context);
        if (secondsPassed % 60 == 0) checkAndFire(minutely, secondsPassed / 60, context);
        if (secondsPassed % 3600 == 0) checkAndFire(hourly, secondsPassed / 3600, context);
    }

    private void checkAndFire(Set<EventData> scripts, long timeValue, ContextTag context) {
        for (EventData data : scripts) {
            int every = 1;
            String switchVal = data.getSwitch("every");
            if (switchVal != null) {
                try { every = Integer.parseInt(switchVal); } catch (Exception ignored) {}
            }

            if (timeValue % every == 0) {
                EventRegistry.fire(data, null, context);
            }
        }
    }
}
package dev.corexinc.corex.environment.utils.nms;

import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.utils.ServerVersion;

import java.util.*;

public class NMSRegistry {

    private final Map<Class<?>, List<AdapterEntry>> adapters = new HashMap<>();
    private final Map<Class<?>, Object> activeInstances = new HashMap<>();
    private final ServerVersion currentVersion;

    public NMSRegistry() {
        this.currentVersion = ServerVersion.getCurrent();
        CorexLogger.info("Detected server version: <aqua>" + currentVersion + "</aqua>");
    }

    public <T> void register(Class<T> interfaceClass, String version, String implClassName) {
        adapters.computeIfAbsent(interfaceClass, k -> new ArrayList<>())
                .add(new AdapterEntry(ServerVersion.parse(version), implClassName, null));
    }

    public <T> void register(Class<T> interfaceClass, String version, Class<? extends T> implClass) {
        adapters.computeIfAbsent(interfaceClass, k -> new ArrayList<>())
                .add(new AdapterEntry(ServerVersion.parse(version), implClass.getName(), implClass));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> interfaceClass) {
        if (activeInstances.containsKey(interfaceClass)) {
            return (T) activeInstances.get(interfaceClass);
        }

        List<AdapterEntry> entries = adapters.getOrDefault(interfaceClass, Collections.emptyList());
        if (entries.isEmpty()) {
            CorexLogger.warn("No NMS adapters registered for " + interfaceClass.getSimpleName());
            return null;
        }

        entries.sort((a, b) -> b.version().compareTo(a.version()));

        AdapterEntry bestMatch = null;
        for (AdapterEntry entry : entries) {
            if (entry.version().compareTo(currentVersion) <= 0) {
                bestMatch = entry;
                break;
            }
        }

        if (bestMatch == null) {
            bestMatch = entries.getLast();
        }

        try {
            Class<?> clazz = bestMatch.implClass() != null
                    ? bestMatch.implClass()
                    : Class.forName(bestMatch.implClassName());

            T instance = (T) clazz.getDeclaredConstructor().newInstance();
            activeInstances.put(interfaceClass, instance);

            return instance;
        } catch (Exception e) {
            CorexLogger.error("Failed to load NMS adapter " + bestMatch.implClassName() + ": " + e.getMessage());
            return null;
        }
    }

    private record AdapterEntry(ServerVersion version, String implClassName, Class<?> implClass) {}
}
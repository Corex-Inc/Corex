package dev.corexinc.corex.environment.containers.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ArgumentTypeRegistry {

    public record Entry(
            @NonNull Function<CommandArgumentSpec, ArgumentType<?>> nmsFactory,
            @NonNull ArgumentTypeAdapter adapter
    ) {}

    private static final Map<String, Entry> REGISTRY = new ConcurrentHashMap<>();

    private ArgumentTypeRegistry() {}

    public static void register(
            @NonNull String name,
            @NonNull Function<CommandArgumentSpec, ArgumentType<?>> nmsFactory,
            @NonNull ArgumentTypeAdapter adapter) {
        REGISTRY.put(name, new Entry(nmsFactory, adapter));
    }

    public static @Nullable Entry get(@NonNull String name) {
        return REGISTRY.get(name);
    }

    public static @NonNull Map<String, Entry> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
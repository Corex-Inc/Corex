package dev.corexinc.corex.environment.containers.commands;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record CommandArgumentSpec(
        @NonNull  String name,
        @NonNull  String typeName,
        boolean optional,
        @NonNull  Map<String, Object> options
) {

    public int intOption(@NonNull String key, int defaultValue) {
        Object raw = options.get(key);
        if (raw instanceof Number number) return number.intValue();
        if (raw instanceof String string) {
            try { return Integer.parseInt(string); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public float floatOption(@NonNull String key, float defaultValue) {
        Object raw = options.get(key);
        if (raw instanceof Number number) return number.floatValue();
        if (raw instanceof String string) {
            try { return Float.parseFloat(string); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public double doubleOption(@NonNull String key, double defaultValue) {
        Object raw = options.get(key);
        if (raw instanceof Number number) return number.doubleValue();
        if (raw instanceof String string) {
            try { return Double.parseDouble(string); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public long longOption(@NonNull String key, long defaultValue) {
        Object raw = options.get(key);
        if (raw instanceof Number number) return number.longValue();
        if (raw instanceof String string) {
            try { return Long.parseLong(string); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public @Nullable String stringOption(@NonNull String key) {
        Object raw = options.get(key);
        return raw != null ? raw.toString() : null;
    }
}
package dev.corexinc.corex.engine.utils;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record Position(UUID world, double x, double y, double z) {

    public static Position of(double x, double y, double z) {
        return new Position(null, x, y, z);
    }

    public static Position of(UUID world, double x, double y, double z) {
        return new Position(world, x, y, z);
    }

    @Override
    public @NotNull String toString() {
        return "Position{world=" + world + ", x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
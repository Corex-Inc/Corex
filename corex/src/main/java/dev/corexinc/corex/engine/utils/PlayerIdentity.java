package dev.corexinc.corex.engine.utils;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerIdentity {

    UUID getUniqueId();

    @Nullable
    String getName();

    boolean isOnline();

    default String identify() {
        return "p@" + getUniqueId().toString();
    }
}
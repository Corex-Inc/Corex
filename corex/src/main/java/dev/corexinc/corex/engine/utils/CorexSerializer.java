package dev.corexinc.corex.engine.utils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CorexSerializer {
    
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
}
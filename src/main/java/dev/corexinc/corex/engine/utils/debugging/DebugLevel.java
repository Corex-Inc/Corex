package dev.corexinc.corex.engine.utils.debugging;

public enum DebugLevel {
    NONE,       // No output at all
    ERROR,      // Only errors
    INFO,       // Queue start/stop + errors
    VERBOSE,    // + tag fills + skipped commands
    TRACE       // + timing per instruction
}
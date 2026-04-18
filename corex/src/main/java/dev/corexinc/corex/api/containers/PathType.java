package dev.corexinc.corex.api.containers;

import dev.corexinc.corex.engine.compiler.Instruction;
import org.jetbrains.annotations.ApiStatus.AvailableSince;

/**
 * Categorizes how the Corex Engine should handle a specific YAML path during container loading.
 */
@AvailableSince("1.0.0")
public enum PathType {

    /**
     * Indicates that the path contains a list of commands that must be compiled into {@link Instruction}s.
     */
    @AvailableSince("1.0.0")
    SCRIPT,

    /**
     * Indicates that the path contains raw data (strings, numbers, lists) and should not be processed by the Compiler.
     */
    @AvailableSince("1.0.0")
    DATA,

    /**
     * Indicates that the path contains metadata (like 'type' or 'definitions') and should be ignored by the script processing logic.
     */
    @AvailableSince("1.0.0")
    IGNORE
}
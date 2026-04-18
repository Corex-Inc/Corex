package dev.corexinc.corex.api.data.actions;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface AbstractDataAction {

    // Symbol used for O(1) registry lookup.
    // Return "" only for the fallback AssignAction.
    @NonNull String getSymbol();

    // Return true if the registry matches via startsWith instead of equals.
    // Example: "+:" matches "+:10", "+:5", etc.
    default boolean isPrefix() { return false; }

    @Nullable AbstractTag apply(
            @Nullable AbstractTag current,
            @NonNull String param,
            @Nullable AbstractTag secondArg,
            @NonNull ScriptQueue queue
    );

    // Strips the symbol from the raw action string to expose the param.
    // Example: action="+:10", symbol="+:" → "10"
    // Example: action="hello", symbol="" → "hello" (AssignAction fallback)
    default @NonNull String extractParam(@NonNull String action) {
        String symbol = getSymbol();
        return symbol.isEmpty() ? action : action.substring(symbol.length());
    }
}
package dev.corexinc.corex.api.data.actions;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a data mutation action within the Corex scripting engine.
 * <p>
 * Data actions define the operators used primarily by the {@code def} command
 * to manipulate and modify variable values (e.g., {@code +:}, {@code -:}, {@code |:}).
 */
public interface AbstractDataAction {

    /**
     * Gets the symbol representing this data action.
     * <p>
     * This symbol is used for O(1) registry lookup during script compilation and execution.
     * Return {@code ""} (an empty string) <b>only</b> for the fallback AssignAction.
     *
     * @return a non-null string containing the operator symbol (e.g., "+:").
     */
    @NonNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getSymbol();

    /**
     * Determines whether this action is matched as a prefix.
     * <p>
     * If {@code true}, the registry matches this action using {@link String#startsWith(String)}
     * instead of a strict exact match.
     * <p>
     * <b>Example:</b> If the symbol is {@code "+:"}, it will successfully match an input like {@code "+:10"}.
     *
     * @return {@code true} if the symbol acts as a prefix, {@code false} for an exact match.
     */
    @AvailableSince("1.0.0")
    default boolean isPrefix() {
        return false;
    }

    /**
     * Applies this data action to modify or compute a target value.
     *
     * @param current   the existing definition value, or {@code null} if it is not currently set.
     * @param param     the raw string parameter provided after the symbol (e.g., {@code "10"} from {@code "+:10"}).
     * @param secondArg an optional secondary {@link AbstractTag} argument provided to the command, or {@code null}.
     * @param queue     the current {@link ScriptQueue} execution context.
     * @return the resulting {@link AbstractTag} after the operation is applied, or {@code null} to delete/unset the definition.
     */
    @Nullable
    @OverrideOnly
    @AvailableSince("1.0.0")
    AbstractTag apply(
            @Nullable AbstractTag current,
            @NonNull String param,
            @Nullable AbstractTag secondArg,
            @NonNull ScriptQueue queue
    );

    /**
     * Strips the action's symbol from the raw input string to extract the parameter.
     * <p>
     * <b>Examples:</b>
     * <ul>
     *   <li>{@code action="+:10"}, {@code symbol="+:"} &rarr; returns {@code "10"}</li>
     *   <li>{@code action="hello"}, {@code symbol=""} &rarr; returns {@code "hello"} (AssignAction fallback)</li>
     * </ul>
     * <p>
     * <b>Note:</b> Only override this method if your action requires non-standard prefix stripping logic.
     *
     * @param action the raw action string containing both the symbol and the parameter.
     * @return a non-null string containing just the parameter portion.
     */
    @NonNull
    @AvailableSince("1.0.0")
    default String extractParam(@NonNull String action) {
        String symbol = getSymbol();
        return symbol.isEmpty() ? action : action.substring(symbol.length());
    }
}
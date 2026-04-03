package dev.corexinc.corex.environment.events;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a modular script event handler within the Corex Environment.
 *
 * <p>Implementations of this interface act as bridges between Bukkit/Folia events
 * and the Corex Script Engine. They are responsible for pattern matching and
 * context population.</p>
 *
 * <h2>Life Cycle</h2>
 * <ol>
 *     <li><b>Registration:</b> Classes are registered via {@link EventRegistry#register(Class[])}.</li>
 *     <li><b>Script Mapping:</b> When a script is loaded, {@link #addScript(EventData)} is called if
 *     the syntax matches.</li>
 *     <li><b>Lazy Initialization:</b> {@link #initListener()} is called to register the actual
 *     Bukkit {@link Listener} only when needed.</li>
 *     <li><b>Execution:</b> On actual event trigger, implementation calls {@link EventRegistry#fire}.</li>
 *     <li><b>Cleanup:</b> {@link #reset()} is called during {@code /ex reload} to clear mapped scripts.</li>
 * </ol>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public interface AbstractEvent extends Listener {

    /**
     * Gets the unique internal name of this script event.
     * <p>Used primarily for debugging and logging purposes.</p>
     *
     * @return the unique event name (e.g., "PlayerBreaksBlock").
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getName();

    /**
     * Returns the syntax pattern used to match this event in scripts.
     * <p>The first static part of the syntax is used by the {@link EventRegistry}
     * for high-speed indexing.</p>
     *
     * <p><b>Example:</b> {@code "player breaks <block>"}</p>
     *
     * @return the event syntax string.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getSyntax();

    /**
     * Assigns a compiled script block to this event instance.
     * <p>The implementation should store this data (e.g., in a Map or List)
     * for rapid execution when the event triggers.</p>
     *
     * @param data the compiled script data and its associated switches.
     */
    @Internal
    @OverrideOnly
    @AvailableSince("1.0.0")
    void addScript(@NotNull EventData data);

    /**
     * Initializes the underlying Bukkit listener.
     * <p>This method implements <b>Lazy Registration</b>: it should only register
     * the event in the Plugin Manager if at least one script is currently
     * assigned to this event.</p>
     */
    @Internal
    @AvailableSince("1.0.0")
    void initListener();

    /**
     * Clears all assigned scripts and resets the internal state.
     * <p>Called automatically by the {@link EventRegistry} when scripts are reloaded.</p>
     */
    @Internal
    @AvailableSince("1.0.0")
    void reset();

    default boolean isCancelled(ScriptQueue queue) {
        for (AbstractTag tag : queue.getReturns()) {
            if (tag.identify().equalsIgnoreCase("cancelled")) {
                return true;
            }
        }
        return false;
    }
}
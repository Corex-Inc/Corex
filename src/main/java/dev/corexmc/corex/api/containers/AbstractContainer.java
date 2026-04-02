package dev.corexmc.corex.api.containers;

import dev.corexmc.corex.engine.compiler.Instruction;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a high-level script container defined in a {@code .crx} file.
 *
 * <p>Each implementation corresponds to a specific script type (e.g., {@code task}, {@code events}).
 * The container is responsible for defining which parts of its YAML structure should be compiled
 * into executable instructions and which should remain as raw data.</p>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class MyContainer implements AbstractContainer {
 *     @Override
 *     public String getType() { return "my_type"; }
 *
 *     @Override
 *     public PathType resolvePath(String path) {
 *         if (path.startsWith("script")) return PathType.SCRIPT;
 *         return PathType.DATA;
 *     }
 *     // ... other methods
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public interface AbstractContainer {

    /**
     * Gets the unique identifier for this container type.
     * <p>This value is used in YAML to identify the script type (e.g., {@code type: task}).</p>
     *
     * @return the container type string in lowercase.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getType();

    /**
     * Gets the specific name of this script instance.
     *
     * @return the unique name of the script.
     */
    @NotNull
    @AvailableSince("1.0.0")
    String getName();

    /**
     * Initializes the container with raw data from the YAML configuration.
     * <p><b>Warning:</b> This method is called internally by the Script Manager during load.</p>
     *
     * @param name    the name of the script.
     * @param section the raw configuration data.
     */
    @Internal
    @AvailableSince("1.0.0")
    void init(@NotNull String name, @NotNull ConfigurationSection section);

    /**
     * Returns the raw {@link ConfigurationSection} associated with this container.
     * Useful for retrieving non-script data such as custom keys or metadata.
     *
     * @return the raw YAML section.
     */
    @NotNull
    @AvailableSince("1.0.0")
    ConfigurationSection getData();

    /**
     * Determines how the engine should process a specific YAML path within this container.
     *
     * @param path the full path to the key (e.g., "script.on_fail").
     * @return the {@link PathType} classification for this path.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    PathType resolvePath(@NotNull String path);

    /**
     * Stores a compiled block of instructions for a specific path.
     * <p>This is called by the Compiler after successful processing of a {@link PathType#SCRIPT} path.</p>
     *
     * @param path     the path where the script is located.
     * @param bytecode the array of compiled instructions.
     */
    @Internal
    @AvailableSince("1.0.0")
    void addCompiledScript(@NotNull String path, @NotNull Instruction[] bytecode);

    /**
     * Retrieves the compiled instructions for the given path.
     *
     * @param path the script path.
     * @return the bytecode array, or {@code null} if no script exists at this path.
     */
    @Nullable
    @AvailableSince("1.0.0")
    Instruction[] getScript(@NotNull String path);

    /**
     * Returns a list of expected input variables (definitions) for this script.
     *
     * @return a list of definition keys, or an empty list if none are required.
     */
    @NotNull
    @AvailableSince("1.0.0")
    default List<String> getDefinitions() {
        return Collections.emptyList();
    }
}
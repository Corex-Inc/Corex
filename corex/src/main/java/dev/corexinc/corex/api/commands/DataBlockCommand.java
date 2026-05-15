package dev.corexinc.corex.api.commands;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.scripts.ScriptManager;

/**
 * Marker interface for commands that accept a YAML data block (Map or List)
 * as their inline body instead of a compiled instruction block.
 *
 * <p>When {@link ScriptManager#compileBlock} encounters
 * a YAML block value for a command implementing this interface, it stores the raw
 * YAML data ({@code Map<?,?>} or {@code List<?>}) directly in
 * {@link Instruction#customData} instead of
 * trying to recursively compile the block as nested script instructions.
 *
 * <p>This allows commands to receive structured data inline in a natural YAML style:
 * <pre>{@code
 * // Map block:
 * - def myMap:
 *     key1: value1
 *     key2: value2
 *
 * // List block:
 * - def myList:
 *     - element1
 *     - element2
 * }</pre>
 *
 * @since 1.0.0
 */
public interface DataBlockCommand {}
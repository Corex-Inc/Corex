package dev.corexinc.corex.api.scripts;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Lets an addon change scripts on their way through the compiler.
 *
 * <p>Register it like any other component, and Corex works out which stages it wants from the
 * methods it overrides:</p>
 *
 * <pre>{@code
 * CorexRegistrar.open(this)
 *         .register(MyPreprocessor.class)
 *         .close();
 * }</pre>
 *
 * <p>Every method here has a default that does nothing and returns {@code null}, and {@code null}
 * means "I did not change anything". That is not only a convenience: a stage nobody changed does
 * not invalidate what came after it, and Corex skips the work of rebuilding it.</p>
 *
 * <p>Pick the latest stage that can do the job. {@link #parsedYaml} sees a real tree with strings
 * already separated from structure, so a pass there cannot accidentally eat a comment or split a
 * quoted string, while {@link #rawScript} hands over the file as text and trusts the addon to
 * behave. If the goal is new syntax for a single line, {@link #unknownLine} is cheaper and safer
 * than all of them.</p>
 *
 * <pre>{@code
 * public class UsingDirective implements AbstractPreprocessor {
 *
 *     @Override
 *     public String getName() {
 *         return "using";
 *     }
 *
 *     @Override
 *     public Map<String, Object> parsedYaml(ScriptSource source, Map<String, Object> tree) {
 *         if (!tree.containsKey("using")) return null;
 *
 *         Map<String, Object> copy = new LinkedHashMap<>(tree);
 *         copy.remove("using");
 *         return copy;
 *     }
 * }
 * }</pre>
 *
 * <p>Order between stages is fixed by the pipeline. Order inside one stage comes from
 * {@link #getPriority()}, highest first, and ties are broken by the addon name so that two servers
 * with the same addons always compile the same way.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public interface AbstractPreprocessor {

    /**
     * The name this pass is logged under. Keep it short, it appears in the startup chain and in
     * any error a script picks up after this pass touched it.
     *
     * @return the name.
     */
    @NotNull
    String getName();

    /**
     * Where this pass sits among the others on the same stage. Higher runs first; the default is
     * {@code 0}. Passes with equal priority run in addon-name order.
     *
     * @return the priority.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Rewrites the file as text, before Corex has looked at it at all.
     *
     * <p>Comments are still present here, which is the one thing later stages cannot offer, and
     * the reason to reach for this stage. Everything else about it is a liability: indentation,
     * quoting and line folding are all still the author's problem, and a mistake shows up as a
     * syntax error somewhere further down.</p>
     *
     * @param source the file being loaded.
     * @param lines  its current lines.
     * @return the replacement lines, or {@code null} to leave them alone.
     */
    @Nullable
    default List<String> rawScript(@NotNull ScriptSource source, @NotNull List<String> lines) {
        return null;
    }

    /**
     * Reads the comments Corex is about to discard.
     *
     * <p>Read-only on purpose: a directive in a comment is metadata, so note what it said here and
     * act on it at {@link #parsedYaml}, where changing the script cannot damage anything else. This
     * is the reason to not reach for {@link #rawScript} — it is the only thing raw text offered
     * that no other stage could.</p>
     *
     * <pre>{@code
     * @Override
     * public void comments(ScriptSource source, List<ScriptComment> comments) {
     *     for (ScriptComment comment : comments) {
     *         String module = comment.directive("using");
     *         if (module != null) required.add(module);
     *     }
     * }
     * }</pre>
     *
     * @param source   the file being loaded.
     * @param comments its comments, in file order.
     */
    default void comments(@NotNull ScriptSource source, @NotNull List<ScriptComment> comments) {
    }

    /**
     * Rewrites the normalized YAML, after comments are gone and command lines are quoted.
     *
     * <p>The result has to stay parseable; if it does not, the failure names this pass. Note that
     * {@code #} is not a comment character here, see {@link PreprocessStage#RAW_YAML}.</p>
     *
     * @param source the file being loaded.
     * @param yaml   its current YAML text.
     * @return the replacement YAML, or {@code null} to leave it alone.
     */
    @Nullable
    default String rawYaml(@NotNull ScriptSource source, @NotNull String yaml) {
        return null;
    }

    /**
     * Rewrites the parsed tree of one file.
     *
     * <p>The usual place to add syntax: a key Corex does not know is just an entry in this map, and
     * removing or rewriting it cannot disturb anything else in the file.</p>
     *
     * @param source the file being loaded.
     * @param tree   its current tree.
     * @return the replacement tree, or {@code null} to leave it alone.
     */
    @Nullable
    default Map<String, Object> parsedYaml(@NotNull ScriptSource source, @NotNull Map<String, Object> tree) {
        return null;
    }

    /**
     * Runs once when every file has been parsed and nothing has been compiled yet.
     *
     * <p>This is the only view of the whole scriptpack, so it is where a pass belongs that needs
     * one file to know about another. Edit a file through {@link ScriptSource#setTree}.</p>
     *
     * @param sources every script file that loaded, in file order.
     */
    default void allParsed(@NotNull List<ScriptSource> sources) {
    }

    /**
     * Rewrites one script body, line by line, just before it is compiled.
     *
     * <p>Where line-level syntax belongs. An annotation is its own entry in this list, so a pass
     * reads it, drops it, and changes the entry after it; a macro entry expands into several; a
     * line meant for the addon alone is swallowed and never reaches the compiler.</p>
     *
     * <pre>{@code
     * @Override
     * public List<Object> scriptBlock(ScriptSource source, String script, String path,
     *                                 List<Object> lines) {
     *     List<Object> out = new ArrayList<>(lines.size());
     *     boolean quiet = false;
     *
     *     for (Object line : lines) {
     *         if ("@NoDebug".equals(line)) {
     *             quiet = true;
     *             continue;
     *         }
     *         out.add(quiet && line instanceof String text ? text + " --silent" : line);
     *         quiet = false;
     *     }
     *     return out;
     * }
     * }</pre>
     *
     * <p>An annotation has to be its own list entry ({@code - @NoDebug}). A bare line with no
     * dash is a continuation line in Corex and gets folded onto the end of the line above it
     * while the file is normalized, long before this stage.</p>
     *
     * @param source the file being loaded.
     * @param script the container name, e.g. {@code my_task}.
     * @param path   the path inside it, e.g. {@code script} or {@code events.on_click}.
     * @param lines  the entries: strings for commands, maps for nested blocks.
     * @return the replacement entries, or {@code null} to leave them alone.
     */
    @Nullable
    default List<Object> scriptBlock(@NotNull ScriptSource source, @NotNull String script,
                                     @NotNull String path, @NotNull List<Object> lines) {
        return null;
    }

    /**
     * Claims a line whose first word is not a known command.
     *
     * <p>Corex asks each pass in turn just before it would report the line as unknown, and compiles
     * the first replacement it is given as though the author had written it. So an addon can invent
     * a line without ever touching the file: rewrite it into commands that already exist, or into
     * one the addon registered itself.</p>
     *
     * <pre>{@code
     * @Override
     * public String unknownLine(String line) {
     *     if (!line.startsWith("goto ")) return null;
     *     return "run " + line.substring(5).trim();
     * }
     * }</pre>
     *
     * <p>The replacement is compiled once and is not offered back to this hook, so a pass cannot
     * loop by returning something equally unknown.</p>
     *
     * @param line the line Corex could not compile, trimmed.
     * @return the line to compile instead, or {@code null} to pass.
     */
    @Nullable
    default String unknownLine(@NotNull String line) {
        return null;
    }
}

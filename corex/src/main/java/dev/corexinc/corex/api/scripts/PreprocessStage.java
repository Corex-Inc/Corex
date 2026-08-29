package dev.corexinc.corex.api.scripts;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * A point in script loading where addons get to interfere.
 *
 * <p>The first three are the artifacts the compiler actually materializes, one per step, and a
 * preprocessor sees whichever ones it asks for. They are listed here in the order they happen, so
 * a pass on an earlier stage always runs before a pass on a later one no matter who registered
 * first.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public enum PreprocessStage {

    /**
     * The file exactly as it sits on disk, line by line. Comments are still here, indentation is
     * still the author's, nothing has been normalized. Anything is possible at this stage and
     * nothing is verified, which is why an error later in a file touched here is reported with
     * the name of whoever touched it.
     */
    RAW_SCRIPT("RAW_SCRIPT"),

    /**
     * Every comment Corex found while normalizing the file, handed over read-only just before it
     * discards them. This is where a directive written as a comment is read, without taking on the
     * whole file as text the way {@link #RAW_SCRIPT} makes you.
     */
    COMMENTS("COMMENTS"),

    /**
     * The same file after Corex normalized it into valid YAML: comments stripped, folded lines
     * joined, command lines quoted. A pass here must hand back something YAML can still parse.
     *
     * <p>{@code #} does not mean a comment at this stage. Corex escapes it before normalizing so
     * that a hash inside a script survives, and re-escapes whatever a pass returns, so writing a
     * YAML comment here does nothing.</p>
     */
    RAW_YAML("RAW_YAML"),

    /**
     * The parsed tree of one file: the map YAML produced, keys to strings, lists and nested maps.
     * This is where a new top-level key like {@code import:} is handled, without any text
     * wrangling.
     */
    PARSED_YAML("PARSED_YAML"),

    /**
     * Every file has been read and parsed, and nothing has been compiled yet. The only stage that
     * sees the whole scriptpack at once, so it is where cross-file work belongs.
     */
    ALL_PARSED("ALL_PARSED"),

    /**
     * One script body, as the list of raw lines Corex is about to compile, after its container has
     * decided that this path holds a script at all.
     *
     * <p>The stage for anything that works line by line: an annotation on its own entry that
     * modifies the next one, a macro that expands into several commands, a line the addon wants to
     * swallow entirely. Entries are the strings the author wrote, plus maps for the nested blocks
     * under {@code if:} and friends, and the list may be returned longer, shorter or reordered.</p>
     *
     * <p>Fires once per script path, not for the nested blocks inside it; those arrive as maps in
     * the same list and can be walked from there. Containers that compile their own blocks during
     * {@code init} rather than through a script path, as dialog buttons do, are not covered here
     * and have to be handled at {@link #PARSED_YAML}.</p>
     */
    SCRIPT_BLOCK("SCRIPT_BLOCK"),

    /**
     * Not a pass over a file: fires for a single line whose first word is not a known command,
     * right before Corex would report it as unknown. A pass returning a replacement line gets it
     * compiled as if the author had written that instead.
     */
    UNKNOWN_LINE("UNKNOWN_LINE");

    private final String label;

    PreprocessStage(String label) {
        this.label = label;
    }

    /**
     * Returns the name used in logs.
     *
     * @return the stage label.
     */
    @NotNull
    public String getLabel() {
        return label;
    }
}

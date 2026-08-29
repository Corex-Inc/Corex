package dev.corexinc.corex.api.scripts;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One script file on its way through the compiler.
 *
 * <p>It carries every artifact produced so far, so a pass on a later stage can still look at what
 * the author actually wrote. The lines are what came off the disk plus whatever earlier passes did
 * to them; {@link #getOriginalLines()} is the untouched version and never changes.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class ScriptSource {

    private final Path path;
    private final List<String> originalLines;
    private final Map<PreprocessStage, List<String>> touchedBy = new EnumMap<>(PreprocessStage.class);

    private List<String> lines;
    private String yaml;
    private Map<String, Object> tree;
    private List<ScriptComment> comments = List.of();

    /**
     * @param path  the file this came from.
     * @param lines its contents, as read.
     */
    @Internal
    public ScriptSource(@NotNull Path path, @NotNull List<String> lines) {
        this.path = path;
        this.originalLines = List.copyOf(lines);
        this.lines = lines;
    }

    /**
     * The file this script was read from.
     *
     * @return the path.
     */
    @NotNull
    public Path getPath() {
        return path;
    }

    /**
     * The file name, for messages.
     *
     * @return the name, e.g. {@code quests.cx}.
     */
    @NotNull
    public String getFileName() {
        return path.getFileName().toString();
    }

    /**
     * The file as it was read from disk, before any pass touched it.
     *
     * @return the original lines, immutable.
     */
    @NotNull
    public List<String> getOriginalLines() {
        return originalLines;
    }

    /**
     * The lines as they stand now.
     *
     * @return the current lines.
     */
    @NotNull
    public List<String> getLines() {
        return lines;
    }

    /**
     * The comments that were stripped out of this file.
     *
     * <p>Only collected when some addon asks for {@link PreprocessStage#COMMENTS}; empty otherwise,
     * because finding them costs nothing but keeping them does.</p>
     *
     * @return the comments, in file order.
     */
    @NotNull
    public List<ScriptComment> getComments() {
        return comments;
    }

    /**
     * The normalized YAML, once {@link PreprocessStage#RAW_YAML} has been reached.
     *
     * @return the YAML text, or {@code null} earlier than that.
     */
    @Nullable
    public String getYaml() {
        return yaml;
    }

    /**
     * The parsed tree, once {@link PreprocessStage#PARSED_YAML} has been reached.
     *
     * @return the tree, or {@code null} earlier than that.
     */
    @Nullable
    public Map<String, Object> getTree() {
        return tree;
    }

    /**
     * Replaces the parsed tree.
     *
     * <p>Returning a tree from {@code parsedYaml} does this already; this is for
     * {@link PreprocessStage#ALL_PARSED}, where a pass edits several files at once and has nothing
     * to return.</p>
     *
     * @param tree the new tree.
     */
    public void setTree(@NotNull Map<String, Object> tree) {
        this.tree = tree;
    }

    /**
     * Names the passes that changed this file at a stage, in the order they ran.
     *
     * @param stage the stage to ask about.
     * @return the names, empty when nothing changed the file there.
     */
    @NotNull
    public List<String> getTouchedBy(@NotNull PreprocessStage stage) {
        return touchedBy.getOrDefault(stage, List.of());
    }

    /**
     * Replaces the lines after a text pass rewrote them.
     *
     * @param lines the new lines.
     */
    @Internal
    public void setLines(@NotNull List<String> lines) {
        this.lines = lines;
    }

    /**
     * Stores the normalized YAML once it exists.
     *
     * @param yaml the YAML text.
     */
    @Internal
    public void setYaml(@NotNull String yaml) {
        this.yaml = yaml;
    }

    /**
     * Stores the comments stripped during normalization.
     *
     * @param comments the comments, in file order.
     */
    @Internal
    public void setComments(@NotNull List<ScriptComment> comments) {
        this.comments = comments;
    }

    /**
     * Records that a pass changed this file at a stage, for the message shown when the file
     * later fails to parse or compile.
     *
     * @param stage        the stage it changed.
     * @param preprocessor the pass name.
     */
    @Internal
    public void markTouched(@NotNull PreprocessStage stage, @NotNull String preprocessor) {
        touchedBy.computeIfAbsent(stage, key -> new ArrayList<>()).add(preprocessor);
    }
}

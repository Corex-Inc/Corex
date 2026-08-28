package dev.corexinc.corex.engine.scripts;

import dev.corexinc.corex.api.scripts.AbstractPreprocessor;
import dev.corexinc.corex.api.scripts.PreprocessStage;
import dev.corexinc.corex.api.scripts.ScriptComment;
import dev.corexinc.corex.api.scripts.ScriptSource;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the addon passes that run while scripts compile, and runs them in a fixed order.
 *
 * <p>A pass declares nothing: which stages it wants is read off the methods it actually overrides,
 * so there is no second list to keep in sync with the code. Within a stage, passes run by priority
 * (highest first) and then by addon name, which keeps the order identical on every server rather
 * than dependent on which plugin happened to load first.</p>
 *
 * <p>A pass that throws is reported with the name of the addon that supplied it and then skipped.
 * One broken addon costs its own effect, not the whole scriptpack.</p>
 */
public final class PreprocessorRegistry {

    /**
     * One registered pass, with the addon behind it and the stages it turned out to want.
     *
     * @param preprocessor the pass itself.
     * @param owner        the addon that registered it.
     * @param stages       the stages whose methods it overrides.
     */
    public record Registration(@NotNull AbstractPreprocessor preprocessor,
                               @NotNull AddonOwner owner,
                               @NotNull Set<PreprocessStage> stages) {

        /**
         * Returns the pass name with its addon, for logs.
         *
         * @return e.g. {@code "using (FooScript)"}.
         */
        @NotNull
        public String describe() {
            return preprocessor.getName() + " (" + owner.name() + ")";
        }
    }

    private static final Comparator<Registration> ORDER =
            Comparator.comparingInt((Registration entry) -> entry.preprocessor().getPriority()).reversed()
                    .thenComparing(entry -> entry.owner().name())
                    .thenComparing(entry -> entry.preprocessor().getName());

    private final List<Registration> registrations = new ArrayList<>();
    private final Map<PreprocessStage, List<Registration>> byStage = new EnumMap<>(PreprocessStage.class);

    /**
     * Adds a pass and works out which stages it wants.
     *
     * @param preprocessor the pass.
     * @param owner        the addon registering it.
     */
    public void register(@NotNull AbstractPreprocessor preprocessor, @NotNull AddonOwner owner) {
        Set<PreprocessStage> stages = stagesOf(preprocessor);

        if (stages.isEmpty()) {
            CorexLogger.warn("Preprocessor '<yellow>" + preprocessor.getName() + "</yellow>' from "
                    + owner.label() + " overrides no stage method, so it will never run.");
            return;
        }

        Registration registration = new Registration(preprocessor, owner, stages);
        registrations.add(registration);

        for (PreprocessStage stage : stages) {
            List<Registration> chain = byStage.computeIfAbsent(stage, key -> new ArrayList<>());
            chain.add(registration);
            chain.sort(ORDER);
        }
    }

    /**
     * Whether anything at all is registered, so the loader can skip the machinery entirely.
     *
     * @return {@code true} when no addon preprocesses anything.
     */
    public boolean isEmpty() {
        return registrations.isEmpty();
    }

    /**
     * Every registered pass, in registration order.
     *
     * @return the registrations.
     */
    @NotNull
    public List<Registration> getRegistrations() {
        return registrations;
    }

    /**
     * Prints the resolved chain for each stage, so the order is visible before anything goes wrong.
     */
    public void reportChains() {
        if (registrations.isEmpty()) {
            return;
        }
        for (PreprocessStage stage : PreprocessStage.values()) {
            List<Registration> chain = byStage.get(stage);
            if (chain == null) continue;

            StringBuilder line = new StringBuilder("<gray>" + stage.getLabel() + " chain:</gray>");
            for (Registration entry : chain) {
                line.append(" <yellow>").append(entry.preprocessor().getName()).append("</yellow>")
                        .append("<dark_gray>(").append(entry.owner().name())
                        .append(", ").append(entry.preprocessor().getPriority()).append(")");
            }
            CorexLogger.info(line.toString());
        }
    }

    /**
     * Runs the text passes over a file.
     *
     * @param source the file being loaded.
     */
    public void runRawScript(@NotNull ScriptSource source) {
        for (Registration entry : chain(PreprocessStage.RAW_SCRIPT)) {
            List<String> result = call(entry, PreprocessStage.RAW_SCRIPT, source,
                    () -> entry.preprocessor().rawScript(source, source.getLines()));

            if (result == null || result.equals(source.getLines())) continue;
            source.setLines(result);
            source.markTouched(PreprocessStage.RAW_SCRIPT, entry.preprocessor().getName());
        }
    }

    /**
     * Whether any pass wants a stage, so the loader can skip producing what nobody reads.
     *
     * @param stage the stage to ask about.
     * @return {@code true} when at least one pass runs there.
     */
    public boolean hasStage(@NotNull PreprocessStage stage) {
        return !chain(stage).isEmpty();
    }

    /**
     * Hands the stripped comments to the passes that asked for them.
     *
     * @param source the file being loaded.
     */
    public void runComments(@NotNull ScriptSource source) {
        List<ScriptComment> comments = source.getComments();
        for (Registration entry : chain(PreprocessStage.COMMENTS)) {
            try {
                entry.preprocessor().comments(source, comments);
            } catch (Throwable failure) {
                report(entry, PreprocessStage.COMMENTS, source, failure);
            }
        }
    }

    /**
     * Runs the YAML passes over a file, keeping hashes escaped so a returned {@code #} does not
     * silently become a comment.
     *
     * @param source the file being loaded.
     */
    public void runRawYaml(@NotNull ScriptSource source) {
        for (Registration entry : chain(PreprocessStage.RAW_YAML)) {
            String current = source.getYaml();
            String result = call(entry, PreprocessStage.RAW_YAML, source,
                    () -> entry.preprocessor().rawYaml(source, current));

            if (result == null || result.equals(current)) continue;
            source.setYaml(ScriptNormalizer.escapeHashes(result));
            source.markTouched(PreprocessStage.RAW_YAML, entry.preprocessor().getName());
        }
    }

    /**
     * Runs the tree passes over a file.
     *
     * @param source the file being loaded.
     */
    public void runParsedYaml(@NotNull ScriptSource source) {
        for (Registration entry : chain(PreprocessStage.PARSED_YAML)) {
            Map<String, Object> current = source.getTree();
            if (current == null) return;

            Map<String, Object> result = call(entry, PreprocessStage.PARSED_YAML, source,
                    () -> entry.preprocessor().parsedYaml(source, current));

            if (result == null || result.equals(current)) continue;
            source.setTree(result);
            source.markTouched(PreprocessStage.PARSED_YAML, entry.preprocessor().getName());
        }
    }

    /**
     * Runs the whole-scriptpack passes, once every file has been parsed.
     *
     * @param sources every file that loaded.
     */
    public void runAllParsed(@NotNull List<ScriptSource> sources) {
        for (Registration entry : chain(PreprocessStage.ALL_PARSED)) {
            try {
                entry.preprocessor().allParsed(sources);
            } catch (Throwable failure) {
                report(entry, PreprocessStage.ALL_PARSED, null, failure);
            }
        }
    }

    /**
     * Runs the line passes over one script body.
     *
     * @param source the file being loaded.
     * @param script the container name.
     * @param path   the script path inside it.
     * @param lines  the raw entries about to be compiled.
     * @return the entries to compile, the same list when nothing changed it.
     */
    @NotNull
    public List<?> runScriptBlock(@NotNull ScriptSource source, @NotNull String script,
                                  @NotNull String path, @NotNull List<?> lines) {
        List<Registration> chain = chain(PreprocessStage.SCRIPT_BLOCK);
        if (chain.isEmpty()) {
            return lines;
        }

        List<Object> current = new ArrayList<>(lines);
        for (Registration entry : chain) {
            List<Object> snapshot = current;
            List<Object> result = call(entry, PreprocessStage.SCRIPT_BLOCK, source,
                    () -> entry.preprocessor().scriptBlock(source, script, path, snapshot));

            if (result == null || result.equals(current)) continue;
            current = result;
            source.markTouched(PreprocessStage.SCRIPT_BLOCK, entry.preprocessor().getName());
        }
        return current;
    }

    /**
     * Asks the passes whether any of them recognises a line Corex could not compile.
     *
     * @param line the unknown line, trimmed.
     * @return the line to compile instead, or {@code null} when nobody claims it.
     */
    @Nullable
    public String resolveUnknownLine(@NotNull String line) {
        for (Registration entry : chain(PreprocessStage.UNKNOWN_LINE)) {
            String result;
            try {
                result = entry.preprocessor().unknownLine(line);
            } catch (Throwable failure) {
                report(entry, PreprocessStage.UNKNOWN_LINE, null, failure);
                continue;
            }

            if (result == null || result.equals(line)) continue;
            if (Debugger.getMode() == Debugger.Mode.ALL) {
                CorexLogger.info("<dark_gray>" + entry.describe() + " rewrote <gray>"
                        + line.replace("<", "\\<") + "<dark_gray> into <gray>" + result.replace("<", "\\<"));
            }
            return result;
        }
        return null;
    }

    private List<Registration> chain(PreprocessStage stage) {
        return byStage.getOrDefault(stage, List.of());
    }

    private <T> T call(Registration entry, PreprocessStage stage, ScriptSource source, PassCall<T> pass) {
        try {
            return pass.run();
        } catch (Throwable failure) {
            report(entry, stage, source, failure);
            return null;
        }
    }

    private static void report(Registration entry, PreprocessStage stage, ScriptSource source, Throwable failure) {
        String where = source != null ? " on " + source.getFileName() : "";
        Debugger.error("Preprocessor " + entry.describe() + " failed at " + stage.getLabel()
                + where + ", skipping it.", failure);
    }

    private static Set<PreprocessStage> stagesOf(AbstractPreprocessor preprocessor) {
        Set<PreprocessStage> stages = EnumSet.noneOf(PreprocessStage.class);
        Class<?> type = preprocessor.getClass();

        if (overrides(type, "rawScript", ScriptSource.class, List.class)) stages.add(PreprocessStage.RAW_SCRIPT);
        if (overrides(type, "comments", ScriptSource.class, List.class)) stages.add(PreprocessStage.COMMENTS);
        if (overrides(type, "rawYaml", ScriptSource.class, String.class)) stages.add(PreprocessStage.RAW_YAML);
        if (overrides(type, "parsedYaml", ScriptSource.class, Map.class)) stages.add(PreprocessStage.PARSED_YAML);
        if (overrides(type, "allParsed", List.class)) stages.add(PreprocessStage.ALL_PARSED);
        if (overrides(type, "scriptBlock", ScriptSource.class, String.class, String.class, List.class))
            stages.add(PreprocessStage.SCRIPT_BLOCK);
        if (overrides(type, "unknownLine", String.class)) stages.add(PreprocessStage.UNKNOWN_LINE);

        return stages;
    }

    private static boolean overrides(Class<?> type, String name, Class<?>... parameters) {
        try {
            return !type.getMethod(name, parameters).isDefault();
        } catch (NoSuchMethodException impossible) {
            return false;
        }
    }

    @FunctionalInterface
    private interface PassCall<T> {
        T run();
    }
}

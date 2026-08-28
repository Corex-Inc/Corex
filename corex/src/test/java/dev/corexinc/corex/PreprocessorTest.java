package dev.corexinc.corex;

import dev.corexinc.corex.api.scripts.AbstractPreprocessor;
import dev.corexinc.corex.api.scripts.PreprocessStage;
import dev.corexinc.corex.api.scripts.ScriptSource;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.scripts.PreprocessorRegistry;
import dev.corexinc.corex.engine.scripts.ScriptNormalizer;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers what an addon can count on from a preprocessor: that the stages it wants are worked out
 * from the code rather than declared, that a chain runs in a stated order, that a broken pass costs
 * only itself, and that a rewritten line really does compile.
 */
public class PreprocessorTest {

    private static final AddonOwner ADDON = new AddonOwner("PrepAddon", "1.0", AddonOwner.Origin.ADDON);
    private static final AddonOwner OTHER = new AddonOwner("ZOtherAddon", "1.0", AddonOwner.Origin.ADDON);

    private static CorexRegistry registry;

    @BeforeAll
    public static void setup() {
        registry = CorexTestEnvironment.bootstrap();
    }

    @Test
    public void stagesComeFromTheOverriddenMethods() {
        PreprocessorRegistry preprocessors = new PreprocessorRegistry();
        preprocessors.register(new TreePass("tree", 0), ADDON);

        PreprocessorRegistry.Registration registration = preprocessors.getRegistrations().getFirst();
        assertEquals(java.util.Set.of(PreprocessStage.PARSED_YAML), registration.stages(),
                "a pass that only overrides parsedYaml must not be scheduled anywhere else");
    }

    @Test
    public void aPassOverridingNothingIsNotRegistered() {
        PreprocessorRegistry preprocessors = new PreprocessorRegistry();
        preprocessors.register(new InertPass(), ADDON);

        assertTrue(preprocessors.isEmpty(), "a pass with no stage would never run, so it is refused");
    }

    @Test
    public void higherPriorityRunsFirst() {
        PreprocessorRegistry preprocessors = new PreprocessorRegistry();
        preprocessors.register(new TreePass("low", 0), ADDON);
        preprocessors.register(new TreePass("high", 50), ADDON);

        ScriptSource source = source("a: 1");
        preprocessors.runParsedYaml(source);

        assertEquals(List.of("high", "low"), source.getTouchedBy(PreprocessStage.PARSED_YAML));
    }

    @Test
    public void equalPrioritiesFallBackToAddonName() {
        PreprocessorRegistry preprocessors = new PreprocessorRegistry();
        preprocessors.register(new TreePass("fromZ", 0), OTHER);
        preprocessors.register(new TreePass("fromP", 0), ADDON);

        ScriptSource source = source("a: 1");
        preprocessors.runParsedYaml(source);

        assertEquals(List.of("fromP", "fromZ"), source.getTouchedBy(PreprocessStage.PARSED_YAML),
                "order must not depend on which addon happened to register first");
    }

    @Test
    public void returningNullCountsAsNoChange() {
        PreprocessorRegistry preprocessors = new PreprocessorRegistry();
        preprocessors.register(new IdlePass(), ADDON);

        ScriptSource source = source("a: 1");
        Map<String, Object> before = source.getTree();
        preprocessors.runParsedYaml(source);

        assertSame(before, source.getTree(), "an unchanged stage must not be replaced");
        assertTrue(source.getTouchedBy(PreprocessStage.PARSED_YAML).isEmpty());
    }

    @Test
    public void aThrowingPassOnlyCostsItself() {
        PreprocessorRegistry preprocessors = new PreprocessorRegistry();
        preprocessors.register(new ExplodingPass(), ADDON);
        preprocessors.register(new TreePass("survivor", -10), ADDON);

        ScriptSource source = source("a: 1");
        preprocessors.runParsedYaml(source);

        assertEquals(List.of("survivor"), source.getTouchedBy(PreprocessStage.PARSED_YAML),
                "the pass after a crashing one still runs");
        assertFalse(source.getTouchedBy(PreprocessStage.PARSED_YAML).contains("boom"));
    }

    @Test
    public void aRewrittenLineCompilesAsTheRealCommand() {
        AddonOwner previous = AddonManager.enter(ADDON);
        try {
            registry.register(GreetPass.class);
        }
        finally {
            AddonManager.exit(previous);
        }

        Instruction instruction = ScriptCompiler.compile("unittestgreet world");

        assertNotNull(instruction, "the pass claimed the line, so it must compile");
        assertEquals("narrate", instruction.command.getName(),
                "the replacement is compiled as though the author had written it");
    }

    @Test
    public void aBareAnnotationLineIsFoldedIntoTheLineAbove() {
        String yaml = ScriptNormalizer.preprocess(List.of(
                "script:",
                "- narrate a",
                "@NoDebug",
                "- narrate b"));

        assertTrue(yaml.contains("narrate a@NoDebug"),
                "a line without a dash is a continuation line, so an annotation written that way "
                        + "is glued onto the command above it long before any stage sees it: " + yaml);
    }

    private static ScriptSource source(String yaml) {
        ScriptSource source = new ScriptSource(Path.of("test.cx"), List.of(yaml));
        source.setYaml(yaml);
        source.setTree(new LinkedHashMap<>(Map.of("a", 1)));
        return source;
    }

    /** Rewrites the tree so the run counts as a change, and records its name through that. */
    private record TreePass(String name, int priority) implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public @Nullable Map<String, Object> parsedYaml(@NotNull ScriptSource source,
                                                        @NotNull Map<String, Object> tree) {
            Map<String, Object> copy = new LinkedHashMap<>(tree);
            copy.put(name, true);
            return copy;
        }
    }

    /** Overrides a stage but never changes anything. */
    private static class IdlePass implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return "idle";
        }

        @Override
        public @Nullable Map<String, Object> parsedYaml(@NotNull ScriptSource source,
                                                        @NotNull Map<String, Object> tree) {
            return null;
        }
    }

    /** Overrides no stage at all. */
    private static class InertPass implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return "inert";
        }
    }

    /** Throws on every file. */
    private static class ExplodingPass implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return "boom";
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public @Nullable Map<String, Object> parsedYaml(@NotNull ScriptSource source,
                                                        @NotNull Map<String, Object> tree) {
            throw new IllegalStateException("this addon is broken");
        }
    }

    /** Invents a line that is not a command and hands back one that is. */
    public static class GreetPass implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return "greet";
        }

        @Override
        public @Nullable String unknownLine(@NotNull String line) {
            if (!line.startsWith("unittestgreet ")) return null;
            return "narrate Hello, " + line.substring("unittestgreet ".length()).trim() + "!";
        }
    }
}

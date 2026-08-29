package dev.corexinc.corex;

import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.scripts.AbstractPreprocessor;
import dev.corexinc.corex.api.scripts.ScriptComment;
import dev.corexinc.corex.api.scripts.ScriptSource;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads real files off disk, which is the only way to check that the two-pass loader still reads,
 * normalizes, parses and compiles a scriptpack, and that a preprocessor placed in the middle of it
 * reaches the compiler.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptLoadingTest {

    private static final AddonOwner ADDON = new AddonOwner("LoaderAddon", "1.0", AddonOwner.Origin.ADDON);

    @TempDir
    static Path dataFolder;

    private static CorexRegistry registry;
    private static Path previousDataFolder;

    @BeforeAll
    public static void setup() throws IOException {
        registry = CorexTestEnvironment.bootstrap();
        previousDataFolder = ScriptManager.getDataFolder();

        Path scripts = Files.createDirectories(dataFolder.resolve("scripts"));
        Files.writeString(scripts.resolve("sample.cx"), """
                // using:combat
                sample_task:
                  type: task
                  script:
                  // this line is a comment and must not reach the compiler
                  - narrate Hello #1!
                  - narrate Second line
                """);

        Files.writeString(scripts.resolve("annotated.cx"), """
                annotated_task:
                  type: task
                  script:
                  - @quiet
                  - narrate Loud
                """);

        ScriptManager.setDataFolder(dataFolder);
    }

    @AfterAll
    public static void restore() {
        if (previousDataFolder != null) {
            ScriptManager.setDataFolder(previousDataFolder);
        }
    }

    @Test
    @Order(1)
    public void aPlainScriptLoadsAndCompiles() {
        ScriptManager.loadScripts();

        AbstractContainer container = ScriptManager.getContainer("sample_task");
        assertNotNull(container, "the task container must load from disk");
        assertEquals("task", container.getType());

        Instruction[] bytecode = container.getScript("script");
        assertNotNull(bytecode, "the script path must compile to bytecode");
        assertEquals(2, bytecode.length, "the comment line must not become an instruction");
        assertEquals("narrate", bytecode[0].command.getName());
        StringBuilder narrated = new StringBuilder();
        for (CompiledArgument argument : bytecode[0].linearArgs) {
            narrated.append(argument.getRaw());
        }
        assertTrue(narrated.toString().contains("#"),
                "a hash inside a script line is not a comment and must survive the round trip");
    }

    @Test
    @Order(2)
    public void aTreePassCanAddAWholeScript() {
        AddonOwner previous = AddonManager.enter(ADDON);
        try {
            registry.register(InjectingPass.class);
        }
        finally {
            AddonManager.exit(previous);
        }

        ScriptManager.loadScripts();

        AbstractContainer injected = ScriptManager.getContainer("injected_task");
        assertNotNull(injected, "a container invented at PARSED_YAML must compile like any other");
        assertNotNull(injected.getScript("script"));
        assertTrue(ScriptManager.getContainersByType(injected.getClass()).size() >= 2,
                "the file's own container must survive alongside the injected one");
    }

    @Test
    @Order(3)
    public void aCommentDirectiveReachesTheAddon() {
        AddonOwner previous = AddonManager.enter(ADDON);
        try {
            registry.register(UsingPass.class);
        }
        finally {
            AddonManager.exit(previous);
        }

        UsingPass.seen.clear();
        ScriptManager.loadScripts();

        assertEquals(List.of("combat"), UsingPass.seen,
                "a directive written as a comment must survive to the COMMENTS stage");
        assertNotNull(ScriptManager.getContainer("sample_task"),
                "reading comments must not disturb the script itself");
    }

    @Test
    @Order(4)
    public void anAnnotationEntryIsConsumedBeforeCompilation() {
        AddonOwner previous = AddonManager.enter(ADDON);
        try {
            registry.register(AnnotationPass.class);
        }
        finally {
            AddonManager.exit(previous);
        }

        ScriptManager.loadScripts();

        AbstractContainer annotated = ScriptManager.getContainer("annotated_task");
        assertNotNull(annotated);

        Instruction[] bytecode = annotated.getScript("script");
        assertNotNull(bytecode);
        assertEquals(1, bytecode.length, "the annotation entry must not survive as an instruction");
        assertEquals("narrate", bytecode[0].command.getName());

        StringBuilder narrated = new StringBuilder();
        for (CompiledArgument argument : bytecode[0].linearArgs) {
            narrated.append(argument.getRaw());
        }
        assertTrue(narrated.toString().contains("Quiet"),
                "the annotation must have changed the entry after it, got: " + narrated);
    }

    /** Consumes an annotation entry and applies it to the next line. */
    public static class AnnotationPass implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return "annotations";
        }

        @Override
        public @Nullable List<Object> scriptBlock(@NotNull ScriptSource source, @NotNull String script,
                                                  @NotNull String path, @NotNull List<Object> lines) {
            if (!lines.contains("@quiet")) return null;

            List<Object> rewritten = new ArrayList<>(lines.size());
            boolean quiet = false;

            for (Object line : lines) {
                if ("@quiet".equals(line)) {
                    quiet = true;
                    continue;
                }
                if (quiet && line instanceof String text) {
                    rewritten.add(text.replace("Loud", "Quiet"));
                    quiet = false;
                    continue;
                }
                rewritten.add(line);
            }
            return rewritten;
        }
    }

    /** Reads a directive out of a comment, which is the one thing no later stage can offer. */
    public static class UsingPass implements AbstractPreprocessor {

        static final List<String> seen = new ArrayList<>();

        @Override
        public @NotNull String getName() {
            return "using";
        }

        @Override
        public void comments(@NotNull ScriptSource source, @NotNull List<ScriptComment> comments) {
            for (ScriptComment comment : comments) {
                String module = comment.directive("using");
                if (module != null) seen.add(module);
            }
        }
    }

    /** Invents a second script out of nothing, to prove the tree stage feeds the compiler. */
    public static class InjectingPass implements AbstractPreprocessor {

        @Override
        public @NotNull String getName() {
            return "injector";
        }

        @Override
        public @Nullable Map<String, Object> parsedYaml(@NotNull ScriptSource source,
                                                        @NotNull Map<String, Object> tree) {
            if (tree.containsKey("injected_task")) return null;

            Map<String, Object> script = new LinkedHashMap<>();
            script.put("type", "task");
            script.put("script", List.of("narrate Injected!"));

            Map<String, Object> copy = new LinkedHashMap<>(tree);
            copy.put("injected_task", script);
            return copy;
        }
    }
}

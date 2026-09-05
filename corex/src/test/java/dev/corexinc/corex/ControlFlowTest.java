package dev.corexinc.corex;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs small scripts through a real queue and reads the definitions they leave behind, which is
 * the smallest thing that fails when block unwinding or if/else pairing regresses.
 */
public class ControlFlowTest {

    @BeforeAll
    public static void setup() {
        CorexTestEnvironment.bootstrap();
    }

    private static String run(List<?> block, String definition) {
        Instruction[] bytecode = ScriptManager.compileScript(block);
        ScriptQueue queue = new ScriptQueue(ScriptQueue.uniqueId("ControlFlowTest"), bytecode, false, null);
        queue.setSilent(true);
        queue.start();
        AbstractTag value = queue.getDefinition(definition);
        assertNotNull(value, definition);
        return value.identify();
    }

    @Test
    public void breakInsideNestedIfSkipsRestOfIteration() {
        String last = run(List.of(
                "def last 0",
                Map.of("foreach 1|2|3:", List.of(
                        Map.of("if <[value]> == 2:", List.of("foreach break")),
                        "def last <[value]>"))
        ), "last");
        assertEquals("1", last);
    }

    @Test
    public void continueInsideNestedIfSkipsRestOfIteration() {
        String seen = run(List.of(
                "def seen none",
                Map.of("foreach 1|2|3:", List.of(
                        Map.of("if <[value]> == 2:", List.of("foreach continue")),
                        "def seen <[seen]>,<[value]>"))
        ), "seen");
        assertEquals("none,1,3", seen);
    }

    @Test
    public void nestedIfDoesNotStealTheOuterElse() {
        String branch = run(List.of(
                "def branch none",
                Map.of("if true:", List.of(
                        Map.of("if false:", List.of("def branch inner")))),
                Map.of("else:", List.of("def branch else"))
        ), "branch");
        assertEquals("none", branch);
    }
}

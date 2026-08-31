package dev.corexinc.corex;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.environment.utils.commands.CommandParser;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A block written in braces after {@code /run} has to reach the command as raw lines, the same way
 * a block written under it in a file does. The far side is the only place those lines can be
 * compiled: a command from an addon installed elsewhere does not exist in this registry, so
 * anything that compiles the block here loses it.
 */
public class RemoteScriptLineTest {

    @BeforeAll
    public static void setup() {
        CorexTestEnvironment.bootstrap();
    }

    @Test
    public void anInlineBlockReachesTheCommandUncompiled() {
        Instruction[] bytecode = CommandParser.compileScript(
                "proxy script to:lobby { - unittestremote alpha }");

        assertEquals(1, bytecode.length);
        assertEquals("proxy", bytecode[0].command.getName());
        assertEquals(List.of("unittestremote alpha"), bytecode[0].customData,
                "a data block command must receive the raw lines, not bytecode built against "
                        + "this server's registry");
    }

    @Test
    public void aNestedInlineBlockKeepsItsShape() {
        Instruction[] bytecode = CommandParser.compileScript(
                "proxy script to:lobby { - if <player.isOp>: { - unittestremote beta } - wait 5 }");

        assertEquals(1, bytecode.length);
        List<?> block = assertInstanceOf(List.class, bytecode[0].customData);
        assertEquals(2, block.size());
        assertEquals(Map.of("if <player.isOp>:", List.of("unittestremote beta")), block.getFirst(),
                "a nested block travels as a map from its line to its own lines");
        assertEquals("wait 5", block.get(1));
    }

    @Test
    public void anUnknownCommandOutsideABlockStillFails() {
        assertNull(ScriptCompiler.compile("unittestremote gamma"),
                "only a block on its way elsewhere skips compilation here");
    }
}

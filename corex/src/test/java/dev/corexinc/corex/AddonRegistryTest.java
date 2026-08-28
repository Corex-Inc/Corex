package dev.corexinc.corex;

import dev.corexinc.corex.api.addons.AbstractCorexAddon;
import dev.corexinc.corex.api.addons.CorexRegistrar;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonOwnership;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the addon gate: who may register, what happens when two addons register at once, and
 * whether a component can still be traced back to the addon that supplied it once it misbehaves.
 *
 * <p>The addon state is global by nature, and booting the test server leaves it sealed exactly as
 * a running server would, so these run in a fixed order: everything that reads the state Corex
 * built comes first, and the case that resets the window comes after it.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddonRegistryTest {

    private static final AddonOwner FIRST_ADDON =
            new AddonOwner("FirstAddon", "1.0", AddonOwner.Origin.ADDON);

    private static final AddonOwner SECOND_ADDON =
            new AddonOwner("SecondAddon", "2.3", AddonOwner.Origin.ADDON);

    private static CorexRegistry registry;

    @BeforeAll
    public static void setup() {
        registry = CorexTestEnvironment.bootstrap();
    }

    @Test
    @Order(1)
    public void corexOwnComponentsAreNotBlamedOnAnyone() {
        assertSame(AddonOwner.CORE, AddonOwnership.ownerOf(AddonOwnership.Kind.COMMAND, "narrate"),
                "built-in commands belong to Corex");
        assertSame(AddonOwner.CORE, AddonOwnership.ownerOf(AddonOwnership.Kind.SUB_TAG, "ElementTag.toUppercase"),
                "built-in sub-tags are claimed under Object.subTag");
        assertNull(AddonOwnership.describe(AddonOwnership.Kind.COMMAND, "narrate"),
                "Corex's own commands produce no addon note");
    }

    @Test
    @Order(2)
    public void anOverrideRecordsTheNewOwner() {
        AddonOwnership.claim(AddonOwnership.Kind.SUB_TAG, "PlayerTag.overrideProbe", AddonOwner.CORE);
        AddonOwnership.claim(AddonOwnership.Kind.SUB_TAG, "PlayerTag.overrideProbe", SECOND_ADDON);

        String note = AddonOwnership.describe(AddonOwnership.Kind.SUB_TAG, "PlayerTag.overrideProbe");
        assertNotNull(note, "an overridden tag must report its new owner");
        assertTrue(note.contains("SecondAddon"), "the note names the overriding addon: " + note);
    }

    @Test
    @Order(3)
    public void aCrashInAddonCodeNamesTheAddon() {
        AddonManager.noteClass(AddonCommand.class, FIRST_ADDON);

        RuntimeException crash = new RuntimeException("boom");
        crash.setStackTrace(new StackTraceElement[]{
                new StackTraceElement(AddonCommand.class.getName(), "run", "AddonCommand.java", 12)
        });

        assertEquals(FIRST_ADDON, AddonManager.blame(crash),
                "a stack frame in a registered addon class points at that addon");
        assertNull(AddonManager.blame(new RuntimeException("no frames of ours")));
    }

    @Test
    @Order(4)
    public void aPlainObjectIsNotAnAddon() {
        assertThrows(IllegalStateException.class, () -> CorexRegistrar.open(new NotAPlugin()));
    }

    @Test
    @Order(5)
    public void registeringAfterCompilationIsRefused() {
        assertTrue(AddonManager.isSealed(), "booting the server compiles scripts and seals the window");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> AddonManager.openScope(FIRST_ADDON));
        assertTrue(failure.getMessage().contains("onLoad"),
                "the refusal says where registration belongs instead: " + failure.getMessage());
    }

    @Test
    @Order(6)
    public void addonRegistrationIsAttributedToTheAddon() {
        AddonManager.reset();
        assertFalse(AddonManager.isSealed(), "a fresh load reopens the window");

        AddonManager.openScope(FIRST_ADDON);
        registry.register(AddonCommand.class);
        AddonManager.closeScope(FIRST_ADDON);

        assertSame(FIRST_ADDON, AddonOwnership.ownerOf(AddonOwnership.Kind.COMMAND, "addontest"),
                "a command registered inside a scope belongs to that addon");
        assertNotNull(registry.getScriptCommands().getCommand("addontest"),
                "the command still has to reach the command registry");
        assertTrue(AddonManager.getAddons().contains(FIRST_ADDON),
                "an addon that registered anything shows up in the addon list");

        String note = AddonOwnership.describe(AddonOwnership.Kind.COMMAND, "addontest");
        assertNotNull(note);
        assertTrue(note.contains("FirstAddon"), "the note names the addon: " + note);
    }

    @Test
    @Order(7)
    public void twoAddonsCannotRegisterAtOnce() {
        AddonManager.openScope(FIRST_ADDON);
        try {
            IllegalStateException failure =
                    assertThrows(IllegalStateException.class, () -> AddonManager.openScope(SECOND_ADDON));
            assertTrue(failure.getMessage().contains("FirstAddon"),
                    "the refusal names whoever is holding the registrar: " + failure.getMessage());
        }
        finally {
            AddonManager.closeScope(FIRST_ADDON);
        }
    }

    /** A command with no dependencies, only here to be registered and owned. */
    public static class AddonCommand implements AbstractCommand {

        @Override
        public @NotNull String getName() {
            return "addontest";
        }

        @Override
        public @NotNull String getSyntax() {
            return "addontest";
        }

        @Override
        public int getMinArgs() {
            return 0;
        }

        @Override
        public int getMaxArgs() {
            return 0;
        }

        @Override
        public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
        }
    }

    /** Carries the marker without being a plugin, which is exactly what the registrar refuses. */
    private static class NotAPlugin implements AbstractCorexAddon {
    }
}

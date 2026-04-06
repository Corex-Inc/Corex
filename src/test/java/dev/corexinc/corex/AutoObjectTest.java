package dev.corexinc.corex;

import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.compiler.TagNode;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.utils.CorexTestLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Tag("ObjectTagTest")
public class AutoObjectTest {
    private static CorexRegistry registry;

    @BeforeAll
    public static void setup() {
        ServerMock server = MockBukkit.mock();

        server.addSimpleWorld("world");
        Objects.requireNonNull(server.getWorld("world")).getBlockAt(1, 1, 1).setType(org.bukkit.Material.STONE);
        java.util.UUID testUUID = java.util.UUID.fromString("465876c1-2a15-4fc0-9f0b-97de13aa46f1");
        org.mockbukkit.mockbukkit.entity.PlayerMock mockPlayer =
                new org.mockbukkit.mockbukkit.entity.PlayerMock(server, "TestPlayer", testUUID);
        server.addPlayer(mockPlayer);
        mockPlayer.setLocation(new org.bukkit.Location(server.getWorld("world"), 10.5, 64.0, 10.5, 90f, 0f));

        java.util.UUID entityUUID = java.util.UUID.fromString("cf5d1e35-fb92-476e-9c96-bc932ca0b0cb");
        org.mockbukkit.mockbukkit.entity.ItemDisplayMock itemDisplay =
                new org.mockbukkit.mockbukkit.entity.ItemDisplayMock(server, entityUUID);
        server.registerEntity(itemDisplay);

        Corex plugin = MockBukkit.load(Corex.class);
        registry = plugin.getRegistry();
        CorexTestLogger.info("ObjectTagTest environment has been started!");
    }

    @Test
    public void runDeepTagFuzzing() {
        List<String> allFailures = new ArrayList<>();

        for (Class<? extends AbstractTag> clazz : registry.getRegisteredTagClasses()) {
            try {
                AbstractTag dummy;
                try {
                    dummy = clazz.getDeclaredConstructor(String.class).newInstance("test_init");
                } catch (NoSuchMethodException e) {
                    dummy = clazz.getDeclaredConstructor().newInstance();
                }

                String startVal = dummy.getTestValue();

                if (startVal == null) {
                    CorexTestLogger.info(clazz.getSimpleName() + ": Test disabled. Skipping...");
                    continue;
                }

                AbstractTag testObject = ObjectFetcher.pickObject(startVal);
                if (testObject == null) {
                    allFailures.add(clazz.getSimpleName() + ": ObjectFetcher returned null for " + startVal);
                    continue;
                }

                CorexTestLogger.info("Testing: " + clazz.getSimpleName() + " (Sample: " + startVal + ")");
                TagProcessor<?> processor = testObject.getProcessor();

                processor.getRegisteredTags().forEach((tagName, data) -> {
                    if (data.skipTest) {
                        CorexTestLogger.info("    [Skipped] ." + tagName + " (Manual ignore)");
                        return;
                    }

                    String fullTagStr = tagName + (data.testParam != null ? "[" + data.testParam + "]" : "");
                    if (data.testChain != null) {
                        for (String c : data.testChain) fullTagStr += "." + c;
                    }

                    System.out.print("    \u001B[36m-> Subtag: ." + fullTagStr + " ... \u001B[0m");

                    try {
                        TagNode[] nodes = ScriptCompiler.parseTagNodes(fullTagStr);
                        Attribute attr = new Attribute(nodes, null);
                        AbstractTag result = testObject.getAttribute(attr);

                        assertNotNull(result, "Tag returned null!");
                        assertTrue(data.returnType.isInstance(result), "Wrong type: " + result.getClass().getSimpleName());

                        System.out.println("\u001B[32mOK! (" + result.identify() + ")\u001B[0m");
                    } catch (Throwable e) {
                        System.out.println("\u001B[31mERROR!\u001B[0m");
                        allFailures.add(clazz.getSimpleName() + "." + fullTagStr + " -> " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                allFailures.add("Critical failure in " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }

        if (!allFailures.isEmpty()) {
            System.out.println("\n--- [ TEST SUMMARY: " + allFailures.size() + " ERRORS FOUND ] ---");
            allFailures.forEach(CorexTestLogger::error);
            fail("Deep Tag Fuzzing failed with " + allFailures.size() + " errors.");
        } else {
            CorexTestLogger.success("All tags passed fuzzing successfully!");
        }
    }
}
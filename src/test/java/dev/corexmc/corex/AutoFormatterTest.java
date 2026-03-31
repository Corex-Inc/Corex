package dev.corexmc.corex;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.engine.tags.TagParser;
import dev.corexmc.corex.utils.CorexTestLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("FormatterTest")
public class AutoFormatterTest {

    private static Corex plugin;

    @BeforeAll
    public static void setup() {
        if (!MockBukkit.isMocked()) MockBukkit.mock();
        plugin = MockBukkit.load(Corex.class);
        CorexTestLogger.info("FormatterTest environment started!");
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void runFormatFuzzing() {
        List<String> allFailures = new ArrayList<>();
        List<Class<? extends AbstractFormatter>> formatters = plugin.getRegistry().getRegisteredFormatterClasses();

        if (formatters.isEmpty()) {
            fail("No registered Formatters were found!");
        }

        for (Class<? extends AbstractFormatter> clazz : formatters) {
            try {
                AbstractFormatter formatter = clazz.getDeclaredConstructor().newInstance();
                String name = formatter.getName();
                String value = formatter.getObject().identify();

                CorexTestLogger.info("Testing Formatter: <" + name + "> (Class: " + clazz.getSimpleName() + ")");

                assertNotNull(name, "Formatter name cannot be null!");
                assertNotNull(value, "Formatter value cannot be null!");

                System.out.print("    \u001B[36m-> Parser Check: <" + name + "> ... \u001B[0m");
                String parsedResult = TagParser.parse("<" + name + ">").evaluate(null);

                assertEquals(value, parsedResult, "Parser returned an incorrect value for <" + name + ">!");
                System.out.println("\u001B[32mOK!\u001B[0m");

                if (formatter.getAlias() != null) {
                    for (String alias : formatter.getAlias()) {
                        System.out.print("    \u001B[36m-> Alias Check: <" + alias + "> ... \u001B[0m");
                        String aliasResult = TagParser.parse("<" + alias + ">").evaluate(null);
                        assertEquals(value, aliasResult, "Parser did not recognize alias <" + alias + ">!");
                        System.out.println("\u001B[32mOK!\u001B[0m");
                    }
                }

            } catch (Throwable e) {
                System.out.println("\u001B[31mERROR!\u001B[0m");
                allFailures.add(clazz.getSimpleName() + " -> " + e.getMessage());
            }
        }

        if (!allFailures.isEmpty()) {
            System.out.println("\n--- [ FORMATTER TEST SUMMARY: " + allFailures.size() + " ERRORS FOUND ] ---");
            allFailures.forEach(CorexTestLogger::error);
            fail("Format Tag Fuzzing failed with " + allFailures.size() + " errors.");
        } else {
            CorexTestLogger.success("All formatters passed successfully!");
        }
    }
}
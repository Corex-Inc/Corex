package dev.corexinc.corex;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.utils.CorexTestLogger;
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
                String testParam = formatter.getTestParam();

                String fullTag;
                if (testParam != null) {
                    fullTag = "<" + name + "[" + testParam + "]>";
                } else {
                    fullTag = "<" + name + ">";
                }

                CorexTestLogger.info("Testing Formatter: " + fullTag + " (Class: " + clazz.getSimpleName() + ")");

                CompiledArgument arg =
                        ScriptCompiler.parseArg(fullTag);

                assertNotNull(arg);
                String result = arg.evaluate(null).identify();

                assertNotNull(result, "Formatter " + name + " returned null!");

                if (name.equals("n")) {
                    assertEquals("\n", result);
                }

                System.out.println("\u001B[32mOK! Result: " + result.replace("\n", "\\n") + "\u001B[0m");

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
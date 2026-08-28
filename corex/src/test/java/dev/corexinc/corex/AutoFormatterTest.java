package dev.corexinc.corex;

import dev.corexinc.corex.api.testing.FormatterTestSuite;
import dev.corexinc.corex.api.testing.TestReport;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import dev.corexinc.corex.testing.CorexTestLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("FormatterTest")
public class AutoFormatterTest {

    private static CorexRegistry registry;

    @BeforeAll
    public static void setup() {
        registry = CorexTestEnvironment.bootstrap();
        CorexTestLogger.info("FormatterTest environment started!");
    }

    @AfterAll
    public static void tearDown() {
        CorexTestEnvironment.shutdown();
    }

    @Test
    public void runFormatFuzzing() {
        TestReport report = FormatterTestSuite.on(registry)
                .onProgress(CorexTestLogger::info)
                .run();

        assertTrue(report.getSubjectCount() > 0, "No registered Formatters were found!");

        if (report.isSuccess()) {
            CorexTestLogger.success("All " + report.getSubjectCount() + " formatters passed successfully!");
        } else {
            CorexTestLogger.error("--- [ FORMATTER TEST SUMMARY: " + report.getFailures().size() + " ERRORS FOUND ] ---");
            report.getFailures().forEach(failure -> CorexTestLogger.error(failure.toString()));
        }

        assertTrue(report.isSuccess(), report::format);
    }

    /** Kept as a separate case: the newline formatter has an exact expected value, not just a type. */
    @Test
    public void newlineFormatterYieldsNewline() {
        assertEquals("\n", ScriptCompiler.parseArg("<n>").evaluate(null).identify());
    }
}

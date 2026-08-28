package dev.corexinc.corex;

import dev.corexinc.corex.api.testing.TagTestSuite;
import dev.corexinc.corex.api.testing.TestReport;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import dev.corexinc.corex.testing.CorexTestLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("ObjectTagTest")
public class AutoObjectTest {

    private static CorexRegistry registry;

    @BeforeAll
    public static void setup() {
        registry = CorexTestEnvironment.bootstrap();
        CorexTestLogger.info("ObjectTagTest environment has been started!");
    }

    @Test
    public void runDeepTagFuzzing() {
        // No registering() here on purpose: Corex tests everything in the registry.
        // An addon narrows the run to its own tags by passing its loader instead.
        TestReport report = TagTestSuite.on(registry)
                .onProgress(CorexTestLogger::info)
                .run();

        if (report.isSuccess()) {
            CorexTestLogger.success("All tags passed fuzzing! ("
                    + report.getSubjectCount() + " tags, " + report.getCheckCount() + " subtags)");
        } else {
            CorexTestLogger.error("--- [ TEST SUMMARY: " + report.getFailures().size() + " ERRORS FOUND ] ---");
            report.getFailures().forEach(failure -> CorexTestLogger.error(failure.toString()));
        }

        assertTrue(report.isSuccess(), report::format);
    }
}

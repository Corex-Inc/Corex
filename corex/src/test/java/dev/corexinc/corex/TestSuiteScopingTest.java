package dev.corexinc.corex;

import dev.corexinc.corex.api.testing.FormatterTestSuite;
import dev.corexinc.corex.api.testing.TagTestSuite;
import dev.corexinc.corex.api.testing.TestReport;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the promise made to addons: handing a loader to the suite scopes the run to whatever
 * that loader registered. An addon must never end up running Corex's own tag suite.
 */
@Tag("ObjectTagTest")
public class TestSuiteScopingTest {

    private static CorexRegistry registry;

    @BeforeAll
    public static void setup() {
        registry = CorexTestEnvironment.bootstrap();
    }

    @Test
    public void loaderRegisteringNothingTestsNothing() {
        TestReport report = TagTestSuite.on(registry).registering(reg -> {}).run();

        assertEquals(0, report.getSubjectCount(), "a loader that registers nothing must test nothing");
        assertEquals(0, report.getCheckCount());
        assertTrue(report.isSuccess());
    }

    @Test
    public void formatterLoaderRegisteringNothingTestsNothing() {
        TestReport report = FormatterTestSuite.on(registry).registering(reg -> {}).run();

        assertEquals(0, report.getSubjectCount(), "a loader that registers nothing must test nothing");
        assertTrue(report.isSuccess());
    }

    @Test
    public void withoutLoaderTheWholeRegistryIsInScope() {
        TestReport report = TagTestSuite.on(registry).filter(clazz -> false).run();

        assertEquals(0, report.getSubjectCount());
        assertTrue(registry.getRegisteredTagClasses().size() > 0, "sanity: registry is populated");
    }
}

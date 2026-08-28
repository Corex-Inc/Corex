package dev.corexinc.corex.api.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a {@link TagTestSuite} or {@link FormatterTestSuite} run.
 * <p>
 * Deliberately free of any test-framework types, so the suites can live in the main
 * artifact without dragging JUnit into every consumer's dependency graph. Assert on
 * {@link #isSuccess()} from whatever framework the caller happens to use, and pass
 * {@link #format()} as the failure message.
 */
public final class TestReport {

    /** A single failed check: {@code subject} is what was tested, {@code detail} is why it failed. */
    public record Failure(String subject, String detail) {
        @Override
        public String toString() {
            return subject + " -> " + detail;
        }
    }

    private final List<Failure> failures;
    private final int subjects;
    private final int checks;

    TestReport(List<Failure> failures, int subjects, int checks) {
        this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
        this.subjects = subjects;
        this.checks = checks;
    }

    /** {@code true} when nothing failed. Note that a run with zero subjects also passes. */
    public boolean isSuccess() {
        return failures.isEmpty();
    }

    public List<Failure> getFailures() {
        return failures;
    }

    /** Number of tags (or formatters) that were exercised. */
    public int getSubjectCount() {
        return subjects;
    }

    /** Number of individual checks performed across all subjects. */
    public int getCheckCount() {
        return checks;
    }

    /** Human-readable summary, suitable as an assertion message. */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tested ").append(subjects).append(" subject(s), ")
                .append(checks).append(" check(s), ")
                .append(failures.size()).append(" failure(s).");

        for (Failure failure : failures) {
            sb.append(System.lineSeparator()).append("  - ").append(failure);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return format();
    }
}

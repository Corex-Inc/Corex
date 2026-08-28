package dev.corexinc.corex.api.testing;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Compiles and evaluates every registered {@link AbstractFormatter}, reporting the ones that throw
 * or produce nothing. Scoping works the same way as {@link TagTestSuite}: hand it your loader via
 * {@link #registering(Consumer)} and only your formatters are exercised.
 */
public final class FormatterTestSuite {

    private final CorexRegistry registry;
    private Consumer<CorexRegistry> loader;
    private Predicate<Class<? extends AbstractFormatter>> filter = clazz -> true;
    private Consumer<String> progress = message -> {};

    private FormatterTestSuite(CorexRegistry registry) {
        this.registry = registry;
    }

    public static FormatterTestSuite on(CorexRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        return new FormatterTestSuite(registry);
    }

    public FormatterTestSuite registering(Consumer<CorexRegistry> loader) {
        this.loader = loader;
        return this;
    }

    public FormatterTestSuite filter(Predicate<Class<? extends AbstractFormatter>> filter) {
        this.filter = filter != null ? filter : clazz -> true;
        return this;
    }

    public FormatterTestSuite onProgress(Consumer<String> progress) {
        this.progress = progress != null ? progress : message -> {};
        return this;
    }

    public TestReport run() {
        List<Class<? extends AbstractFormatter>> targets = resolveTargets();
        targets.removeIf(filter.negate());

        List<TestReport.Failure> failures = new ArrayList<>();
        int checks = 0;

        for (Class<? extends AbstractFormatter> clazz : targets) {
            checks++;
            try {
                AbstractFormatter formatter = clazz.getDeclaredConstructor().newInstance();
                String name = formatter.getName();
                String testParam = formatter.getTestParam();

                String expression = testParam != null
                        ? "<" + name + "[" + testParam + "]>"
                        : "<" + name + ">";

                CompiledArgument arg = ScriptCompiler.parseArg(expression);
                if (arg == null) {
                    failures.add(new TestReport.Failure(clazz.getSimpleName(),
                            "could not compile " + expression));
                    continue;
                }

                var evaluated = arg.evaluate(null);
                if (evaluated == null || evaluated.identify() == null) {
                    failures.add(new TestReport.Failure(clazz.getSimpleName(),
                            expression + " evaluated to null"));
                    continue;
                }

                progress.accept("OK " + expression + " -> " + evaluated.identify().replace("\n", "\n"));
            } catch (Throwable e) {
                String message = e.getMessage();
                failures.add(new TestReport.Failure(clazz.getSimpleName(),
                        e.getClass().getSimpleName() + (message != null ? ": " + message : "")));
            }
        }

        return new TestReport(failures, targets.size(), checks);
    }

    private List<Class<? extends AbstractFormatter>> resolveTargets() {
        if (loader == null) {
            return new ArrayList<>(registry.getRegisteredFormatterClasses());
        }

        List<Class<? extends AbstractFormatter>> before = new ArrayList<>(registry.getRegisteredFormatterClasses());
        loader.accept(registry);

        List<Class<? extends AbstractFormatter>> added = new ArrayList<>(registry.getRegisteredFormatterClasses());
        added.removeAll(before);
        return added;
    }
}

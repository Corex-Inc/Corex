package dev.corexinc.corex.api.testing;

import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.compiler.TagNode;
import dev.corexinc.corex.engine.tags.ObjectFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Fuzzes every registered subtag of a set of {@link AbstractTag} classes and reports what broke.
 * <p>
 * The test data comes from the tags themselves — {@link AbstractTag#getTestValue()} supplies the
 * sample object, and each registered subtag carries its own {@code testParam}, {@code testChain},
 * {@code returnType} and {@code skipTest}. Nothing here is Corex-specific, so an addon's tags are
 * exercised exactly the same way as the built-in ones.
 * <p>
 * By default every tag currently in the registry is tested. Addons should instead hand their own
 * loader to {@link #registering(Consumer)}: the suite snapshots the registry, runs the loader, and
 * tests only what appeared as a result — so an addon never ends up running Corex's own suite.
 *
 * <pre>{@code
 * TestReport report = TagTestSuite.on(registry)
 *         .registering(MyAddonLoader::registerAll)
 *         .run();
 *
 * assertTrue(report.isSuccess(), report::format);
 * }</pre>
 */
public final class TagTestSuite {

    private final CorexRegistry registry;
    private Consumer<CorexRegistry> loader;
    private Predicate<Class<? extends AbstractTag>> filter = clazz -> true;
    private Consumer<String> progress = message -> {};

    private TagTestSuite(CorexRegistry registry) {
        this.registry = registry;
    }

    public static TagTestSuite on(CorexRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        return new TagTestSuite(registry);
    }

    /**
     * Registers tags through the given loader and narrows the run to just those.
     * Pass the same loader the addon uses in production, so the registration path is covered too.
     */
    public TagTestSuite registering(Consumer<CorexRegistry> loader) {
        this.loader = loader;
        return this;
    }

    /** Further narrows which tag classes are tested. Applied after {@link #registering(Consumer)}. */
    public TagTestSuite filter(Predicate<Class<? extends AbstractTag>> filter) {
        this.filter = filter != null ? filter : clazz -> true;
        return this;
    }

    /** Receives a line per subtag as the run progresses. Defaults to discarding them. */
    public TagTestSuite onProgress(Consumer<String> progress) {
        this.progress = progress != null ? progress : message -> {};
        return this;
    }

    public TestReport run() {
        List<Class<? extends AbstractTag>> targets = resolveTargets();
        targets.removeIf(filter.negate());

        List<TestReport.Failure> failures = new ArrayList<>();
        int subjects = 0;
        int checks = 0;

        for (Class<? extends AbstractTag> clazz : targets) {
            AbstractTag sample;
            String startValue;

            try {
                AbstractTag dummy = instantiate(clazz);
                startValue = dummy.getTestValue();

                if (startValue == null) {
                    progress.accept(clazz.getSimpleName() + ": test disabled, skipping");
                    continue;
                }

                sample = ObjectFetcher.pickObject(startValue);
                if (sample == null) {
                    failures.add(new TestReport.Failure(clazz.getSimpleName(),
                            "ObjectFetcher could not build a sample from \"" + startValue + "\""));
                    continue;
                }
            } catch (Throwable e) {
                failures.add(new TestReport.Failure(clazz.getSimpleName(), "could not create sample: " + describe(e)));
                continue;
            }

            subjects++;
            progress.accept("Testing " + clazz.getSimpleName() + " (sample: " + startValue + ")");

            TagProcessor<?> processor = sample.getProcessor();
            if (processor == null) {
                failures.add(new TestReport.Failure(clazz.getSimpleName(), "getProcessor() returned null"));
                continue;
            }

            for (var entry : processor.getRegisteredTags().entrySet()) {
                TagProcessor.TagData<?> data = entry.getValue();
                if (data.skipTest) {
                    progress.accept("    [skipped] ." + entry.getKey());
                    continue;
                }

                StringBuilder expression = new StringBuilder(entry.getKey());
                if (data.testParam != null) expression.append('[').append(data.testParam).append(']');
                if (data.testChain != null) {
                    for (String link : data.testChain) expression.append('.').append(link);
                }

                String subject = clazz.getSimpleName() + "." + expression;
                checks++;

                try {
                    TagNode[] nodes = ScriptCompiler.parseTagNodes(expression.toString());
                    AbstractTag result = sample.getAttribute(new Attribute(nodes, null));

                    if (result == null) {
                        failures.add(new TestReport.Failure(subject, "tag returned null"));
                        continue;
                    }
                    if (!data.returnType.isInstance(result)) {
                        failures.add(new TestReport.Failure(subject, "expected " + data.returnType.getSimpleName()
                                + " but got " + result.getClass().getSimpleName()));
                        continue;
                    }

                    progress.accept("    OK ." + expression + " -> " + result.identify());
                } catch (Throwable e) {
                    failures.add(new TestReport.Failure(subject, describe(e)));
                }
            }
        }

        return new TestReport(failures, subjects, checks);
    }

    /**
     * Without a loader every registered tag is a target. With one, only the classes the loader
     * added — the registry list is snapshotted before the call and diffed after.
     */
    private List<Class<? extends AbstractTag>> resolveTargets() {
        if (loader == null) {
            return new ArrayList<>(registry.getRegisteredTagClasses());
        }

        List<Class<? extends AbstractTag>> before = new ArrayList<>(registry.getRegisteredTagClasses());
        loader.accept(registry);

        List<Class<? extends AbstractTag>> added = new ArrayList<>(registry.getRegisteredTagClasses());
        added.removeAll(before);
        return added;
    }

    private static AbstractTag instantiate(Class<? extends AbstractTag> clazz) throws Exception {
        try {
            return clazz.getDeclaredConstructor(String.class).newInstance("test_init");
        } catch (NoSuchMethodException e) {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message != null ? ": " + message : "");
    }
}

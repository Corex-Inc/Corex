package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.CommandExecutionException;
import dev.corexinc.corex.api.commands.NoDebug;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invokes a command through a {@code run} overload that takes resolved arguments.
 *
 * <p>A command may declare {@code run} either the classic way, taking the raw
 * {@link Instruction}, or with one parameter per argument in its
 * {@link AbstractCommand#getSyntax() syntax}. This class detects the second form,
 * checks it against the syntax <b>once, at registration</b>, and afterwards resolves and
 * converts the arguments before the body runs.
 *
 * <p>Binding failures are reported when the plugin loads rather than when a script
 * happens to use the command.
 */
public final class BoundCommand {

    private final AbstractCommand command;
    private final MethodHandle handle;
    private final List<SyntaxSlot> slots;
    private final Class<?>[] targets;
    private final String[] labels;
    private final boolean[] reported;
    private final boolean wantsQueue;

    private BoundCommand(AbstractCommand command, MethodHandle handle, List<SyntaxSlot> slots,
                         Class<?>[] targets, String[] labels, boolean[] reported, boolean wantsQueue) {
        this.command = command;
        this.handle = handle;
        this.slots = slots;
        this.targets = targets;
        this.labels = labels;
        this.reported = reported;
        this.wantsQueue = wantsQueue;
    }

    /**
     * Returns a binding for {@code command}, or null when it uses the classic form.
     * A declared-but-invalid overload logs the reason and also yields null.
     */
    public static BoundCommand bind(AbstractCommand command, List<SyntaxSlot> slots) {
        return bind(command, slots, problem -> fail(command, problem));
    }

    /**
     * Same checks as {@link #bind(AbstractCommand, List)}, but every problem is handed to
     * {@code onProblem} instead of being logged - so a test can assert on them.
     */
    public static BoundCommand bind(AbstractCommand command, List<SyntaxSlot> slots,
                                    java.util.function.Consumer<String> onProblem) {
        Method candidate = null;
        for (Method method : command.getClass().getMethods()) {
            if (!method.getName().equals("run")) continue;
            if (method.isSynthetic() || method.isBridge()) continue;
            if (isClassicForm(method)) continue;

            if (candidate != null) {
                onProblem.accept("declares more than one run(...) overload - keep exactly one");
                return null;
            }
            candidate = method;
        }
        if (candidate == null) return null;

        checkLabelKeys(command, slots, onProblem);

        Class<?>[] params = candidate.getParameterTypes();
        int cursor = 0;
        boolean wantsQueue = params.length > cursor && params[cursor] == ScriptQueue.class;
        if (wantsQueue) cursor++;

        int argCount = params.length - cursor;
        if (argCount != slots.size()) {
            onProblem.accept("run(...) takes " + argCount + " argument parameter(s) but its syntax declares "
                    + slots.size() + ": " + describe(slots)
                    + " - parameters after the optional ScriptQueue must match the syntax one for one");
            return null;
        }

        Class<?>[] targets = new Class<?>[argCount];
        String[] labels = new String[argCount];
        boolean[] reported = new boolean[argCount];
        Parameter[] declared = candidate.getParameters();

        for (int i = 0; i < argCount; i++) {
            Class<?> target = params[cursor + i];
            SyntaxSlot slot = slots.get(i);
            labels[i] = labelFor(command, declared[cursor + i], slot);
            reported[i] = !declared[cursor + i].isAnnotationPresent(NoDebug.class);

            if (slot.isFlag()) {
                if (target != boolean.class && target != Boolean.class) {
                    onProblem.accept("parameter #" + (i + 1) + " maps to flag " + slot.describe()
                            + " so it must be boolean, but is " + target.getSimpleName());
                    return null;
                }
            } else if (!isSupportedValueType(target)) {
                onProblem.accept("parameter #" + (i + 1) + " maps to " + slot.describe()
                        + " but " + target.getSimpleName() + " is not a tag, String, boolean or number");
                return null;
            }
            targets[i] = target;
        }

        try {
            candidate.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(candidate)
                    .asSpreader(Object[].class, params.length)
                    .asType(MethodType.methodType(void.class, Object.class, Object[].class));
            return new BoundCommand(command, handle, slots, targets, labels, reported, wantsQueue);
        } catch (Throwable t) {
            onProblem.accept("run(...) could not be bound: " + t);
            return null;
        }
    }

    /**
     * Picks the label a debug report uses for an argument.
     *
     * <p>An explicit {@link AbstractCommand#getReportLabels() label} wins. Otherwise the
     * syntax name is used, which is what the scripter reads anyway. Only when the syntax
     * carries a bare placeholder such as {@code <#.#>}, and so has no name to offer, does
     * the Java parameter name step in - and a positional label after that.
     */
    private static String labelFor(AbstractCommand command, Parameter parameter, SyntaxSlot slot) {
        String explicit = command.getReportLabels().get(slot.name());
        if (explicit != null && !explicit.isEmpty()) return explicit;

        String name = slot.name();
        if (name.startsWith("arg") && parameter.isNamePresent() && !parameter.getName().isEmpty()) {
            name = parameter.getName();
        }
        if (name.isEmpty()) return "Arg";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** A label pointing at no argument is almost always a typo, so say so at startup. */
    private static void checkLabelKeys(AbstractCommand command, List<SyntaxSlot> slots,
                                       java.util.function.Consumer<String> onProblem) {
        if (command.getReportLabels().isEmpty()) return;
        for (String key : command.getReportLabels().keySet()) {
            boolean known = false;
            for (SyntaxSlot slot : slots) {
                if (slot.name().equals(key)) { known = true; break; }
            }
            if (!known) {
                onProblem.accept("labels an argument '" + key + "' that its syntax does not declare"
                        + " - known arguments are " + describe(slots));
            }
        }
    }

    /** The classic form stays supported and is never treated as a bound overload. */
    private static boolean isClassicForm(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return params.length == 2 && params[0] == ScriptQueue.class && params[1] == Instruction.class;
    }

    public void invoke(ScriptQueue queue, Instruction instruction) {
        Object[] args = new Object[(wantsQueue ? 1 : 0) + slots.size()];
        int cursor = 0;
        if (wantsQueue) args[cursor++] = queue;

        boolean reporting = Debugger.shouldReport(queue);
        Object[] report = reporting ? new Object[slots.size() * 2] : null;

        AbstractTag[] positional = matchPositional(queue, instruction);

        for (int i = 0; i < slots.size(); i++) {
            SyntaxSlot slot = slots.get(i);
            Object value;
            try {
                value = resolve(slot, targets[i], queue, instruction,
                        slot.isLinear() ? positional[slot.linearIndex()] : null);
            } catch (CommandExecutionException e) {
                Debugger.echoError(queue, e.getMessage());
                return;
            }
            args[cursor + i] = value;

            if (reporting && reported[i]) {
                report[i * 2] = labels[i];
                report[i * 2 + 1] = value instanceof AbstractTag tag ? tag.identify() : value;
            }
        }

        // Reported before the body runs, so a crash still shows what the command was given.
        if (reporting) Debugger.report(queue, instruction, report);

        try {
            handle.invokeExact((Object) command, args);
        } catch (CommandExecutionException e) {
            Debugger.echoError(queue, e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decides which supplied positional argument fills which linear slot.
     *
     * <p>Arguments are matched by <b>type</b> rather than by position, so
     * {@code teleport <player> <[loc]>} and {@code teleport <[loc]> <player>} both land
     * correctly. Matching runs in confidence order: an argument that already <i>is</i> the
     * wanted type wins over one that merely parses into it, which in turn wins over a
     * loose conversion. Slots and arguments that cannot be told apart - two elements, say -
     * keep their written order, so nothing that worked positionally starts moving around.
     *
     * @return one entry per linear slot, holding the argument chosen for it, or null
     */
    private AbstractTag[] matchPositional(ScriptQueue queue, Instruction instruction) {
        int slotCount = 0;
        for (SyntaxSlot slot : slots) if (slot.isLinear()) slotCount++;
        AbstractTag[] chosen = new AbstractTag[slotCount];
        if (slotCount == 0) return chosen;

        int supplied = instruction.linearArgs.length;
        AbstractTag[] values = new AbstractTag[supplied];
        for (int i = 0; i < supplied; i++) values[i] = instruction.getLinearObject(i, queue);

        Class<?>[] slotTargets = new Class<?>[slotCount];
        int cursor = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isLinear()) slotTargets[cursor++] = targets[i];
        }

        boolean[] taken = new boolean[supplied];

        // Strongest matches first, so a precise type claims its argument before a
        // permissive one (ListTag accepts nearly anything) can swallow it.
        for (int confidence = 2; confidence >= 1; confidence--) {
            for (int slot = 0; slot < slotCount; slot++) {
                if (chosen[slot] != null) continue;
                int only = -1;
                for (int arg = 0; arg < supplied; arg++) {
                    if (taken[arg] || values[arg] == null) continue;
                    if (confidenceOf(values[arg], slotTargets[slot]) != confidence) continue;
                    if (only >= 0) { only = -2; break; }
                    only = arg;
                }
                if (only >= 0) {
                    chosen[slot] = values[only];
                    taken[only] = true;
                }
            }
        }

        // Whatever is left keeps the order it was written in.
        int next = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            if (chosen[slot] != null) continue;
            while (next < supplied && taken[next]) next++;
            if (next >= supplied) break;
            chosen[slot] = values[next];
            taken[next] = true;
        }
        return chosen;
    }

    /** 2 = already the wanted type, 1 = parses into it, 0 = only a loose conversion. */
    private static int confidenceOf(AbstractTag value, Class<?> target) {
        if (target.isInstance(value)) return 2;
        if (!AbstractTag.class.isAssignableFrom(target)) return 0;
        String identified = value.identify();
        if (identified.indexOf('@') > 0 && target.isInstance(ObjectFetcher.pickObject(identified))) return 1;
        return 0;
    }

    private Object resolve(SyntaxSlot slot, Class<?> target, ScriptQueue queue, Instruction instruction,
                           AbstractTag positional) {
        if (slot.isFlag()) return instruction.hasFlag(slot.name());

        AbstractTag raw = slot.isLinear()
                ? positional
                : instruction.getPrefixObject(slot.name(), queue);

        if (raw == null) {
            if (slot.defaultRaw() != null) return convert(new ElementTag(slot.defaultRaw()), target, slot);
            if (slot.required()) {
                throw new CommandExecutionException("Missing required argument " + slot.describe()
                        + " for '" + command.getName() + "'");
            }
            return emptyValue(target);
        }
        return convert(raw, target, slot);
    }

    private Object convert(AbstractTag raw, Class<?> target, SyntaxSlot slot) {
        if (target.isInstance(raw)) return raw;

        if (target == String.class) return raw.identify();

        if (target == boolean.class || target == Boolean.class) {
            return raw instanceof ElementTag el ? el.asBoolean() : Boolean.parseBoolean(raw.identify());
        }

        if (isNumeric(target)) {
            ElementTag el = raw instanceof ElementTag e ? e : new ElementTag(raw.identify());
            if (!el.isDouble()) {
                throw new CommandExecutionException("Argument " + slot.describe() + " of '" + command.getName()
                        + "' must be a number, got '" + raw.identify() + "'");
            }
            if (target == int.class || target == Integer.class) return el.asInt();
            if (target == long.class || target == Long.class) return el.asLong();
            if (target == float.class || target == Float.class) return (float) el.asDouble();
            return el.asDouble();
        }

        if (AbstractTag.class.isAssignableFrom(target)) {
            String identified = raw.identify();

            // Prefixed text such as "l@1,2,3" must go through the fetcher, which strips the
            // prefix. Handing the whole string to the constructor would prefix it twice.
            if (identified.indexOf('@') > 0) {
                AbstractTag fetched = ObjectFetcher.pickObject(identified);
                if (target.isInstance(fetched)) return fetched;
            }

            Object converted = fromStringConstructor(target, identified);
            if (converted != null) return converted;
            if (target.isAssignableFrom(ElementTag.class)) return new ElementTag(identified);

            throw new CommandExecutionException("Argument " + slot.describe() + " of '" + command.getName()
                    + "' expects " + target.getSimpleName() + ", got " + raw.getClass().getSimpleName()
                    + " ('" + raw.identify() + "')");
        }

        throw new CommandExecutionException("Cannot convert " + slot.describe() + " of '" + command.getName()
                + "' to " + target.getSimpleName());
    }

    private static Object emptyValue(Class<?> target) {
        if (target == boolean.class) return false;
        if (target == int.class) return 0;
        if (target == long.class) return 0L;
        if (target == float.class) return 0f;
        if (target == double.class) return 0d;
        return null;
    }

    private static boolean isNumeric(Class<?> target) {
        return target == int.class || target == Integer.class
                || target == long.class || target == Long.class
                || target == float.class || target == Float.class
                || target == double.class || target == Double.class;
    }

    private static boolean isSupportedValueType(Class<?> target) {
        return AbstractTag.class.isAssignableFrom(target)
                || target == String.class
                || target == boolean.class || target == Boolean.class
                || isNumeric(target);
    }

    private static final Map<Class<?>, Optional<Constructor<?>>> STRING_CONSTRUCTORS = new ConcurrentHashMap<>();

    private static Object fromStringConstructor(Class<?> type, String raw) {
        Constructor<?> ctor = STRING_CONSTRUCTORS.computeIfAbsent(type, key -> {
            try {
                return Optional.of(key.getConstructor(String.class));
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        }).orElse(null);
        if (ctor == null) return null;
        try {
            return ctor.newInstance(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static String describe(List<SyntaxSlot> slots) {
        List<String> parts = new ArrayList<>(slots.size());
        for (SyntaxSlot slot : slots) parts.add(slot.describe());
        return parts.isEmpty() ? "(none)" : String.join(" ", parts);
    }

    private static void fail(AbstractCommand command, String problem) {
        CorexLogger.error("Command '<yellow>" + command.getName() + "</yellow>' ("
                + command.getClass().getSimpleName() + ") " + problem + "!");
        CorexLogger.error("  <gray>Syntax: <white>" + command.getSyntax());
        CorexLogger.error("  <gray>Falling back to the classic run(queue, instruction).");
    }

}

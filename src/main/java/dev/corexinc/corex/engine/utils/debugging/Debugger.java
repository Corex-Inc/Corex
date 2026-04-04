package dev.corexinc.corex.engine.utils.debugging;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debugger - per-queue colored debug output for Corex.
 */
public class Debugger {

    private static final List<String> COLOR_POOL = List.of(
            "#f5a442", "#a442f5", "#42f5d4", "#f542a4", "#42a4f5",
            "#f5f542", "#78f542", "#f57842", "#4278f5", "#f54278",
            "#42f578", "#c242f5"
    );

    private static final AtomicInteger colorCounter = new AtomicInteger(0);

    private static final Map<String, QueueFormat> queueFormats = new ConcurrentHashMap<>();
    private static final Map<String, DebugLevel> queueLevelOverrides = new ConcurrentHashMap<>();

    private static volatile DebugLevel globalLevel = DebugLevel.INFO;

    // Кэш отступов для снижения нагрузки на Garbage Collector
    private static final String[] INDENTS = new String[32];
    static {
        for (int i = 0; i < INDENTS.length; i++) {
            INDENTS[i] = "  ".repeat(i);
        }
    }

    public static void setGlobalLevel(DebugLevel level) {
        globalLevel = level;
    }

    public static DebugLevel getGlobalLevel() {
        return globalLevel;
    }

    public static void setQueueLevel(String queueId, DebugLevel level) {
        queueLevelOverrides.put(queueId, level);
    }

    public static void clearQueueOverride(String queueId) {
        queueLevelOverrides.remove(queueId);
    }

    public static void queueStart(ScriptQueue queue) {
        if (!allows(queue, DebugLevel.INFO)) return;

        QueueFormat format = formatFor(queue);
        String mode   = queue.isAsync() ? "<gray>async" : "<gray>sync";
        String player = queue.getPlayer() != null
                ? " <dark_gray>player=<white>" + queue.getPlayer()
                : "";

        CorexLogger.info(format.header + " <gray>started <dark_gray>(" + mode + "<dark_gray>)" + player);
    }

    public static void queueStop(ScriptQueue queue, long elapsedMs) {
        if (!allows(queue, DebugLevel.INFO)) return;
        CorexLogger.success(formatFor(queue).header + " <gray>done <dark_gray>(" + elapsedMs + "ms)");
    }

    public static void queuePaused(ScriptQueue queue, long ticks) {
        if (!allows(queue, DebugLevel.VERBOSE)) return;
        CorexLogger.info(formatFor(queue).header + " <gray>paused for <white>" + ticks + " ticks");
    }

    public static void queueResumed(ScriptQueue queue) {
        if (!allows(queue, DebugLevel.VERBOSE)) return;
        CorexLogger.info(formatFor(queue).header + " <gray>resumed");
    }

    public static void frameCall(ScriptQueue queue, String calledScript, int depth) {
        if (!allows(queue, DebugLevel.INFO)) return;
        QueueFormat format = formatFor(queue);
        CorexLogger.info(indent(depth) + "<" + format.color + ">+-></" + format.color + "> <gray>calling <white>" + calledScript);
    }

    public static void frameReturn(ScriptQueue queue, int depth) {
        if (!allows(queue, DebugLevel.INFO)) return;
        QueueFormat format = formatFor(queue);
        CorexLogger.info(indent(depth) + "<" + format.color + ">+<-</" + format.color + "> <gray>returned");
    }

    public static void instructionStart(ScriptQueue queue, Instruction inst, int depth) {
        if (!allows(queue, DebugLevel.VERBOSE)) return;

        QueueFormat format = formatFor(queue);
        CorexLogger.info(indent(depth) + format.bar + " <white>" + inst.command.getName() + formatArgs(inst));
    }

    public static void instructionEnd(ScriptQueue queue, Instruction inst, int depth, double elapsedMs) {
        if (!allows(queue, DebugLevel.TRACE)) return;

        QueueFormat format = formatFor(queue);
        String timeStr = String.format(Locale.US, "%.3f", elapsedMs);

        CorexLogger.info(indent(depth) + format.bar + " <dark_gray>" + inst.command.getName() + " done in <white>" + timeStr + "ms");
    }

    public static void instructionSkipped(ScriptQueue queue, Instruction inst, String flagName, int depth) {
        if (!allows(queue, DebugLevel.VERBOSE)) return;

        QueueFormat format = formatFor(queue);
        CorexLogger.info(indent(depth) + format.bar + " <dark_gray>skipped <gray>" + inst.command.getName()
                + " <dark_gray>(flag: <white>" + flagName + "<dark_gray>)");
    }

    public static void tagFilled(ScriptQueue queue, String originalTag, String filledValue, int depth) {
        if (!allows(queue, DebugLevel.VERBOSE)) return;

        QueueFormat format = formatFor(queue);
        CorexLogger.info(indent(depth + 1) + format.bar
                + " <dark_gray><<gray>" + originalTag + "<dark_gray>> <dark_gray>= <aqua>" + filledValue);
    }

    public static void error(ScriptQueue queue, String message, int depth) {
        error(queue, message, null, depth);
    }

    public static void error(ScriptQueue queue, String message, Throwable cause, int depth) {
        if (!allows(queue, DebugLevel.ERROR)) return;

        String prefix = queue != null ? formatFor(queue).header + " " : "";
        CorexLogger.error(indent(depth) + prefix + message);

        if (cause == null) return;

        CorexLogger.error(indent(depth + 1) + "<dark_red>Caused by: <red>"
                + cause.getClass().getSimpleName() + ": " + cause.getMessage());

        StackTraceElement[] frames = cause.getStackTrace();
        int limit = Math.min(5, frames.length);
        for (int i = 0; i < limit; i++) {
            CorexLogger.error(indent(depth + 2) + "<dark_red>at <red>" + frames[i]);
        }
    }

    public static void error(String message) {
        CorexLogger.error(message);
    }

    public static void error(String message, Throwable cause) {
        CorexLogger.error(message);
        if (cause != null) {
            CorexLogger.error("<dark_red>Caused by: <red>" + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    public static void critical(ScriptQueue queue, String message) {
        String prefix = queue != null ? formatFor(queue).header + " " : "";
        CorexLogger.error(prefix + "<bold><red>!! CRITICAL !! <white>" + message);
    }

    public static void releaseQueue(String queueId) {
        queueFormats.remove(queueId);
        queueLevelOverrides.remove(queueId);
    }

    private static boolean allows(ScriptQueue queue, DebugLevel required) {
        if (queue == null) return globalLevel.ordinal() >= required.ordinal();
        DebugLevel effective = queueLevelOverrides.getOrDefault(queue.getId(), globalLevel);
        return effective.ordinal() >= required.ordinal();
    }

    private static QueueFormat formatFor(ScriptQueue queue) {
        return queueFormats.computeIfAbsent(queue.getId(), id -> {
            // Math.abs защищает от отрицательных значений при переполнении int
            int index = Math.abs(colorCounter.getAndIncrement()) % COLOR_POOL.size();
            return new QueueFormat(COLOR_POOL.get(index), id);
        });
    }

    private static String indent(int depth) {
        if (depth <= 0) return "";
        return depth < INDENTS.length ? INDENTS[depth] : "  ".repeat(depth);
    }

    private static String formatArgs(Instruction inst) {
        StringBuilder sb = new StringBuilder();

        for (CompiledArgument arg : inst.linearArgs) {
            sb.append(" <gray>").append(arg.getRaw());
        }
        for (Map.Entry<String, CompiledArgument> entry : inst.prefixArgs.entrySet()) {
            sb.append(" <gray>").append(entry.getKey()).append("<dark_gray>:<gray>").append(entry.getValue().getRaw());
        }
        for (String flag : inst.flags) {
            sb.append(" <dark_gray>--<gray>").append(flag);
        }

        return sb.toString();
    }

    private static class QueueFormat {
        final String color;
        final String header;
        final String bar;

        QueueFormat(String color, String queueId) {
            this.color = color;
            this.header = "<" + color + "><bold>[" + queueId + "]</bold></" + color + ">";
            this.bar = "<" + color + ">|</" + color + ">";
        }
    }
}
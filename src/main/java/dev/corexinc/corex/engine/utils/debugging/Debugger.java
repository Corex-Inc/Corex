package dev.corexinc.corex.engine.utils.debugging;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Debugger {

    public enum Mode {
        NONE,
        ERROR,
        DEFAULT,
        VERBOSE
    }

    private static final List<String> COLORS = List.of(
            "#d64933", "#fee440", "#02a9ea", "#778da9", "#fc9d39",
            "#fefae0", "#d3d3d3", "#f2542d", "#ffb703", "#9ef01a", "#ffee32",
            "#9b5de5", "#a1d8dd", "#1dc0ff"
    );

    private static final AtomicInteger nextColor = new AtomicInteger(0);
    private static final Map<String, QueueStyle> styles = new ConcurrentHashMap<>();
    private static volatile Mode mode = Mode.DEFAULT;

    public static Mode getMode() { return mode; }
    public static void setMode(Mode value) { mode = value; }
    public static boolean needsEvalCache() { return mode == Mode.VERBOSE; }

    public static void updateDebugMode() {
        String raw = Corex.getInstance().getConfig().getString("logger.debug-mode", "default");
        Mode debugMode;

        try {
            debugMode = Mode.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            debugMode = Mode.DEFAULT;
            error("Unknown debug-mode value: '" + raw + "', falling back to " + debugMode.name());
        }
        setMode(debugMode);
    }

    public static void queueStart(ScriptQueue queue) {
        if (mode == Mode.NONE || mode == Mode.ERROR) return;
        String player = queue.getPlayer() != null
                ? " <dark_gray>player=<white>" + queue.getPlayer().identify() : "";
        String m = queue.isAsync() ? "<gray>async" : "<gray>sync";
        CorexLogger.info(styleOf(queue).header + " <gray>started <dark_gray>(" + m + "<dark_gray>)" + player);
    }

    public static void queueStop(ScriptQueue queue, double ms) {
        if (mode == Mode.NONE || mode == Mode.ERROR) return;
        CorexLogger.success(styleOf(queue).header + " <gray>done <dark_gray>("
                + String.format(java.util.Locale.US, "%.4f", ms) + "ms)");
    }

    public static void instruction(ScriptQueue queue, Instruction inst, int depth) {
        if (mode != Mode.VERBOSE) return;
        CorexLogger.info(indent(depth) + styleOf(queue).bar + " <white>" + inst.command.getName() + formatArgs(inst, queue));
    }

    public static void tag(ScriptQueue queue, String original, String filled, int depth) {
        if (mode != Mode.VERBOSE) return;
        CorexLogger.info(indent(depth + 1) + styleOf(queue).bar
                + " <dark_gray><<gray>" + original + "<dark_gray>> <dark_gray>= <aqua>" + filled);
    }

    public static void error(ScriptQueue queue, String message, int depth) {
        error(queue, message, null, depth);
    }

    public static void error(ScriptQueue queue, String message, Throwable cause, int depth) {
        if (mode == Mode.NONE) return;
        String prefix = queue != null ? styleOf(queue).header + " " : "";
        CorexLogger.error(indent(depth) + prefix + message);
        if (cause == null) return;
        CorexLogger.error(indent(depth + 1) + "<dark_red>Caused by: <red>"
                + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        StackTraceElement[] trace = cause.getStackTrace();
        for (int i = 0; i < Math.min(5, trace.length); i++)
            CorexLogger.error(indent(depth + 2) + "<dark_red>at <red>" + trace[i]);
    }

    public static void error(String message) {
        if (mode == Mode.NONE) return;
        CorexLogger.error(message);
    }

    public static void error(String message, Throwable cause) {
        if (mode == Mode.NONE) return;
        CorexLogger.error(message);
        if (cause != null)
            CorexLogger.error("<dark_red>Caused by: <red>"
                    + cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    public static void releaseQueue(String queueId) {
        styles.remove(queueId);
    }

    private static QueueStyle styleOf(ScriptQueue queue) {
        return styles.computeIfAbsent(queue.getId(), id -> {
            int colorIndex = Math.abs(nextColor.getAndIncrement()) % COLORS.size();
            return new QueueStyle(COLORS.get(colorIndex), id);
        });
    }

    private static String indent(int depth) {
        return depth > 0 ? "  ".repeat(depth) : "";
    }

    private static String formatArgs(Instruction inst, ScriptQueue queue) {
        StringBuilder sb = new StringBuilder();
        for (CompiledArgument arg : inst.linearArgs) {
            AbstractTag cached = queue.getCached(arg);
            sb.append(" <gray>").append(cached != null ? cached.identify() : arg.getRaw());
        }
        for (Map.Entry<String, CompiledArgument> entry : inst.prefixArgs.entrySet()) {
            AbstractTag cached = queue.getCached(entry.getValue());
            sb.append(" <gray>").append(entry.getKey())
                    .append("<dark_gray>:<gray>")
                    .append(cached != null ? cached.identify() : entry.getValue().getRaw());
        }
        for (String flag : inst.flags)
            sb.append(" <dark_gray>--<gray>").append(flag);
        return sb.toString();
    }

    private record QueueStyle(String color, String header, String bar) {
        QueueStyle(String color, String queueId) {
            this(color,
                    "<" + color + "><bold>[" + queueId + "]</bold></" + color + ">",
                    "<" + color + ">|</" + color + ">");
        }
    }
}
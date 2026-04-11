package dev.corexinc.corex.engine.utils.debugging;

import dev.corexinc.corex.Corex;
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
        ERRORS,
        DEFAULT,
        ALL
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
        if (mode == Mode.NONE || mode == Mode.ERRORS) return;
        String player = queue.getPlayer() != null
                ? " <dark_gray>player=<white>" + queue.getPlayer().identify() : "";
        String m = queue.isAsync() ? "<gray>async" : "<gray>sync";
        CorexLogger.info(styleOf(queue).header + " <gray>started <dark_gray>(" + m + "<dark_gray>)" + player);
    }

    public static void queueStop(ScriptQueue queue, double ms) {
        if (mode == Mode.NONE || mode == Mode.ERRORS) return;
        CorexLogger.success(styleOf(queue).header + " <gray>done <dark_gray>("
                + String.format(java.util.Locale.US, "%.4f", ms) + "ms)");
    }

    public static void instruction(ScriptQueue queue, Instruction inst, int depth) {
        if (mode != Mode.ALL) return;
        CorexLogger.info(indent(depth) + styleOf(queue).bar + " <white>" + inst.command.getName() + formatArgs(inst));
    }

    public static void tag(ScriptQueue queue, String original, String filled, int depth) {
        if (mode != Mode.ALL) return;
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

    public static void report(ScriptQueue queue, Instruction inst, Object... keyValues) {
        flushErrors(queue, inst);

        if (mode != Mode.ALL) return;

        int depth = queue.getDepth();
        StringBuilder sb = new StringBuilder();

        sb.append(indent(depth)).append(styleOf(queue).bar)
                .append("<yellow> => <white>Executing '<yellow>").append(inst.command.getName().toUpperCase()).append("</yellow>'<gray>:");

        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length && keyValues[i + 1] != null) {
                String key = String.valueOf(keyValues[i]);
                String val = String.valueOf(keyValues[i + 1]);

                sb.append(" ").append(key).append("='<aqua>").append(val).append("</aqua><gray>'");
            }
        }

        CorexLogger.info(sb.toString());
    }

    public static void echoError(ScriptQueue queue, String message) {
        if (queue != null) {
            queue.addError(message);
        } else {
            CorexLogger.error(message);
        }
    }

    public static void flushErrors(ScriptQueue queue, Instruction inst) {
        if (queue == null || !queue.hasErrors()) return;

        if (mode == Mode.NONE) return;

        List<String> errors = queue.getAndClearErrors();

        if (!queue.isErrorHeaderPrinted()) {
            String cmdName = (inst != null && inst.command != null) ? inst.command.getName().toUpperCase() : "UNKNOWN";
            CorexLogger.error(styleOf(queue).bar + " ERROR while executing command '<yellow>" + cmdName + "</yellow>'!");
            CorexLogger.error(styleOf(queue).bar + "  <gray>Error Message:</gray> <white>" + errors.getFirst());
            queue.setErrorHeaderPrinted(true);
        }
        if (errors.size() != 1) {
            for (int i = 1; i < errors.size() - 1; i++) {
                CorexLogger.error(styleOf(queue).bar + "  <gray>├─> Additional Error Info: <white>" + errors.get(i));
            }
            CorexLogger.error(styleOf(queue).bar + "  <gray>└─> Additional Error Info: <white>" + errors.getLast());
        }
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

    private static String formatArgs(Instruction inst) {
        StringBuilder sb = new StringBuilder();

        for (CompiledArgument arg : inst.linearArgs) {
            sb.append(" <gray>").append(arg.getRaw());
        }
        for (Map.Entry<String, CompiledArgument> entry : inst.prefixArgs.entrySet()) {
            sb.append(" <gray>").append(entry.getKey())
                    .append("<dark_gray>:<gray>").append(entry.getValue().getRaw());
        }
        for (String flag : inst.flags) {
            sb.append(" <dark_gray>--<gray>").append(flag);
        }
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
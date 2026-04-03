package dev.corexinc.corex.engine.utils.debugging;

import dev.corexinc.corex.engine.utils.CorexLogger;

public class Debugger {
    public static boolean isDebugEnabled = true;

    public static void echoDebug(String message) {
        if (!isDebugEnabled) return;
        CorexLogger.info("<dark_gray>[<gray>Debug<dark_gray>] <white>" + message);
    }

    public static void echoError(String message) {
        CorexLogger.error("<red>ERROR: <white>" + message);
    }

    public static void echoTagFill(String originalTag, String filledValue) {
        if (!isDebugEnabled) return;
        echoDebug("<gray>Filled tag <<white>" + originalTag + "<gray>> with '<aqua>" + filledValue + "<gray>'.");
    }
}
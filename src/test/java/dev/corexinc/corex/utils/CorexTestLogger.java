package dev.corexinc.corex.utils;

public class CorexTestLogger {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String WHITE = "\u001B[37m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";

    private static final String COLOR_COREX = "\u001B[38;2;140;230;255m";   // <#8ce6ff>
    private static final String COLOR_INFO = "\u001B[38;2;66;182;245m";     // <#42b6f5>
    private static final String COLOR_SUCCESS = "\u001B[38;2;66;245;117m";  // <#42f575>
    private static final String COLOR_WARN = "\u001B[38;2;245;161;66m";     // <#f5a142>
    private static final String COLOR_ERROR = "\u001B[38;2;245;66;66m";     // <#f54242>

    private static final String PREFIX_INFO = COLOR_INFO + BOLD + "+> " + RESET + COLOR_COREX + "[Corex] " + WHITE;
    private static final String PREFIX_SUCCESS = COLOR_SUCCESS + BOLD + "v> " + RESET + COLOR_COREX + "[Corex] " + WHITE;
    private static final String PREFIX_WARN = COLOR_WARN + BOLD + "~> " + RESET + COLOR_COREX + "[Corex] " + YELLOW;
    private static final String PREFIX_ERROR = COLOR_ERROR + BOLD + "!> " + RESET + COLOR_COREX + "[Corex] " + RED;

    public static void info(String message) {
        System.out.println(PREFIX_INFO + message + RESET);
    }

    public static void success(String message) {
        System.out.println(PREFIX_SUCCESS + message + RESET);
    }

    public static void warn(String message) {
        System.out.println(PREFIX_WARN + message + RESET);
    }

    public static void error(String message) {
        System.out.println(PREFIX_ERROR + message + RESET);
    }
}
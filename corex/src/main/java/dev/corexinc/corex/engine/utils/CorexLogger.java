package dev.corexinc.corex.engine.utils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public class CorexLogger {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static Audience console = Audience.empty();

    private static final String PREFIX_INFO = "<#42b6f5><bold>+></bold> <#8ce6ff>[%s]</#8ce6ff> <white>";
    private static final String PREFIX_SUCCESS = "<#42f575><bold>v></bold> <#8ce6ff>[%s]</#8ce6ff> <white>";
    private static final String PREFIX_WARN = "<#f5a142><bold>~></bold> <#8ce6ff>[%s]</#8ce6ff> <yellow>";
    private static final String PREFIX_ERROR = "<#f54242><bold>!></bold> <#8ce6ff>[%s]</#8ce6ff> <red>";

    public static void setConsole(Audience audience) {
        if (audience == null) throw new IllegalArgumentException("console audience must not be null");
        console = audience;
    }

    public static void info(String message) {
        send(PREFIX_INFO + message, "Corex");
    }

    public static void success(String message) {
        send(PREFIX_SUCCESS + message, "Corex");
    }

    public static void warn(String message) {
        send(PREFIX_WARN + message, "Corex");
    }

    public static void error(String message) {
        send(PREFIX_ERROR + message, "Corex");
    }

    public static void info(@NotNull String sender, String message) {
        send(PREFIX_INFO + message, sender);
    }

    public static void success(@NotNull String sender, String message) {
        send(PREFIX_SUCCESS + message, sender);
    }

    public static void warn(@NotNull String sender, String message) {
        send(PREFIX_WARN + message, sender);
    }

    public static void error(@NotNull String sender, String message) {
        send(PREFIX_ERROR + message, sender);
    }

    private static void send(String format, String sender) {
        try {
            Component component = MINI_MESSAGE.deserialize(String.format(format, sender).replace("§", "&"));
            console.sendMessage(component);
        } catch (Exception e) {
            console.sendMessage(Component.text("(Raw) " + format));
        }
    }
}
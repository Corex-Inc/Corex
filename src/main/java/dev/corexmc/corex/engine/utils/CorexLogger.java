package dev.corexmc.corex.engine.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

public class CorexLogger {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // +>[Corex]
    private static final String PREFIX_INFO = "<#42b6f5><bold>+></bold> <#8ce6ff>[Corex]</#8ce6ff> <white>";
    // v> [Corex]
    private static final String PREFIX_SUCCESS = "<#42f575><bold>v></bold> <#8ce6ff>[Corex]</#8ce6ff> <white>";
    // ~> [Corex]
    private static final String PREFIX_WARN = "<#f5a142><bold>~></bold> <#8ce6ff>[Corex]</#8ce6ff> <yellow>";
    // !> [Corex]
    private static final String PREFIX_ERROR = "<#f54242><bold>!></bold> <#8ce6ff>[Corex]</#8ce6ff> <red>";

    public static void info(String message) {
        send(PREFIX_INFO + message);
    }

    public static void success(String message) {
        send(PREFIX_SUCCESS + message);
    }

    public static void warn(String message) {
        send(PREFIX_WARN + message);
    }

    public static void error(String message) {
        send(PREFIX_ERROR + message);
    }

    private static void send(String format) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        try {
            Component component = MINI_MESSAGE.deserialize(format);
            console.sendMessage(component);
        } catch (Exception e) {
            console.sendMessage("§b+> §3[Corex] §f(Raw) " + format.replaceAll("<[^>]*>", ""));
        }
    }
}
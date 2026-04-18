package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.api.commands.AbstractCommand;

import java.util.HashSet;
import java.util.Set;

public class CommandMetadata {

    public final AbstractCommand command;
    private final Set<String> allowedPrefixes = new HashSet<>();

    public CommandMetadata(AbstractCommand command) {
        this.command = command;
        parseSyntax(command.getSyntax());
    }

    private void parseSyntax(String syntax) {
        if (syntax == null || syntax.isEmpty()) return;

        String cleaned = syntax.replaceAll("[\\[\\]\\(\\)\\{\\}<>]", "");
        String[] parts = cleaned.split(" ");

        for (String part : parts) {
            if (part.isEmpty() || part.equals("-") || part.equalsIgnoreCase(command.getName())) continue;

            int colonIndex = part.indexOf(':');
            if (colonIndex > 0) {
                allowedPrefixes.add(part.substring(0, colonIndex).toLowerCase());
            }
        }
    }

    public boolean isAllowedPrefix(String prefix) {
        String lower = prefix.toLowerCase();

        if (command.getName().equals("run") && lower.startsWith("def.")) {
            return true;
        }

        return allowedPrefixes.contains(lower);
    }
}
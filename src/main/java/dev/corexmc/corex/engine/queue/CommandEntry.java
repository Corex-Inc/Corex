package dev.corexmc.corex.engine.queue;

import dev.corexmc.corex.engine.tags.TagParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandEntry {

    private final String commandName;

    private final List<String> linearArgs = new ArrayList<>();
    private final Map<String, String> prefixArgs = new HashMap<>();

    public CommandEntry(String rawLine) {
        List<String> tokens = tokenize(rawLine.trim());

        if (tokens.isEmpty()) {
            this.commandName = "unknown";
            return;
        }

        this.commandName = tokens.getFirst().toLowerCase();

        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            int colonIndex = token.indexOf(':');

            if (colonIndex > 0 && !token.substring(0, colonIndex).contains("<")) {
                String prefix = token.substring(0, colonIndex).toLowerCase();
                String value = token.substring(colonIndex + 1);
                prefixArgs.put(prefix, value);
            } else {
                linearArgs.add(token);
            }
        }
    }

    private List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int tagDepth = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == '<') tagDepth++;
            if (c == '>') tagDepth--;

            if (c == ' ' && !inQuotes && tagDepth == 0) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getPrefix(String prefix, ScriptQueue queue) {
        String raw = prefixArgs.get(prefix.toLowerCase());
        if (raw == null) return null;
        return TagParser.parse(raw).evaluate(queue);
    }

    public String getLinear(int index, ScriptQueue queue) {
        if (index < 0 || index >= linearArgs.size()) return null;
        String raw = linearArgs.get(index);
        return TagParser.parse(raw).evaluate(queue);
    }

    public boolean hasFlag(String flag) {
        for (String arg : linearArgs) {
            if (arg.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }
}
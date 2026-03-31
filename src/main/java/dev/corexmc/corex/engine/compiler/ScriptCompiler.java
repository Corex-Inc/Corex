package dev.corexmc.corex.engine.compiler;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.engine.utils.CorexLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptCompiler {

    public static Instruction compile(String rawLine) {
        List<String> tokens = tokenize(rawLine.trim());
        if (tokens.isEmpty()) return null;

        String cmdName = tokens.getFirst().toLowerCase();

        dev.corexmc.corex.engine.registry.CommandMetadata meta =
                Corex.getInstance().getRegistry().getScriptCommands().getMetadata(cmdName);

        if (meta == null) {
            CorexLogger.error("SCRIPT ERROR: Unknown script command '<yellow>" + cmdName + "<yellow/>'!");
            return null;
        }

        List<CompiledArgument> linearArgs = new ArrayList<>();
        Map<String, CompiledArgument> prefixArgs = new HashMap<>();
        List<String> flags = new ArrayList<>();

        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            int colonIndex = token.indexOf(':');

            if (colonIndex > 0) {
                String potentialPrefix = token.substring(0, colonIndex);

                if (meta.isAllowedPrefix(potentialPrefix)) {
                    String value = token.substring(colonIndex + 1);
                    prefixArgs.put(potentialPrefix.toLowerCase(), parseArg(value));
                    continue;
                }
            }

            linearArgs.add(parseArg(token));
            if (!token.contains("<")) flags.add(token.toLowerCase());
        }

        return new Instruction(meta.command, linearArgs.toArray(new CompiledArgument[0]), prefixArgs, flags.toArray(new String[0]));
    }

    private static CompiledArgument parseArg(String text) {
        if (!text.contains("<")) {
            return new CompiledArgument.Static(text);
        }

        if (text.startsWith("<") && text.endsWith(">") && text.indexOf('<', 1) == -1) {
            String rawTag = text.substring(1, text.length() - 1);

            dev.corexmc.corex.engine.registry.FormatRegistry formats =
                    dev.corexmc.corex.Corex.getInstance().getRegistry().getFormats();

            if (formats.isFormat(rawTag)) {
                String formatValue = formats.get(rawTag).identify();
                return new CompiledArgument.Static(formatValue);
            }

            TagNode[] nodes = parseTagNodes(rawTag);
            return new CompiledArgument.PreSlicedDynamic(nodes);
        }
        return new CompiledArgument.Dynamic(text);
    }

    public static TagNode[] parseTagNodes(String rawTag) {
        List<TagNode> nodes = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        StringBuilder param = new StringBuilder();
        int bracketDepth = 0;

        for (char c : rawTag.toCharArray()) {
            if (c == '[') {
                bracketDepth++;
                if (bracketDepth == 1) continue;
            } else if (c == ']') {
                bracketDepth--;
                if (bracketDepth == 0) continue;
            } else if (c == '.' && bracketDepth == 0) {
                nodes.add(new TagNode(name.toString(), param.isEmpty() ? null : parseArg(param.toString())));
                name.setLength(0);
                param.setLength(0);
                continue;
            }

            if (bracketDepth > 0) {
                param.append(c);
            } else {
                name.append(c);
            }
        }
        nodes.add(new TagNode(name.toString(), param.isEmpty() ? null : parseArg(param.toString())));

        return nodes.toArray(new TagNode[0]);
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int tagDepth = 0;
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) { current.append('\\').append(c); escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (c == '<') tagDepth++;
            if (c == '>') tagDepth--;

            if (c == ' ' && !inQuotes && tagDepth == 0) {
                if (!current.isEmpty()) { tokens.add(current.toString()); current.setLength(0); }
            } else { current.append(c); }
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }
}
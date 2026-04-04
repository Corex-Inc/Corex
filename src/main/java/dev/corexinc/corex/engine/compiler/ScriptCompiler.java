package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.registry.CommandMetadata;
import dev.corexinc.corex.engine.registry.FormatRegistry;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptCompiler {

    public static Instruction compile(String rawLine, Instruction[] innerBlock) {
        List<String> tokens = tokenize(rawLine.trim());
        if (tokens.isEmpty()) return null;

        String cmdName = tokens.getFirst().toLowerCase();

        boolean isWaitable = false;
        if (cmdName.startsWith("~")) {
            isWaitable = true;
            cmdName = cmdName.substring(1);
        }

        CommandMetadata meta =
                Corex.getInstance().getRegistry().getScriptCommands().getMetadata(cmdName);

        if (meta == null) {
            CorexLogger.error("SCRIPT ERROR: Unknown script command '<yellow>" + cmdName + "</yellow>'!");
            return null;
        }

        List<CompiledArgument> linearArgs = new ArrayList<>();
        Map<String, CompiledArgument> prefixArgs = new HashMap<>();
        List<String> flags = new ArrayList<>();

        Map<AbstractGlobalFlag, CompiledArgument> gFlags = new HashMap<>();

        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            int colonIndex = token.indexOf(':');

            if (colonIndex > 0) {
                String potentialPrefix = token.substring(0, colonIndex);

                AbstractGlobalFlag gFlag = Corex.getInstance().getRegistry().getGlobalFlag(potentialPrefix);

                if (gFlag != null) {
                    gFlags.put(gFlag, parseArg(token.substring(colonIndex + 1)));
                    continue;
                }

                if (meta.isAllowedPrefix(potentialPrefix)) {
                    String value = token.substring(colonIndex + 1);
                    prefixArgs.put(potentialPrefix.toLowerCase(), parseArg(value));
                    continue;
                }
            }

            CompiledArgument compiled = parseArg(token);
            linearArgs.add(compiled);
            if (compiled instanceof CompiledArgument.Static) flags.add(token.toLowerCase());
        }

        int argsCount = linearArgs.size();
        if (argsCount < meta.command.getMinArgs() || (meta.command.getMaxArgs() != -1 && argsCount > meta.command.getMaxArgs())) {
            CorexLogger.error("COMPILE ERROR: Command '" + cmdName + "' expect from "
                    + meta.command.getMinArgs() + " to " + meta.command.getMaxArgs()
                    + " args, but provided " + argsCount + "!");
            CorexLogger.error("-> Line: " + rawLine);
            return null;
        }

        return new Instruction(meta.command, linearArgs.toArray(new CompiledArgument[0]), prefixArgs, flags.toArray(new String[0]), innerBlock, isWaitable, gFlags);
    }

    public static Instruction compile(String rawLine) {
        return compile(rawLine, null);
    }

    public static CompiledArgument parseArg(String text) {
        if (!text.contains("<")) return new CompiledArgument.Static(text);

        List<CompiledArgument> parts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int tagDepth = 0;
        boolean escaped = false;
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            if (escaped) {
                if (tagDepth > 0) buffer.append('\\');
                buffer.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') { escaped = true; continue; }

            if (c == '<') {
                if (tagDepth == 0) {
                    if (!buffer.isEmpty()) {
                        parts.add(new CompiledArgument.Static(buffer.toString()));
                        buffer.setLength(0);
                    }
                } else buffer.append(c);
                tagDepth++;
            } else if (c == '>') {
                tagDepth--;
                if (tagDepth == 0) {
                    parts.add(compileSingleTag(buffer.toString()));
                    buffer.setLength(0);
                } else buffer.append(c);
            } else buffer.append(c);
        }
        if (!buffer.isEmpty()) parts.add(new CompiledArgument.Static(buffer.toString()));

        if (parts.size() == 1) return parts.getFirst();

        return new CompiledArgument.Mixed(parts.toArray(new CompiledArgument[0]));
    }

    private static CompiledArgument compileSingleTag(String rawTag) {
        String mainTag = rawTag;
        CompiledArgument fallback = null;

        int pipeIndex = rawTag.indexOf("||");
        if (pipeIndex >= 0) {
            mainTag = rawTag.substring(0, pipeIndex);
            fallback = parseArg(rawTag.substring(pipeIndex + 2));
        }

        TagNode[] nodes = parseTagNodes(mainTag);

        if (nodes.length == 1 && fallback == null) {
            FormatRegistry formats = Corex.getInstance().getRegistry().getFormats();
            AbstractFormatter fmt = formats.get(nodes[0].name);
            if (fmt != null) {
                if (nodes[0].param == null || nodes[0].param instanceof CompiledArgument.Static) {
                    Attribute mockAttr = new Attribute(nodes, null);
                    AbstractTag result = fmt.parse(mockAttr);
                    return new CompiledArgument.Static(result.identify());
                }
            }
        }

        return new CompiledArgument.PreSlicedDynamic(nodes, fallback, mainTag);
    }

    public static TagNode[] parseTagNodes(String rawTag) {
        List<TagNode> nodes = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        StringBuilder param = new StringBuilder();
        int bracketDepth = 0;
        int len = rawTag.length();

        for (int i = 0; i < len; i++) {
            char c = rawTag.charAt(i);

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

            if (bracketDepth > 0) param.append(c);
            else name.append(c);
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
        int len = line.length();

        for (int i = 0; i < len; i++) {
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
package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.compiler.args.MathArg;
import dev.corexinc.corex.engine.compiler.args.MixedArg;
import dev.corexinc.corex.engine.compiler.args.PreSlicedDynamicArg;
import dev.corexinc.corex.engine.compiler.args.StaticArg;
import dev.corexinc.corex.engine.compiler.math.MathCompiler;
import dev.corexinc.corex.engine.registry.CommandMetadata;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.tags.core.ElementTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptCompiler {

    private static boolean isTagStart(String str, int index) {
        if (str.charAt(index) != '<') return false;
        if (index + 1 >= str.length()) return false;
        char next = str.charAt(index + 1);
        return Character.isLetter(next) || next == '_' || next == '[' || next == '#' || next == '&';
    }

    public static Instruction compile(String rawLine, Instruction[] innerBlock) {
        List<String> tokens = tokenize(rawLine.trim());
        if (tokens.isEmpty()) return null;

        String cmdName = tokens.getFirst().toLowerCase();

        boolean isWaitable = false;
        if (cmdName.startsWith("~")) {
            isWaitable = true;
            cmdName = cmdName.substring(1);
        }

        CommandMetadata meta = Corex.getInstance().getRegistry().getScriptCommands().getMetadata(cmdName);

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
                    prefixArgs.put(potentialPrefix.toLowerCase(), parseArg(token.substring(colonIndex + 1)));
                    continue;
                }
            }

            CompiledArgument compiled = parseArg(token);
            linearArgs.add(compiled);

            if (compiled instanceof StaticArg && token.matches("^[a-zA-Z_]+$")) {
                flags.add(token.toLowerCase());
            }
        }

        int argsCount = linearArgs.size();
        if (argsCount < meta.command.getMinArgs() || (meta.command.getMaxArgs() != -1 && argsCount > meta.command.getMaxArgs())) {
            CorexLogger.error("COMPILE ERROR: Command '" + cmdName + "' expect from " + meta.command.getMinArgs() + " to " + meta.command.getMaxArgs() + " args, but provided " + argsCount + "!");
            CorexLogger.error("-> Line: " + rawLine);
            return null;
        }

        return new Instruction(meta.command, linearArgs.toArray(new CompiledArgument[0]), prefixArgs, flags.toArray(new String[0]), innerBlock, isWaitable, gFlags);
    }

    public static Instruction compile(String rawLine) {
        return compile(rawLine, null);
    }

    public static CompiledArgument parseArg(String text) {
        if (text.startsWith("(") && text.endsWith(")")) {
            try {
                return new MathArg(MathCompiler.compile(text), text);
            } catch (Exception e) {
                return new StaticArg(new ElementTag(text));
            }
        }

        if (!text.contains("<")) return new StaticArg(unescape(text));

        List<CompiledArgument> parts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        List<Integer> scope = new ArrayList<>();
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                buffer.append('\\').append(c);
                escaped = false; continue;
            }
            if (c == '\\') { escaped = true; continue; }

            int top = scope.isEmpty() ? -1 : scope.getLast();

            if (c == '<' && isTagStart(text, i)) {
                if (scope.isEmpty()) {
                    if (!buffer.isEmpty()) {
                        parts.add(new StaticArg(unescape(buffer.toString())));
                        buffer.setLength(0);
                    }
                    scope.add(0);
                    continue;
                } else {
                    scope.add(0);
                }
            } else if (c == '>') {
                if (top == 0) {
                    scope.removeLast();
                    if (scope.isEmpty()) {
                        parts.add(compileSingleTag(buffer.toString()));
                        buffer.setLength(0);
                        continue;
                    }
                }
            } else if (c == '[') {
                if (top == 0) scope.add(1);
            } else if (c == ']') {
                if (top == 1) scope.removeLast();
            }

            buffer.append(c);
        }
        if (!buffer.isEmpty()) parts.add(new StaticArg(unescape(buffer.toString())));
        return parts.size() == 1 ? parts.getFirst() : new MixedArg(parts.toArray(new CompiledArgument[0]));
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
            var formats = Corex.getInstance().getRegistry().getFormats();
            if (formats.isFormat(nodes[0].name())) {
                if (nodes[0].param() == null || nodes[0].param() instanceof StaticArg) {
                    Attribute mockAttr = new Attribute(nodes, null);
                    AbstractTag result = formats.get(nodes[0].name()).parse(mockAttr);
                    return new StaticArg(result);
                }
            }
        }
        return new PreSlicedDynamicArg(nodes, fallback, rawTag);
    }

    public static TagNode[] parseTagNodes(String rawTag) {
        List<TagNode> nodes = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        StringBuilder param = new StringBuilder();
        int bracketDepth = 0;
        int len = rawTag.length();
        boolean escaped = false;

        for (int i = 0; i < len; i++) {
            char c = rawTag.charAt(i);
            if (escaped) {
                if (bracketDepth > 0) param.append('\\').append(c);
                else name.append(c);
                escaped = false; continue;
            }
            if (c == '\\') { escaped = true; continue; }

            if (c == '[') { bracketDepth++; if (bracketDepth == 1) continue; }
            else if (c == ']') { bracketDepth--; if (bracketDepth == 0) continue; }
            else if (c == '.' && bracketDepth == 0) {
                nodes.add(new TagNode(name.toString(), param.isEmpty() ? null : parseArg(param.toString())));
                name.setLength(0); param.setLength(0); continue;
            }

            if (bracketDepth > 0) param.append(c); else name.append(c);
        }
        nodes.add(new TagNode(name.toString(), param.isEmpty() ? null : parseArg(param.toString())));
        return nodes.toArray(new TagNode[0]);
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        List<Integer> scope = new ArrayList<>();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escaped) {
                current.append('\\').append(c);
                escaped = false; continue;
            }
            if (c == '\\') { escaped = true; continue; }

            int top = scope.isEmpty() ? -1 : scope.getLast();

            if (c == ' ' && !inQuotes && scope.isEmpty()) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (c == '<' && isTagStart(line, i)) scope.add(0);
            else if (c == '>') { if (top == 0) scope.removeLast(); }
            else if (c == '[') { if (top == 0) scope.add(1); }
            else if (c == ']') { if (top == 1) scope.removeLast(); }
            else if (c == '(') { if (scope.isEmpty() || top == 2) scope.add(2); }
            else if (c == ')') { if (top == 2) scope.removeLast(); }

            if (c == '"') {
                if (scope.isEmpty()) {
                    inQuotes = !inQuotes;
                    continue;
                }
            }

            current.append(c);
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }

    public static String unescape(String str) {
        if (str == null || !str.contains("\\")) return str;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (char c : str.toCharArray()) {
            if (!escaped && c == '\\') escaped = true;
            else { sb.append(c); escaped = false; }
        }
        return sb.toString();
    }
}
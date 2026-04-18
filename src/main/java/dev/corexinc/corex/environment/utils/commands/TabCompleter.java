package dev.corexinc.corex.environment.utils.commands;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.registry.CommandMetadata;
import dev.corexinc.corex.engine.registry.ScriptCommandRegistry;
import dev.corexinc.corex.engine.tags.TagManager;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class TabCompleter {

    private static final Map<Class<? extends AbstractTag>, TagProcessor<?>> PROCESSOR_CACHE = new HashMap<>();

    public static List<String> getSuggestions(String[] args) {
        if (args.length == 0) return filterCommands("");

        String input = String.join(" ", args);
        boolean trailingSpace = input.endsWith(" ");
        String rawToken = trailingSpace ? "" : args[args.length - 1];

        String currentCmdStr = isolateCurrentCommand(input);
        List<String> tokens = tokenizeForCompletion(currentCmdStr);

        if (trailingSpace) tokens.add("");
        if (tokens.isEmpty()) return filterCommands("");
        if (tokens.size() == 1) return filterCommands(tokens.getFirst());

        String cmdName = tokens.getFirst().toLowerCase();
        CommandMetadata meta = Corex.getInstance().getRegistry().getScriptCommands().getMetadata(cmdName);

        if (meta == null) return new ArrayList<>();

        List<String> suggestions = new ArrayList<>();

        if (!rawToken.contains("<")) {
            for (String token : meta.command.getSyntax().split(" ")) {
                String clean = token.replaceAll("[]\\[(){}<>]", "");
                if (clean.isEmpty()) continue;

                int colonIndex = clean.indexOf(':');
                if (colonIndex > 0) {
                    // keyed arg like "naturally:" or "max_delay_ms:"
                    String cleanPrefix = clean.substring(0, colonIndex + 1);
                    if (cleanPrefix.toLowerCase().startsWith(rawToken.toLowerCase())) {
                        suggestions.add(cleanPrefix);
                    }
                } else if (!clean.startsWith("<") && !clean.contains("|")) {
                    // bare flag like "delayed" or "no_physics"
                    if (clean.toLowerCase().startsWith(rawToken.toLowerCase())) {
                        suggestions.add(clean);
                    }
                }
            }

            for (String gFlag : Corex.getInstance().getRegistry().getGlobalFlagsNames()) {
                String prefix = gFlag + ":";
                if (prefix.toLowerCase().startsWith(rawToken.toLowerCase())) {
                    suggestions.add(prefix);
                }
            }
        }

        if (rawToken.contains("<")) {
            int lastTagStart = rawToken.lastIndexOf('<');
            String baseToken = rawToken.substring(0, lastTagStart + 1);
            String tagContent = rawToken.substring(lastTagStart + 1);

            int lastDot = tagContent.lastIndexOf('.');
            if (lastDot == -1) {
                for (String fmt : Corex.getInstance().getRegistry().getFormats().getAllFormatNames()) {
                    if (fmt.toLowerCase().startsWith(tagContent.toLowerCase())) suggestions.add(baseToken + fmt);
                }
                for (String baseTag : TagManager.getBaseTagNames()) {
                    if (baseTag.toLowerCase().startsWith(tagContent.toLowerCase())) suggestions.add(baseToken + baseTag);
                }
            } else {
                String previousParts = tagContent.substring(0, lastDot);
                String currentAttr = tagContent.substring(lastDot + 1);

                List<String> deepAttrs = getDeepTagSuggestions(previousParts);
                for (String attr : deepAttrs) {
                    if (attr.toLowerCase().startsWith(currentAttr.toLowerCase())) {
                        suggestions.add(baseToken + previousParts + "." + attr);
                    }
                }
            }
        }

        return suggestions;
    }

    private static List<String> getDeepTagSuggestions(String previousParts) {
        List<String> suggestions = new ArrayList<>();
        String[] parts = previousParts.split("\\.");
        if (parts.length == 0) return suggestions;

        String rawBase = parts[0];
        String baseName = rawBase.replaceAll("\\[.*?\\]", "");

        List<Class<? extends AbstractTag>> currentClasses = new ArrayList<>();

        if (rawBase.startsWith("[")) {
            currentClasses.addAll(Corex.getInstance().getRegistry().getRegisteredTagClasses());

            currentClasses.add(ElementTag.class);
            currentClasses.add(ComponentTag.class);
            currentClasses.add(PlayerTag.class);
        } else if (Corex.getInstance().getRegistry().getFormats().isFormat(baseName)) {
            Class<? extends AbstractTag> elementClass = getTagClassByName("element");
            if (elementClass != null) currentClasses.add(elementClass);
        } else {
            Class<? extends AbstractTag> clazz = getTagClassByName(baseName);
            if (clazz != null) currentClasses.add(clazz);
        }

        if (currentClasses.isEmpty()) return suggestions;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].replaceAll("\\[.*?\\]", "").toLowerCase();
            List<Class<? extends AbstractTag>> nextClasses = new ArrayList<>();

            for (Class<? extends AbstractTag> clazz : currentClasses) {
                TagProcessor<?> processor = getProcessor(clazz);
                if (processor != null) {
                    TagProcessor.TagData<?> data = processor.getRegisteredTags().get(part);
                    if (data != null && data.returnType != null) {
                        nextClasses.add(data.returnType);
                    }
                }
            }

            if (nextClasses.isEmpty()) return suggestions;
            currentClasses = nextClasses;
        }

        for (Class<? extends AbstractTag> clazz : currentClasses) {
            TagProcessor<?> processor = getProcessor(clazz);
            if (processor != null) {
                suggestions.addAll(processor.getRegisteredTags().keySet());
            }
        }

        return suggestions.stream().distinct().toList();
    }

    private static Class<? extends AbstractTag> getTagClassByName(String name) {
        String search = name.toLowerCase().replace("tag", "");

        if (search.contains("@")) {
            search = search.substring(0, search.indexOf('@'));
        }

        for (Class<? extends AbstractTag> clazz : Corex.getInstance().getRegistry().getRegisteredTagClasses()) {
            String className = clazz.getSimpleName().toLowerCase();
            if (className.replace("tag", "").equals(search) || className.startsWith(search)) {
                return clazz;
            }
        }

        return switch (search) {
            case "player", "p" -> PlayerTag.class;
            case "element", "el", "format" -> ElementTag.class;
            case "component", "c" -> ComponentTag.class;
            default -> null;
        };
    }

    private static TagProcessor<?> getProcessor(Class<? extends AbstractTag> clazz) {
        if (PROCESSOR_CACHE.containsKey(clazz)) return PROCESSOR_CACHE.get(clazz);

        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().equals(TagProcessor.class) && Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    TagProcessor<?> proc = (TagProcessor<?>) field.get(null);
                    PROCESSOR_CACHE.put(clazz, proc);
                    return proc;
                }
            }
        } catch (Exception ignored) {}

        PROCESSOR_CACHE.put(clazz, null);
        return null;
    }

    private static String isolateCurrentCommand(String input) {
        Stack<Integer> boundaries = new Stack<>();
        boundaries.push(0);

        int tagDepth = 0;
        int mathDepth = 0;
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }

            if (c == '<') tagDepth++;
            if (c == '>') tagDepth--;
            if (c == '(' && tagDepth == 0) mathDepth++;
            if (c == ')' && tagDepth == 0) mathDepth--;
            if (c == '"' && tagDepth == 0 && mathDepth == 0) inQuotes = !inQuotes;

            if (!inQuotes && tagDepth == 0 && mathDepth == 0) {
                if (c == '{') boundaries.push(i + 1);
                else if (c == '}') { if (boundaries.size() > 1) boundaries.pop(); }
                else if (c == '-') {
                    boolean spaceBefore = (i == 0 || Character.isWhitespace(input.charAt(i - 1)));
                    boolean spaceAfter = (i + 1 == input.length() || Character.isWhitespace(input.charAt(i + 1)));
                    if (spaceBefore && spaceAfter) { boundaries.pop(); boundaries.push(i + 1); }
                }
            }
        }
        return input.substring(boundaries.peek()).trim();
    }

    private static List<String> tokenizeForCompletion(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int tagDepth = 0;
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) { current.append(c); escaped = false; continue; }
            if (c == '\\') { escaped = true; current.append(c); continue; }
            if (c == '<') tagDepth++;
            if (c == '>') tagDepth--;
            if (c == '"' && tagDepth == 0) { inQuotes = !inQuotes; current.append(c); continue; }

            if (c == ' ' && !inQuotes && tagDepth == 0) {
                if (!current.isEmpty()) { tokens.add(current.toString()); current.setLength(0); }
            } else current.append(c);
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }

    private static List<String> filterCommands(String prefix) {
        List<String> suggestions = new ArrayList<>();
        for (String cmd : ScriptCommandRegistry.getCommands().keySet()) {
            if (cmd.toLowerCase().startsWith(prefix.toLowerCase())) suggestions.add(cmd);
        }
        return suggestions;
    }
}
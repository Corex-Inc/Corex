package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.api.commands.AbstractCommand;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommandMetadata {

    public final AbstractCommand command;

    private final Set<String> requiredPrefixes = new LinkedHashSet<>();
    private final Set<String> allowedPrefixes  = new LinkedHashSet<>();
    private final List<SyntaxSlot> slots;
    public final int syntaxRequiredLinear;

    /**
     * Set when the command declares a {@code run} overload taking resolved arguments
     * instead of the raw instruction. Null means the command uses the classic form.
     */
    public final BoundCommand bound;

    public CommandMetadata(AbstractCommand command) {
        this.command = command;

        String syntax = command.getSyntax();
        this.slots = syntax.isBlank() ? List.of() : parseSyntax(syntax, command.getName());

        int requiredLinear = 0;
        for (SyntaxSlot slot : slots) {
            switch (slot.kind()) {
                case LINEAR -> { if (slot.required()) requiredLinear++; }
                case PREFIX -> {
                    allowedPrefixes.add(slot.name());
                    if (slot.required()) requiredPrefixes.add(slot.name());
                }
                case FLAG -> {}
            }
        }
        this.syntaxRequiredLinear = requiredLinear;
        this.bound = BoundCommand.bind(command, slots);
    }

    public List<SyntaxSlot> getSlots() {
        return slots;
    }

    public List<String> getMissingRequiredPrefixes(Set<String> providedPrefixes) {
        List<String> missing = new ArrayList<>();
        for (String req : requiredPrefixes) {
            if (!providedPrefixes.contains(req)) missing.add(req);
        }
        return missing;
    }

    public boolean isSyntaxLinearSatisfied(int providedLinear) {
        return providedLinear >= syntaxRequiredLinear;
    }

    public boolean isArgCountValid(int linearCount, int prefixCount) {
        int total = linearCount + prefixCount;
        int min   = command.getMinArgs();
        int max   = command.getMaxArgs();
        if (total < min) return false;
        if (max != -1 && total > max) return false;
        return true;
    }

    public boolean isAllowedPrefix(String prefix) {
        if (allowedPrefixes.contains(prefix)) return true;

        int dot = prefix.indexOf('.');
        if (dot > 0) {
            String family = prefix.substring(0, dot + 1);
            for (String allowed : allowedPrefixes) {
                if (allowed.startsWith(family)) return true;
            }
        }
        return false;
    }

    private static List<SyntaxSlot> parseSyntax(String syntax, String commandName) {
        List<SyntaxSlot> parsed = new ArrayList<>();
        int linearIndex = 0;

        for (String token : tokenize(syntax)) {
            if (token.isBlank()) continue;
            if (token.equalsIgnoreCase(commandName)) continue;

            boolean isMandatory = token.startsWith("[") && token.endsWith("]");
            boolean isOptional  = token.startsWith("(") && token.endsWith(")");

            if (!isMandatory && !isOptional) continue;

            String inner = token.substring(1, token.length() - 1).trim();

            String defaultRaw = null;
            int equals = indexOfTopLevel(inner, '=');
            if (equals >= 0) {
                defaultRaw = inner.substring(equals + 1).trim();
                inner = inner.substring(0, equals).trim();
            }

            int colon = indexOfTopLevel(inner, ':');
            if (colon > 0 && !inner.startsWith("<")) {
                parsed.add(new SyntaxSlot(SyntaxSlot.Kind.PREFIX,
                        inner.substring(0, colon).trim(), -1, isMandatory, defaultRaw));
                continue;
            }

            if (inner.indexOf('<') < 0) {
                parsed.add(new SyntaxSlot(SyntaxSlot.Kind.FLAG,
                        inner.trim(), -1, false, defaultRaw));
                continue;
            }

            parsed.add(new SyntaxSlot(SyntaxSlot.Kind.LINEAR,
                    linearName(inner, linearIndex), linearIndex, isMandatory, defaultRaw));
            linearIndex++;
        }

        return List.copyOf(parsed);
    }

    /**
     * Pulls a readable name out of the first {@code <...>} group. Placeholders such as
     * {@code <#>} or {@code <#.#>} carry no name, so those fall back to a positional one.
     */
    private static String linearName(String inner, int index) {
        int open = inner.indexOf('<');
        int close = inner.indexOf('>', open + 1);
        String candidate = open >= 0 && close > open ? inner.substring(open + 1, close).trim() : "";

        if (!candidate.isEmpty() && Character.isJavaIdentifierStart(candidate.charAt(0))) {
            boolean clean = true;
            for (int i = 1; i < candidate.length(); i++) {
                if (!Character.isJavaIdentifierPart(candidate.charAt(i))) { clean = false; break; }
            }
            if (clean) return candidate;
        }
        return "arg" + index;
    }

    /** Finds a character that sits outside any {@code <...>} group. */
    private static int indexOfTopLevel(String text, char target) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == target && depth == 0) return i;
        }
        return -1;
    }

    private static List<String> tokenize(String syntax) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < syntax.length(); i++) {
            char c = syntax.charAt(i);

            if (c == '[' || c == '(' || c == '<') depth++;
            else if (c == ']' || c == ')' || c == '>') depth--;

            if (Character.isWhitespace(c) && depth == 0) {
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
}
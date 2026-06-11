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
    public final int syntaxRequiredLinear;

    public CommandMetadata(AbstractCommand command) {
        this.command = command;

        String syntax = command.getSyntax();
        if (!syntax.isBlank()) {
            ParsedSyntax parsed = parseSyntax(syntax, command.getName());
            this.syntaxRequiredLinear = parsed.requiredLinear;
            this.requiredPrefixes.addAll(parsed.requiredPrefixes);
            this.allowedPrefixes.addAll(parsed.allowedPrefixes);
        } else {
            this.syntaxRequiredLinear = 0;
        }
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

    private record ParsedSyntax(
            int requiredLinear,
            Set<String> requiredPrefixes,
            Set<String> allowedPrefixes
    ) {}

    private static ParsedSyntax parseSyntax(String syntax, String commandName) {
        int requiredLinear = 0;
        Set<String> required = new LinkedHashSet<>();
        Set<String> allowed  = new LinkedHashSet<>();

        for (String token : tokenize(syntax)) {
            if (token.isBlank()) continue;
            if (token.equalsIgnoreCase(commandName)) continue;

            boolean isMandatory = token.startsWith("[") && token.endsWith("]");
            boolean isOptional  = token.startsWith("(") && token.endsWith(")");

            if (!isMandatory && !isOptional) continue;

            String inner = token.substring(1, token.length() - 1).trim();

            int colon = inner.indexOf(':');
            boolean isPrefix = colon > 0 && !inner.startsWith("<");

            if (isPrefix) {
                String prefix = inner.substring(0, colon).trim();
                allowed.add(prefix);
                if (isMandatory) required.add(prefix);
            } else {
                if (isMandatory) requiredLinear++;
            }
        }

        return new ParsedSyntax(requiredLinear, required, allowed);
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
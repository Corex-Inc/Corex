package dev.corexinc.corex.environment.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventPattern {
    private final Pattern compiledPattern;
    private final List<String> argNames;

    public EventPattern(String syntax) {
        this.argNames = new ArrayList<>();
        String regex = syntax.trim();

        StringBuilder alternationsFixed = new StringBuilder();
        for (String word : regex.split("\\s+")) {
            if (word.contains("|") && !word.contains("<")) {
                alternationsFixed.append("(?:").append(word).append(") ");
            } else {
                alternationsFixed.append(word).append(" ");
            }
        }
        regex = alternationsFixed.toString().trim();

        Matcher optMatcher = Pattern.compile("\\s*\\((?!\\?:)([^)]+)\\)").matcher(regex);
        StringBuilder optSb = new StringBuilder();
        while (optMatcher.find()) {
            optMatcher.appendReplacement(optSb, "(?:\\\\s+" + optMatcher.group(1) + ")?");
        }
        optMatcher.appendTail(optSb);
        regex = optSb.toString();

        Matcher m = Pattern.compile("<([a-zA-Z0-9_]+)>").matcher(regex);
        StringBuilder sb = new StringBuilder();

        while (m.find()) {
            argNames.add(m.group(1).toLowerCase());
            m.appendReplacement(sb, "([a-zA-Z0-9_:\\\\*\\\\-]+)");
        }

        m.appendTail(sb);
        regex = sb.toString();

        regex = regex.replace(" ", "\\s+");
        regex = regex.replace("\\s+\\s+", "\\s+");

        this.compiledPattern = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);
    }

    public Map<String, List<String>> match(String rawLine) {
        Matcher m = compiledPattern.matcher(rawLine.trim());
        if (!m.matches()) return null;

        Map<String, List<String>> args = new HashMap<>();
        for (int i = 1; i <= m.groupCount(); i++) {
            String val = m.group(i);
            if (val != null && (i - 1) < argNames.size()) {
                String name = argNames.get(i - 1);
                args.computeIfAbsent(name, k -> new ArrayList<>()).add(val);
            }
        }
        return args;
    }
}
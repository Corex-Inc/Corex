package dev.corexmc.corex.engine.scripts;

import java.util.List;

public class ScriptPreprocessor {

    public static String preprocess(List<String> rawLines) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        boolean inBlockComment = false;

        for (String line : rawLines) {
            if (inBlockComment) {
                if (line.contains("*/")) {
                    inBlockComment = false;
                    line = line.substring(line.indexOf("*/") + 2);
                } else continue;
            }
            if (line.contains("/*")) {
                if (line.contains("*/")) line = line.replaceAll("/\\*.*?\\*/", "");
                else {
                    inBlockComment = true;
                    line = line.substring(0, line.indexOf("/*"));
                }
            }
            if (line.contains("//")) line = line.substring(0, line.indexOf("//"));

            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.startsWith("- ") || trimmed.matches("^[a-zA-Z0-9_]+:.*")) {
                flushLine(result, currentLine);
                currentLine.append(line);
            } else {
                currentLine.append(trimmed);
            }
        }
        flushLine(result, currentLine);

        return result.toString();
    }

    private static void flushLine(StringBuilder result, StringBuilder currentLine) {
        if (currentLine.isEmpty()) return;

        String line = currentLine.toString();
        int dashIndex = line.indexOf("- ");

        if (dashIndex != -1 && line.substring(0, dashIndex).trim().isEmpty()) {
            String spaces = line.substring(0, dashIndex + 2);
            String content = line.substring(dashIndex + 2);

            result.append(spaces).append("'").append(content.replace("'", "''")).append("'\n");
        } else {
            result.append(line).append("\n");
        }
        currentLine.setLength(0);
    }
}
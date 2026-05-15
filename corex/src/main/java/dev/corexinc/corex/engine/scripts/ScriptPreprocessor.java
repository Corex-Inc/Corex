package dev.corexinc.corex.engine.scripts;

import java.util.List;

public class ScriptPreprocessor {

    public static String preprocess(List<String> rawLines) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        boolean inBlockComment = false;

        for (String line : rawLines) {

            line = line.replace("\t", "    ");

            if (inBlockComment) {
                int endIdx = line.indexOf("*/");
                if (endIdx == -1) continue;
                inBlockComment = false;
                line = line.substring(endIdx + 2);
            }

            line = stripInlineBlockComments(line);

            int blockStart = line.indexOf("/*");
            if (blockStart != -1) {
                inBlockComment = true;
                line = line.substring(0, blockStart);
            }

            line = stripLineComment(line);

            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            boolean isNewYamlLine = trimmed.startsWith("- ")
                    || trimmed.endsWith(":")
                    || (trimmed.contains(": ") && !trimmed.startsWith("<"));

            if (isNewYamlLine) {
                flushLine(result, currentLine, line);
                currentLine.append(line);
            } else {
                currentLine.append(trimmed);
            }
        }

        flushLine(result, currentLine, null);
        return result.toString();
    }

    private static String stripInlineBlockComments(String line) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '*') {
                int end = line.indexOf("*/", i + 2);
                if (end != -1) {
                    i = end + 2;
                } else {
                    sb.append(line, i, line.length());
                    break;
                }
            } else {
                sb.append(line.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static String stripLineComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '/' && line.charAt(i + 1) == '/') {
                    if (i > 0 && line.charAt(i - 1) == ':') continue;
                    return line.substring(0, i);
                }
            }
        }
        return line;
    }

    private static void flushLine(StringBuilder result, StringBuilder currentLine, String nextLine) {
        if (currentLine.isEmpty()) return;

        String line = currentLine.toString();
        int dashIndex = line.indexOf("- ");

        if (dashIndex != -1 && line.substring(0, dashIndex).trim().isEmpty()) {
            String spaces = line.substring(0, dashIndex + 2);
            String content = line.substring(dashIndex + 2);
            String trimmedContent = content.trim();

            if (trimmedContent.endsWith(":")) {
                boolean hasOffset = false;

                if (nextLine != null) {
                    int currentIndent = getIndent(line);
                    int nextIndent = getIndent(nextLine);
                    if (nextIndent > currentIndent) {
                        hasOffset = true;
                    }
                }

                if (hasOffset) {
                    result.append(line).append("\n");
                } else {
                    result.append(spaces).append("'").append(content.replace("'", "''")).append("'\n");
                }
            } else if ((trimmedContent.startsWith("\"") && trimmedContent.endsWith("\"")) ||
                    (trimmedContent.startsWith("'") && trimmedContent.endsWith("'"))) {
                result.append(line).append("\n");
            } else {
                result.append(spaces).append("'").append(content.replace("'", "''")).append("'\n");
            }
        } else {
            result.append(line).append("\n");
        }

        currentLine.setLength(0);
    }

    private static int getIndent(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t') count++;
            else break;
        }
        return count;
    }
}
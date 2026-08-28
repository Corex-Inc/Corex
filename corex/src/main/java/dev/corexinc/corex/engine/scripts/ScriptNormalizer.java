package dev.corexinc.corex.engine.scripts;

import dev.corexinc.corex.api.scripts.ScriptComment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Turns a {@code .cx} file into valid YAML.
 *
 * <p>Comments come out, folded continuation lines are joined onto the line they belong to, and
 * command lines are quoted so that a colon inside one does not read as a YAML mapping. This is
 * Corex's own normalization step, not the addon hook: that one is
 * {@link dev.corexinc.corex.api.scripts.AbstractPreprocessor}, and it runs around this.</p>
 */
public class ScriptNormalizer {

    /**
     * Stands in for {@code #} between normalization and parsing, so a hash inside a script is not
     * read as the start of a YAML comment.
     */
    public static final char HASH_PLACEHOLDER = '\uE000';

    /**
     * Replaces every {@code #} with {@link #HASH_PLACEHOLDER}.
     *
     * @param text the text to escape.
     * @return the escaped text.
     */
    public static String escapeHashes(String text) {
        return text.indexOf('#') == -1 ? text : text.replace('#', HASH_PLACEHOLDER);
    }

    /**
     * Normalizes a script into YAML, throwing its comments away.
     *
     * @param rawLines the file contents.
     * @return the YAML text.
     */
    public static String preprocess(List<String> rawLines) {
        return preprocess(rawLines, null);
    }

    /**
     * Normalizes a script into YAML, optionally keeping the comments it strips.
     *
     * @param rawLines the file contents.
     * @param comments a list to collect comments into, or {@code null} to discard them. Corex
     *                 passes one only when an addon asked for
     *                 {@link dev.corexinc.corex.api.scripts.PreprocessStage#COMMENTS}, so the
     *                 collection costs nothing when nobody wants it.
     * @return the YAML text.
     */
    public static String preprocess(List<String> rawLines, @Nullable List<ScriptComment> comments) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        boolean inBlockComment = false;
        int lineNumber = 0;

        for (String line : rawLines) {
            lineNumber++;

            line = line.replace("\t", "    ");
            line = escapeHashes(line);

            if (inBlockComment) {
                int endIdx = line.indexOf("*/");
                if (endIdx == -1) {
                    collect(comments, lineNumber, line, true);
                    continue;
                }
                collect(comments, lineNumber, line.substring(0, endIdx), true);
                inBlockComment = false;
                line = line.substring(endIdx + 2);
            }

            line = stripInlineBlockComments(line, comments, lineNumber);

            int blockStart = line.indexOf("/*");
            if (blockStart != -1) {
                inBlockComment = true;
                collect(comments, lineNumber, line.substring(blockStart + 2), true);
                line = line.substring(0, blockStart);
            }

            String withoutLineComment = stripLineComment(line);
            if (comments != null && withoutLineComment.length() != line.length()) {
                collect(comments, lineNumber, line.substring(withoutLineComment.length() + 2), false);
            }
            line = withoutLineComment;

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

    private static void collect(@Nullable List<ScriptComment> comments, int line, String text, boolean block) {
        if (comments == null) return;

        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        comments.add(new ScriptComment(line, restoreHashes(trimmed), block));
    }

    private static String restoreHashes(String text) {
        return text.indexOf(HASH_PLACEHOLDER) == -1 ? text : text.replace(HASH_PLACEHOLDER, '#');
    }

    private static String stripInlineBlockComments(String line, @Nullable List<ScriptComment> comments, int lineNumber) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '*') {
                int end = line.indexOf("*/", i + 2);
                if (end != -1) {
                    collect(comments, lineNumber, line.substring(i + 2, end), true);
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
                if (inSingleQuote || i == 0 || !Character.isLetterOrDigit(line.charAt(i - 1))) {
                    inSingleQuote = !inSingleQuote;
                }
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
                    String key = trimmedContent.substring(0, trimmedContent.length() - 1);
                    boolean alreadyQuoted = (key.startsWith("\"") && key.endsWith("\""))
                            || (key.startsWith("'") && key.endsWith("'"));
                    if (alreadyQuoted) {
                        result.append(line).append("\n");
                    } else {
                        result.append(spaces).append("'").append(key.replace("'", "''")).append("':\n");
                    }
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
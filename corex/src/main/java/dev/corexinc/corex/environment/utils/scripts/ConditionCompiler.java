package dev.corexinc.corex.environment.utils.scripts;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.util.ArrayList;
import java.util.List;

public class ConditionCompiler {

    public interface Condition {
        boolean evaluate(ScriptQueue queue);
    }

    public static Condition compile(CompiledArgument[] args) {
        if (args == null || args.length == 0) return q -> false;

        try {
            StringBuilder sb = new StringBuilder();
            for (CompiledArgument arg : args) {
                sb.append(arg.getRaw()).append(" ");
            }
            String line = sb.toString().trim();

            List<String> tokens = tokenizeCondition(line);

            Parser parser = new Parser(tokens);
            Condition condition = parser.parseExpression();

            if (parser.pos < tokens.size()) {
                CorexLogger.error("IF COMPILER ERROR: Unexpected tokens at the end of condition: '" + tokens.get(parser.pos) + "'");
            }

            return condition;
        } catch (Exception e) {
            CorexLogger.error("IF COMPILER ERROR: " + e.getMessage());
            return q -> false;
        }
    }

    private static boolean isTagStart(String str, int index) {
        if (str.charAt(index) != '<') return false;
        if (index + 1 >= str.length()) return false;
        char next = str.charAt(index + 1);
        return Character.isLetter(next) || next == '_' || next == '[' || next == '#' || next == '&';
    }

    private static List<String> tokenizeCondition(String line) {
        List<String> tokens = new ArrayList<>();
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ') continue;

            if (c == '(' || c == ')') {
                tokens.add(String.valueOf(c));
                continue;
            }

            if (c == '=' && i + 1 < len && line.charAt(i + 1) == '=') {
                if (i + 2 < len && line.charAt(i + 2) == '=') {
                    tokens.add("==="); i += 2;
                } else {
                    tokens.add("=="); i++;
                }
                continue;
            }
            if (c == '!' && i + 1 < len && line.charAt(i + 1) == '=') {
                tokens.add("!="); i++; continue;
            }
            if (c == '>' && i + 1 < len && line.charAt(i + 1) == '=') {
                tokens.add(">="); i++; continue;
            }
            if (c == '<' && i + 1 < len && line.charAt(i + 1) == '=') {
                tokens.add("<="); i++; continue;
            }
            if (c == '&' && i + 1 < len && line.charAt(i + 1) == '&') {
                tokens.add("&&"); i++; continue;
            }
            if (c == '|' && i + 1 < len && line.charAt(i + 1) == '|') {
                tokens.add("||"); i++; continue;
            }

            if (c == '>' || c == '!') {
                tokens.add(String.valueOf(c));
                continue;
            }

            if (c == '<') {
                if (isTagStart(line, i)) {
                    int start = i;
                    int depth = 0;
                    while (i < len) {
                        char tc = line.charAt(i);
                        if (tc == '<') depth++;
                        else if (tc == '>') {
                            depth--;
                            if (depth == 0) break;
                        }
                        i++;
                    }
                    tokens.add(line.substring(start, Math.min(i + 1, len)));
                    continue;
                } else {
                    tokens.add("<");
                    continue;
                }
            }

            int start = i;
            while (i < len) {
                char ch = line.charAt(i);
                if (ch == ' ' || ch == '(' || ch == ')' || ch == '=' || ch == '!' || ch == '>' || ch == '<' || ch == '&' || ch == '|') {
                    break;
                }
                i++;
            }
            tokens.add(line.substring(start, i));
            i--;
        }
        return tokens;
    }

    private static class Parser {
        int pos = 0;
        List<String> tokens;

        Parser(List<String> tokens) {
            this.tokens = tokens;
        }

        String peek() {
            if (pos >= tokens.size()) return null;
            return tokens.get(pos);
        }

        boolean match(String... expected) {
            String current = peek();
            if (current == null) return false;
            for (String t : expected) {
                if (current.equalsIgnoreCase(t)) {
                    pos++;
                    return true;
                }
            }
            return false;
        }

        Condition parseExpression() {
            Condition left = parseAnd();
            while (match("||", "or")) {
                Condition right = parseAnd();
                Condition finalLeft = left;
                left = q -> finalLeft.evaluate(q) || right.evaluate(q);
            }
            return left;
        }

        Condition parseAnd() {
            Condition left = parseNot();
            while (match("&&", "and")) {
                Condition right = parseNot();
                Condition finalLeft = left;
                left = q -> finalLeft.evaluate(q) && right.evaluate(q);
            }
            return left;
        }

        Condition parseNot() {
            if (match("!", "not")) {
                Condition inner = parseNot();
                return q -> !inner.evaluate(q);
            }
            return parseFactor();
        }

        boolean isOperator(String s) {
            if (s == null) return false;
            s = s.toLowerCase();
            return s.equals("===") || s.equals("==") || s.equals("=") || s.equals("!=") ||
                    s.equals(">") || s.equals("<") || s.equals(">=") ||
                    s.equals("<=") || s.equals("contains");
        }

        Condition parseFactor() {
            if (match("(")) {
                Condition inner = parseExpression();
                if (!match(")")) throw new RuntimeException("Missing closing parenthesis ')'!");
                return inner;
            }

            if (pos >= tokens.size()) throw new RuntimeException("Unexpected end of condition!");

            String leftRaw = tokens.get(pos++);
            CompiledArgument leftArg = ScriptCompiler.parseArg(leftRaw);

            if (pos < tokens.size() && isOperator(peek())) {
                String op = peek();
                pos++;

                if (pos >= tokens.size()) throw new RuntimeException("Missing right side for operator '" + op + "'");

                String rightRaw = tokens.get(pos++);
                CompiledArgument rightArg = ScriptCompiler.parseArg(rightRaw);

                return q -> compare(leftArg.evaluate(q).identify(), op, rightArg.evaluate(q).identify());
            }

            return q -> {
                String val = leftArg.evaluate(q).identify().trim();
                boolean inverted = false;

                if (val.startsWith("!")) {
                    inverted = true;
                    val = val.substring(1);
                }

                val = val.toLowerCase();
                boolean result = val.equals("true") || (!val.equals("false") && !val.equals("null") && !val.isEmpty());

                return inverted != result;
            };
        }
    }

    private static boolean compare(String val1, String op, String val2) {
        if (op.equals("===")) return val1.equals(val2);

        Double d1 = tryParse(val1);
        Double d2 = tryParse(val2);
        boolean isNumeric = (d1 != null && d2 != null);

        return switch (op) {
            case "=", "==" -> isNumeric ? d1.equals(d2) : val1.equalsIgnoreCase(val2);
            case "!=" -> isNumeric ? !d1.equals(d2) : !val1.equalsIgnoreCase(val2);
            case ">"  -> isNumeric ? d1 > d2  : val1.compareToIgnoreCase(val2) > 0;
            case "<"  -> isNumeric ? d1 < d2  : val1.compareToIgnoreCase(val2) < 0;
            case ">=" -> isNumeric ? d1 >= d2 : val1.compareToIgnoreCase(val2) >= 0;
            case "<=" -> isNumeric ? d1 <= d2 : val1.compareToIgnoreCase(val2) <= 0;
            case "contains" -> val1.toLowerCase().contains(val2.toLowerCase());
            default -> false;
        };
    }

    private static Double tryParse(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }
}
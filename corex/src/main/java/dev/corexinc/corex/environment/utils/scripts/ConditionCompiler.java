package dev.corexinc.corex.environment.utils.scripts;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.queue.ScriptQueue;

public class ConditionCompiler {

    public interface Condition {
        boolean evaluate(ScriptQueue queue);
    }

    public static Condition compile(CompiledArgument[] args) {
        if (args.length == 0) return q -> false;

        return queue -> {
            boolean finalResult = true;
            boolean nextIsOr = false;

            for (int i = 0; i < args.length; i++) {
                String token = args[i].evaluate(queue).identify();
                String lower = token.toLowerCase();

                if (lower.equals("&&") || lower.equals("and")) {
                    if (!finalResult) return false;
                    nextIsOr = false;
                    continue;
                }
                if (lower.equals("||") || lower.equals("or")) {
                    if (finalResult) return true;
                    nextIsOr = true;
                    continue;
                }

                boolean inverted = false;
                if (token.startsWith("!")) {
                    inverted = true;
                    token = token.substring(1);
                }

                boolean checkResult;
                if (i + 2 < args.length && isOperator(args[i + 1].evaluate(queue).identify())) {
                    checkResult = compare(token, args[i + 1].evaluate(queue).identify(), args[i + 2].evaluate(queue).identify());
                    i += 2;
                } else {
                    checkResult = token.equalsIgnoreCase("true") ||
                            (!token.equalsIgnoreCase("null") && !token.equalsIgnoreCase("false") && !token.isEmpty());
                }

                if (inverted) checkResult = !checkResult;
                finalResult = nextIsOr ? finalResult || checkResult : finalResult && checkResult;
            }
            return finalResult;
        };
    }

    private static boolean isOperator(String op) {
        String o = op.toLowerCase();
        return o.equals("==") || o.equals("=") || o.equals("!=") || o.equals(">") || o.equals("<")
                || o.equals(">=") || o.equals("<=") || o.equals("contains");
    }

    private static boolean compare(String val1, String op, String val2) {
        Double d1 = tryParse(val1);
        Double d2 = tryParse(val2);
        boolean isNumeric = (d1 != null && d2 != null);

        return switch (op.toLowerCase()) {
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
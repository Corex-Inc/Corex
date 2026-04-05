package dev.corexinc.corex.environment.utils;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ElementTag;

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
                if (token.isEmpty()) continue;

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
                if (lower.startsWith("!")) {
                    inverted = true;
                    token = token.substring(1);
                }

                boolean checkResult = false;

                if (i + 2 < args.length && isOperator(args[i + 1].evaluate(queue).identify())) {
                    String op = args[i + 1].evaluate(queue).identify();
                    String val2 = args[i + 2].evaluate(queue).identify();
                    checkResult = compare(token, op, val2);
                    i += 2;
                } else {
                    checkResult = new ElementTag(token).asBoolean() ||
                            (!token.equalsIgnoreCase("null") && !token.equals("false") && !token.isEmpty());
                }

                if (inverted) checkResult = !checkResult;

                if (nextIsOr) finalResult = finalResult || checkResult;
                else finalResult = finalResult && checkResult;
            }

            return finalResult;
        };
    }

    private static boolean isOperator(String op) {
        return op.equals("==") || op.equals("!=") || op.equals(">") || op.equals("<")
                || op.equals(">=") || op.equals("<=") || op.equalsIgnoreCase("contains");
    }

    private static boolean compare(String val1, String op, String val2) {
        ElementTag el1 = new ElementTag(val1);
        ElementTag el2 = new ElementTag(val2);

        return switch (op.toLowerCase()) {
            case "==" -> val1.equalsIgnoreCase(val2);
            case "!=" -> !val1.equalsIgnoreCase(val2);
            case ">" -> el1.isDouble() && el2.isDouble() && el1.asDouble() > el2.asDouble();
            case "<" -> el1.isDouble() && el2.isDouble() && el1.asDouble() < el2.asDouble();
            case ">=" -> el1.isDouble() && el2.isDouble() && el1.asDouble() >= el2.asDouble();
            case "<=" -> el1.isDouble() && el2.isDouble() && el1.asDouble() <= el2.asDouble();
            case "contains" -> val1.toLowerCase().contains(val2.toLowerCase());
            default -> false;
        };
    }
}
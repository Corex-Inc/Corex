package dev.corexmc.corex.environment.utils;

import dev.corexmc.corex.engine.compiler.CompiledArgument;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.core.ElementTag;

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
                String token = args[i].evaluate(queue);
                if (token == null || token.isEmpty()) continue;

                String lower = token.toLowerCase();

                // Логические операторы
                if (lower.equals("&&") || lower.equals("and")) {
                    if (!finalResult) return false; // Оптимизация: дальше нет смысла проверять
                    nextIsOr = false;
                    continue;
                }
                if (lower.equals("||") || lower.equals("or")) {
                    if (finalResult) return true; // Оптимизация
                    nextIsOr = true;
                    continue;
                }

                // Инверсия
                boolean inverted = false;
                if (lower.startsWith("!")) {
                    inverted = true;
                    token = token.substring(1);
                }

                boolean checkResult = false;

                // Если есть оператор сравнения
                if (i + 2 < args.length && isOperator(args[i + 1].evaluate(queue))) {
                    String op = args[i + 1].evaluate(queue);
                    String val2 = args[i + 2].evaluate(queue);
                    checkResult = compare(token, op, val2);
                    i += 2; // Пропускаем оператор и второе значение
                } else {
                    // Проверка на правдивость (truthiness)
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
package dev.corexinc.corex.engine.compiler.math;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;

public class MathCompiler {

    public static MathNode compile(String expression) {
        if (expression.startsWith("(") && expression.endsWith(")")) {
            expression = expression.substring(1, expression.length() - 1);
        }
        return new Parser(expression).parse();
    }

    private static class Parser {
        private int pos = -1, ch;
        private final String str;

        public Parser(String str) { this.str = str; }

        private void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        public MathNode parse() {
            nextChar();
            MathNode x = parseExpression();
            if (pos < str.length()) {
                throw new RuntimeException("Unexpected symbol: '" + (char)ch + "' in position " + pos);
            }
            return x;
        }

        private MathNode parseExpression() {
            MathNode x = parseTerm();
            for (;;) {
                if      (eat('+')) { MathNode a = x, b = parseTerm(); x = q -> a.eval(q) + b.eval(q); }
                else if (eat('-')) { MathNode a = x, b = parseTerm(); x = q -> a.eval(q) - b.eval(q); }
                else return x;
            }
        }

        private MathNode parseTerm() {
            MathNode x = parseFactor();
            for (;;) {
                if      (eat('*')) { MathNode a = x, b = parseFactor(); x = q -> a.eval(q) * b.eval(q); }
                else if (eat('/')) { MathNode a = x, b = parseFactor(); x = q -> a.eval(q) / b.eval(q); }
                else if (eat('%')) { MathNode a = x, b = parseFactor(); x = q -> a.eval(q) % b.eval(q); }
                else return x;
            }
        }

        private MathNode parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) { MathNode a = parseFactor(); return q -> -a.eval(q); }

            MathNode x;
            int startPos = this.pos;

            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing closing bracket ')'!");
            }
            else if (ch == '<') {
                int depth = 0;
                while (ch != -1) {
                    if (ch == '<') depth++;
                    else if (ch == '>') {
                        depth--;
                        if (depth == 0) {
                            nextChar();
                            break;
                        }
                    }
                    nextChar();
                }

                String tagStr = str.substring(startPos, pos);
                CompiledArgument arg = ScriptCompiler.parseArg(tagStr);

                x = q -> {
                    try {
                        assert arg != null;
                        return Double.parseDouble(arg.evaluate(q)); }
                    catch(Exception e) { return 0.0; }
                };
            }
            else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                double val = Double.parseDouble(str.substring(startPos, pos));
                x = q -> val;
            } else {
                throw new RuntimeException("Expected a number, tag, or bracket, but found '" + (char)ch + "'");
            }

            if (eat('^')) { MathNode a = x, b = parseFactor(); x = q -> Math.pow(a.eval(q), b.eval(q)); }
            return x;
        }
    }
}
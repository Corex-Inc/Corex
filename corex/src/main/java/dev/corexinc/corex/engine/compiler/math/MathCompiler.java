package dev.corexinc.corex.engine.compiler.math;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;

public class MathCompiler {

    public static MathNode compile(String expression) {
        String workStr = expression.trim();
        if (workStr.startsWith("(") && workStr.endsWith(")")) {
            workStr = workStr.substring(1, workStr.length() - 1);
        }
        return new Parser(workStr).parse();
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

        private int peek() {
            int p = pos + 1;
            while (p < str.length() && str.charAt(p) == ' ') p++;
            return (p < str.length()) ? str.charAt(p) : -1;
        }

        public MathNode parse() {
            nextChar();
            MathNode x = parseShift();
            if (pos < str.length()) {
                throw new RuntimeException("Unexpected symbol: '" + (char)ch + "'");
            }
            return x;
        }

        private MathNode parseShift() {
            MathNode x = parseExpression();
            for (;;) {
                while (ch == ' ') nextChar();
                if (ch == '<') {
                    if (peek() == '<') {
                        nextChar(); nextChar();
                        MathNode a = x, b = parseExpression();
                        x = q -> (double) ((long) a.eval(q) << (long) b.eval(q));
                    } else break;
                } else if (ch == '>') {
                    int p1 = peek();
                    if (p1 == '>') {
                        int savedPos = pos;
                        nextChar(); nextChar();
                        while (ch == ' ') nextChar();
                        if (ch == '>') {
                            nextChar();
                            MathNode a = x, b = parseExpression();
                            x = q -> (double) ((long) a.eval(q) >>> (long) b.eval(q));
                        } else {
                            MathNode a = x, b = parseExpression();
                            x = q -> (double) ((long) a.eval(q) >> (long) b.eval(q));
                        }
                    } else break;
                } else break;
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
                x = parseShift();
                if (!eat(')')) throw new RuntimeException("Missing closing bracket ')'");
            } else if (eat('|')) {
                x = parseShift();
                if (!eat('|')) throw new RuntimeException("Missing closing pipe '|'");
                MathNode inner = x;
                x = q -> Math.abs(inner.eval(q));
            } else if (ch == '<') {
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
                    try { return Double.parseDouble(arg.evaluate(q).identify()); }
                    catch(Exception e) { return 0.0; }
                };
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                double val = Double.parseDouble(str.substring(startPos, pos));
                x = q -> val;
            } else {
                throw new RuntimeException("Expected number/tag/bracket, found '" + (char)ch + "'");
            }

            if (eat('^')) { MathNode a = x, b = parseFactor(); x = q -> Math.pow(a.eval(q), b.eval(q)); }
            return x;
        }
    }
}
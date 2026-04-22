package dev.corexinc.corex.environment.utils.commands;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;

import java.util.ArrayList;
import java.util.List;

public class CommandParser {

    public static class ParsedCommand {
        public final String command;
        public final String innerBlock;

        public ParsedCommand(String command, String innerBlock) {
            this.command = command;
            this.innerBlock = innerBlock;
        }
    }

    public static Instruction[] compileScript(String script) {
        List<ParsedCommand> parsed = split(script);
        List<Instruction> instructions = new ArrayList<>();

        for (ParsedCommand cmd : parsed) {
            Instruction[] inner = null;
            if (cmd.innerBlock != null) {
                inner = compileScript(cmd.innerBlock);
            }
            Instruction inst = ScriptCompiler.compile(cmd.command, inner);
            if (inst != null) {
                instructions.add(inst);
            }
        }
        return instructions.toArray(new Instruction[0]);
    }

    public static List<ParsedCommand> split(String script) {
        List<ParsedCommand> result = new ArrayList<>();
        StringBuilder currentCmd = new StringBuilder();
        StringBuilder currentBlock = null;

        boolean inQuotes = false;
        int tagDepth = 0;
        int mathDepth = 0;
        int braceDepth = 0;
        boolean escaped = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            if (escaped) {
                if (braceDepth > 0) currentBlock.append(c);
                else currentCmd.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                if (braceDepth > 0) currentBlock.append(c);
                else currentCmd.append(c);
                continue;
            }

            if (c == '<') tagDepth++;
            if (c == '>') tagDepth--;
            if (c == '(' && tagDepth == 0) mathDepth++;
            if (c == ')' && tagDepth == 0) mathDepth--;

            if (c == '"') {
                if (tagDepth == 0 && mathDepth == 0) {
                    inQuotes = !inQuotes;
                }
            }

            if (c == '{' && !inQuotes && tagDepth == 0 && mathDepth == 0) {
                if (braceDepth == 0) {
                    currentBlock = new StringBuilder();
                    braceDepth++;
                    continue;
                }
                braceDepth++;
            } else if (c == '}' && !inQuotes && tagDepth == 0 && mathDepth == 0) {
                braceDepth--;
                if (braceDepth == 0) {
                    continue;
                }
            } else if (c == '-' && braceDepth == 0 && !inQuotes && tagDepth == 0 && mathDepth == 0) {
                boolean spaceBefore = (i == 0 || Character.isWhitespace(script.charAt(i - 1)));
                boolean spaceAfter = (i + 1 == script.length() || Character.isWhitespace(script.charAt(i + 1)));

                if (spaceBefore && spaceAfter) {
                    finalizeCommand(result, currentCmd, currentBlock);
                    currentCmd.setLength(0);
                    currentBlock = null;
                    continue;
                }
            }

            if (braceDepth > 0) {
                currentBlock.append(c);
            } else {
                currentCmd.append(c);
            }
        }

        finalizeCommand(result, currentCmd, currentBlock);
        return result;
    }

    private static void finalizeCommand(List<ParsedCommand> list, StringBuilder cmdBuilder, StringBuilder blockBuilder) {
        String cmdStr = cmdBuilder.toString().trim();
        if (cmdStr.isEmpty()) return;

        String blockStr = blockBuilder != null ? blockBuilder.toString() : null;
        list.add(new ParsedCommand(cmdStr, blockStr));
    }
}
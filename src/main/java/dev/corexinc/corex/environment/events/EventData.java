package dev.corexinc.corex.environment.events;

import dev.corexinc.corex.engine.compiler.Instruction;
import java.util.List;
import java.util.Map;

public class EventData {
    public final String rawLine;
    public final boolean isAfter;
    public final Instruction[] bytecode;
    public final Map<String, String> switches;
    public final Map<String, List<String>> arguments;

    public EventData(String rawLine, boolean isAfter, Instruction[] bytecode, Map<String, String> switches, Map<String, List<String>> arguments) {
        this.rawLine = rawLine;
        this.isAfter = isAfter;
        this.bytecode = bytecode;
        this.switches = switches;
        this.arguments = arguments;
    }

    public String getSwitch(String key) {
        return switches.get(key.toLowerCase());
    }

    public String getArgument(String key, int index) {
        List<String> args = arguments.get(key.toLowerCase());
        if (args != null && args.size() > index) {
            return args.get(index);
        }
        return null;
    }

    public boolean isGenericMatch(String argName, int index, String actualValue) {
        String requested = getArgument(argName, index);
        if (requested == null) return true;

        if (requested.equalsIgnoreCase(argName) || requested.equals("*") || requested.equalsIgnoreCase("any")) {
            return true;
        }

        return requested.equalsIgnoreCase(actualValue) ||
                requested.equalsIgnoreCase("minecraft:" + actualValue) ||
                actualValue.equalsIgnoreCase("minecraft:" + requested);
    }
}
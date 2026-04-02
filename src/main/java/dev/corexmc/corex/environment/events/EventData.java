package dev.corexmc.corex.environment.events;

import dev.corexmc.corex.engine.compiler.Instruction;
import java.util.Map;

public class EventData {
    public final String rawLine;
    public final boolean isAfter;
    public final Instruction[] bytecode;
    public final Map<String, String> switches;

    public EventData(String rawLine, boolean isAfter, Instruction[] bytecode, Map<String, String> switches) {
        this.rawLine = rawLine;
        this.isAfter = isAfter;
        this.bytecode = bytecode;
        this.switches = switches;
    }

    public String getSwitch(String key) {
        return switches.get(key.toLowerCase());
    }
}
package dev.corexmc.corex.api.tags;

import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.tags.TagParser;

import java.util.ArrayList;
import java.util.List;

public class Attribute {

    private final List<TagComponent> components = new ArrayList<>();
    private int currentIndex = 0;
    private final ScriptQueue queue;

    public Attribute(String rawTag, ScriptQueue queue) {
        this.queue = queue;
        parseComponents(rawTag);
    }

    private void parseComponents(String raw) {
        StringBuilder name = new StringBuilder();
        StringBuilder param = new StringBuilder();
        int bracketDepth = 0;

        for (char c : raw.toCharArray()) {
            if (c == '[') {
                bracketDepth++;
                if (bracketDepth == 1) continue;
            } else if (c == ']') {
                bracketDepth--;
                if (bracketDepth == 0) continue;
            } else if (c == '.' && bracketDepth == 0) {
                components.add(new TagComponent(name.toString(), !param.isEmpty() ? param.toString() : null));
                name.setLength(0);
                param.setLength(0);
                continue;
            }

            if (bracketDepth > 0) {
                param.append(c);
            } else {
                name.append(c);
            }
        }
        components.add(new TagComponent(name.toString(), !param.isEmpty() ? param.toString() : null));
    }

    public boolean hasNext() {
        return currentIndex < components.size();
    }

    public String getName() {
        return components.get(currentIndex).name;
    }

    public boolean hasParam() {
        return components.get(currentIndex).param != null;
    }

    public String getParam() {
        String rawParam = components.get(currentIndex).param;
        if (rawParam == null) return null;
        return TagParser.parse(rawParam).evaluate(queue);
    }

    public boolean matchesNext(String expectedName) {
        if (currentIndex + 1 >= components.size()) return false;
        return components.get(currentIndex + 1).name.equalsIgnoreCase(expectedName);
    }

    public boolean hasNextParam() {
        if (currentIndex + 1 >= components.size()) return false;
        return components.get(currentIndex + 1).param != null;
    }

    public String getNextParam() {
        String rawParam = components.get(currentIndex + 1).param;
        return TagParser.parse(rawParam).evaluate(queue);
    }

    public void fulfill(int steps) {
        this.currentIndex += steps;
    }

    private static class TagComponent {
        final String name;
        final String param;
        TagComponent(String name, String param) {
            this.name = name;
            this.param = param;
        }
    }

    public ScriptQueue getQueue() {
        return queue;
    }
}
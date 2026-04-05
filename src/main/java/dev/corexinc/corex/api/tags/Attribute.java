package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.engine.compiler.TagNode;
import dev.corexinc.corex.engine.queue.ScriptQueue;

public class Attribute {

    private final TagNode[] components;
    private int currentIndex = 0;
    private final ScriptQueue queue;

    public Attribute(TagNode[] components, ScriptQueue queue) {
        this.components = components;
        this.queue = queue;
    }

    public boolean hasNext() {
        return currentIndex < components.length;
    }

    public String getName() {
        return components[currentIndex].name;
    }

    public boolean hasParam() {
        return components[currentIndex].param != null;
    }

    public String getParam() {
        AbstractTag tag = getParamObject();
        return tag != null ? tag.identify() : null;
    }

    public AbstractTag getParamObject() {
        if (components[currentIndex].param == null) return null;
        return components[currentIndex].param.evaluate(queue);
    }

    public boolean matchesNext(String expectedName) {
        if (currentIndex + 1 >= components.length) return false;
        return components[currentIndex + 1].name.equalsIgnoreCase(expectedName);
    }

    public boolean hasNextParam() {
        if (currentIndex + 1 >= components.length) return false;
        return components[currentIndex + 1].param != null;
    }

    public String getNextParam() {
        AbstractTag tag = getNextParamObject();
        return tag != null ? tag.identify() : null;
    }

    public AbstractTag getNextParamObject() {
        if (currentIndex + 1 >= components.length || components[currentIndex + 1].param == null) return null;
        return components[currentIndex + 1].param.evaluate(queue);
    }

    public void fulfill(int steps) {
        this.currentIndex += steps;
    }

    public ScriptQueue getQueue() {
        return queue;
    }
}
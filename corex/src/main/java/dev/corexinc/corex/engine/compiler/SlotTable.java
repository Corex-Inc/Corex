package dev.corexinc.corex.engine.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SlotTable {

    public static final int NO_SLOT = -1;

    private final Map<String, Integer> indexByName = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    int intern(String name) {
        Integer existing = indexByName.get(name);
        if (existing != null) return existing;
        int index = names.size();
        names.add(name);
        indexByName.put(name, index);
        return index;
    }

    public int indexOf(String name) {
        Integer index = indexByName.get(name);
        return index != null ? index : NO_SLOT;
    }

    public String nameAt(int index) {
        return names.get(index);
    }

    public int size() {
        return names.size();
    }
}

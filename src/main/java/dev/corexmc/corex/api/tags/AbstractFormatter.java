package dev.corexmc.corex.api.tags;

import java.util.List;

public interface AbstractFormatter {

    String getName();

    default List<String> getAlias() {
        return List.of(getName());
    }

    AbstractTag parse(Attribute attribute);

    default String getTestParam() {
        return null;
    }
}
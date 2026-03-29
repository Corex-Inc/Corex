package dev.corexmc.corex.api.tags;

public interface AbstractTag {
    String identify();
    String getPrefix();

    AbstractTag setPrefix(String prefix);

    AbstractTag getAttribute(Attribute attribute);

}
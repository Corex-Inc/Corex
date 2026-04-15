package dev.corexinc.corex.engine.flags;

import dev.corexinc.corex.api.tags.AbstractTag;

public interface FlagExpirationHandler {
    AbstractTag onExpired(String trackerId, String path, AbstractTag value);
}
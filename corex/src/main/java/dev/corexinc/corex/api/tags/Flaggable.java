package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;

public interface Flaggable {
    AbstractFlagTracker getFlagTracker();
}
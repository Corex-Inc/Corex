package dev.corexinc.corex.engine.utils.exceptions;

import org.bukkit.Location;

public class RegionRelocateException extends RuntimeException {

    private final Location location;

    public RegionRelocateException(Location location) {
        super(null, null, true, false);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
package dev.corexinc.corex.environment.tags.world.area;

import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Location;

public interface AbstractAreaObject {

    boolean contains(Location location);

    LocationTag getCenter();
}
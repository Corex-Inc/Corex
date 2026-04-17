package dev.corexinc.corex.environment.tags.world.area;

import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Location;

import java.util.List;

public interface AbstractAreaObject {

    boolean contains(Location location);

    LocationTag getCenter();

    List<LocationTag> getBlocks();
}
package dev.corexinc.corex.engine.utils.exceptions;

import dev.corexinc.corex.engine.utils.Position;

public class RegionRelocateException extends RuntimeException {

    private final Position position;

    public RegionRelocateException(Position position) {
        super(null, null, true, false);
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }
}
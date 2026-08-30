package dev.corexinc.corex.engine.network;

/**
 * Thrown when a byte array does not describe a well formed Corex packet.
 *
 * <p>Every decode path funnels malformed input, oversized lengths and failed signature checks
 * into this one exception so a listener can drop the frame with a single catch instead of
 * guarding against {@link IndexOutOfBoundsException}, {@link NegativeArraySizeException} and
 * {@link OutOfMemoryError} separately.</p>
 *
 * @since 1.0.0
 */
public class PacketFormatException extends RuntimeException {

    public PacketFormatException(String message) {
        super(message);
    }

    public PacketFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

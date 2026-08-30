package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;

/**
 * A resolved shared secret and where it was found.
 *
 * <p>The source is worth carrying around because the single most likely failure on a live network
 * is the proxy and a backend ending up on different secrets. A startup line naming where each side
 * got its key turns that from a silent "nothing arrives" into something an admin can read.</p>
 *
 * @param value  the raw secret, before {@link PacketCodec#toKey} derives a key from it.
 * @param source a short human readable description of where it came from.
 * @since 1.0.0
 */
public record NetworkSecret(@NotNull String value, @NotNull String source) {}

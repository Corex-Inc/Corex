package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.api.processors.MechanismProcessor;
import org.jetbrains.annotations.NotNull;

public interface Adjustable extends AbstractTag {

    Adjustable duplicate();

    @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value);

    MechanismProcessor<? extends AbstractTag> getMechanismProcessor();
}
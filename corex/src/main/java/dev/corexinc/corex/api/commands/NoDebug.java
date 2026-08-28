package dev.corexinc.corex.api.commands;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Keeps an argument out of a command's debug report.
 *
 * <p>Bound commands report every argument automatically. Mark a parameter with this when
 * printing it would be noise or a leak - a whole inventory, a token, a multi-kilobyte
 * body of text:</p>
 *
 * <pre>{@code
 * public void run(ScriptQueue queue, PlayerTag target, @NoDebug ElementTag token) { ... }
 * }</pre>
 *
 * <p>The argument is still resolved and passed as normal; only the debug line skips it.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface NoDebug {
}

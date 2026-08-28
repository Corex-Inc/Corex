package dev.corexinc.corex.api.commands;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown by a command to abort with a script-level error.
 *
 * <p>The message is reported the same way {@code Debugger.echoError} reports one - it
 * appears under the command's error header, with no Java stack trace - so it reads as
 * a scripting mistake rather than an engine crash.</p>
 *
 * <p>Prefer this over echoing an error and returning when the failure is detected deep
 * inside helper methods, where an early return would have to be threaded back by hand.</p>
 *
 * <pre>{@code
 * public void run(ScriptQueue queue, EntityTag target, ElementTag amount) {
 *     if (!amount.isDouble()) {
 *         throw new CommandExecutionException("Amount must be a number, got '" + amount.identify() + "'");
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public class CommandExecutionException extends RuntimeException {

    public CommandExecutionException(@NotNull String message) {
        super(message, null, false, false);
    }

    public CommandExecutionException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause, false, false);
    }
}

package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce logging ...` command execution.
 */
public final class LoggingCommandHandler {
    private LoggingCommandHandler() {
    }

    /**
     * Updates local session logging.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether local session logging should be enabled
     * @return Brigadier command result code
     */
    public static int enabled(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSessionLoggingEnabled(enabled));
    }

    /**
     * Updates local session logging format.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param format local session log format id
     * @return Brigadier command result code
     */
    public static int format(ReinodoceCommandService service, CommandSourceStack source, String format) {
        return CommandFeedback.sendResult(source, service.setSessionLoggingFormat(format));
    }
}

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

    /**
     * Updates session log retention by age.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param days retention days, or zero to disable age cleanup
     * @return Brigadier command result code
     */
    public static int retentionDays(ReinodoceCommandService service, CommandSourceStack source, int days) {
        return CommandFeedback.sendResult(source, service.setSessionLoggingRetentionDays(days));
    }

    /**
     * Updates session log retention by file count.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param files retained file count, or zero to disable count cleanup
     * @return Brigadier command result code
     */
    public static int retentionFiles(ReinodoceCommandService service, CommandSourceStack source, int files) {
        return CommandFeedback.sendResult(source, service.setSessionLoggingRetentionFiles(files));
    }

    /**
     * Updates session log username masking.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether session log usernames should be masked
     * @return Brigadier command result code
     */
    public static int anonymized(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSessionLoggingAnonymized(enabled));
    }

    /**
     * Updates whether session logs omit message bodies.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether message bodies should be omitted
     * @return Brigadier command result code
     */
    public static int metadataOnly(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSessionLoggingMetadataOnly(enabled));
    }
}

package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce stats` command execution.
 */
public final class StatsCommandHandler {
    private StatsCommandHandler() {
    }

    /**
     * Prints current session stats.
     *
     * @param source command source to receive feedback
     * @param service command service boundary
     * @return Brigadier command result code
     */
    public static int execute(CommandSourceStack source, ReinodoceCommandService service) {
        for (String line : service.statsLines()) {
            CommandFeedback.sendResult(source, CommandResult.ok(line));
        }
        return 1;
    }

    /**
     * Resets current session stats.
     *
     * @param source command source to receive feedback
     * @param service command service boundary
     * @return Brigadier command result code
     */
    public static int reset(CommandSourceStack source, ReinodoceCommandService service) {
        return CommandFeedback.sendResult(source, service.resetStats());
    }
}

package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce status` execution.
 */
public final class StatusCommandHandler {
    private StatusCommandHandler() {
    }

    /**
     * Sends each status line from the command service.
     *
     * @param source command source to receive feedback
     * @param service command service boundary
     * @return Brigadier command result code
     */
    public static int execute(CommandSourceStack source, ReinodoceCommandService service) {
        for (String line : service.statusLines()) {
            CommandFeedback.sendLine(source, line);
        }
        return 1;
    }
}

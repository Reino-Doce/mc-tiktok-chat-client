package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce reload` execution.
 */
public final class ReloadCommandHandler {
    private ReloadCommandHandler() {
    }

    /**
     * Reloads persisted configuration through the command service.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int execute(ReinodoceCommandService service, CommandSourceStack source) {
        return CommandFeedback.sendResult(source, service.reload());
    }
}

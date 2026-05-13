package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce disconnect` execution.
 */
public final class DisconnectCommandHandler {
    private DisconnectCommandHandler() {
    }

    /**
     * Disconnects the configured service from the active TikTok LIVE session.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int execute(ReinodoceCommandService service, CommandSourceStack source) {
        return CommandFeedback.sendResult(source, service.disconnect());
    }
}

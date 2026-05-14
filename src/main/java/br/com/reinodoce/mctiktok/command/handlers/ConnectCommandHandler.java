package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles {@code /reinodoce connect} execution.
 */
public final class ConnectCommandHandler {
    private ConnectCommandHandler() {
    }

    /**
     * Connects the configured service to a TikTok LIVE username.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param username TikTok username argument
     * @return Brigadier command result code
     */
    public static int execute(ReinodoceCommandService service, CommandSourceStack source, String username) {
        return CommandFeedback.sendResult(source, service.connect(username));
    }

    /**
     * Connects the configured service to the saved last TikTok LIVE username.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int executeLast(ReinodoceCommandService service, CommandSourceStack source) {
        return CommandFeedback.sendResult(source, service.connectLast());
    }
}

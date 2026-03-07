package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import net.minecraft.commands.CommandSourceStack;

public final class DisconnectCommandHandler {
    private DisconnectCommandHandler() {
    }

    public static int execute(CommandSourceStack source) {
        CommandResult result = ReinodoceClientBootstrap.service().disconnect();
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }
}

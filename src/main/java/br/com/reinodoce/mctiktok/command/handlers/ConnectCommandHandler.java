package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import net.minecraft.commands.CommandSourceStack;

public final class ConnectCommandHandler {
    private ConnectCommandHandler() {
    }

    public static int execute(CommandSourceStack source, String username) {
        CommandResult result = ReinodoceClientBootstrap.service().connect(username);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }
}

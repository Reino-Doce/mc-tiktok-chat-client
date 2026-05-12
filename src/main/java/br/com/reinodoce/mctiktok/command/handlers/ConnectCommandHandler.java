package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

public final class ConnectCommandHandler {
    private ConnectCommandHandler() {
    }

    public static int execute(ReinodoceCommandService service, CommandSourceStack source, String username) {
        return CommandFeedback.sendResult(source, service.connect(username));
    }
}

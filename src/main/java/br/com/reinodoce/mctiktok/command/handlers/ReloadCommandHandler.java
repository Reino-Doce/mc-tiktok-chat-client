package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

public final class ReloadCommandHandler {
    private ReloadCommandHandler() {
    }

    public static int execute(ReinodoceCommandService service, CommandSourceStack source) {
        return CommandFeedback.sendResult(source, service.reload());
    }
}

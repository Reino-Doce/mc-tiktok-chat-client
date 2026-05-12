package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

public final class StatusCommandHandler {
    private StatusCommandHandler() {
    }

    public static int execute(CommandSourceStack source, ReinodoceCommandService service) {
        for (String line : service.statusLines()) {
            CommandFeedback.sendLine(source, line);
        }
        return 1;
    }
}

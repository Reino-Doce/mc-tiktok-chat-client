package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import net.minecraft.commands.CommandSourceStack;

public final class StatusCommandHandler {
    private StatusCommandHandler() {
    }

    public static int execute(CommandSourceStack source) {
        for (String line : ReinodoceClientBootstrap.service().statusLines()) {
            CommandFeedback.sendLine(source, line);
        }
        return 1;
    }
}

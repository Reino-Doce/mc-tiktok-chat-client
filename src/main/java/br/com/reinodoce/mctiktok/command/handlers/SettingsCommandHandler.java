package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import net.minecraft.commands.CommandSourceStack;

public final class SettingsCommandHandler {
    private SettingsCommandHandler() {
    }

    public static int reconnect(CommandSourceStack source, int seconds) {
        CommandResult result = ReinodoceClientBootstrap.service().setReconnectSeconds(seconds);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }

    public static int chatEmotes(CommandSourceStack source, boolean enabled) {
        CommandResult result = ReinodoceClientBootstrap.service().setChatEmotesEnabled(enabled);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }
}

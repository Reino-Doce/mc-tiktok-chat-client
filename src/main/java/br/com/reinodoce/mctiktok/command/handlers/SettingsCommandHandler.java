package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

public final class SettingsCommandHandler {
    private SettingsCommandHandler() {
    }

    public static int reconnect(ReinodoceCommandService service, CommandSourceStack source, int seconds) {
        return CommandFeedback.sendResult(source, service.setReconnectSeconds(seconds));
    }

    public static int chatEmotes(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setChatEmotesEnabled(enabled));
    }
}

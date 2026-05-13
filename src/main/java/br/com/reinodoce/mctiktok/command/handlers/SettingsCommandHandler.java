package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce settings ...` command execution.
 */
public final class SettingsCommandHandler {
    private SettingsCommandHandler() {
    }

    /**
     * Updates reconnect delay settings.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param seconds reconnect delay in seconds
     * @return Brigadier command result code
     */
    public static int reconnect(ReinodoceCommandService service, CommandSourceStack source, int seconds) {
        return CommandFeedback.sendResult(source, service.setReconnectSeconds(seconds));
    }

    /**
     * Updates chat inline-emote rendering settings.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether inline emotes should be enabled
     * @return Brigadier command result code
     */
    public static int chatEmotes(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setChatEmotesEnabled(enabled));
    }
}

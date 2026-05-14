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

    /**
     * Updates mirrored chat log settings.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether mirrored TikTok lines should be logged
     * @return Brigadier command result code
     */
    public static int chatLog(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setChatLogEnabled(enabled));
    }

    /**
     * Updates the visible LIVE chat prefix.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param prefix prefix text
     * @return Brigadier command result code
     */
    public static int prefix(ReinodoceCommandService service, CommandSourceStack source, String prefix) {
        return CommandFeedback.sendResult(source, service.setChatPrefix(prefix));
    }

    /**
     * Updates the LIVE chat format template.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param format format template
     * @return Brigadier command result code
     */
    public static int format(ReinodoceCommandService service, CommandSourceStack source, String format) {
        return CommandFeedback.sendResult(source, service.setChatFormat(format));
    }
}

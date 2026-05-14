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
     * Updates startup auto-connect settings.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether startup auto-connect should be enabled
     * @return Brigadier command result code
     */
    public static int autoConnect(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setAutoConnectOnStart(enabled));
    }

    /**
     * Shows the active language setting.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int language(ReinodoceCommandService service, CommandSourceStack source) {
        for (String line : service.languageLines()) {
            CommandFeedback.sendLine(source, line);
        }
        return 1;
    }

    /**
     * Updates the language setting.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param language {@code auto} or locale
     * @return Brigadier command result code
     */
    public static int language(ReinodoceCommandService service, CommandSourceStack source, String language) {
        return CommandFeedback.sendResult(source, service.setLanguage(language));
    }

    /**
     * Updates mirrored output mode.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param mode output mode identifier
     * @return Brigadier command result code
     */
    public static int output(ReinodoceCommandService service, CommandSourceStack source, String mode) {
        return CommandFeedback.sendResult(source, service.setOutputMode(mode));
    }

    /**
     * Updates HUD position.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param position HUD position identifier
     * @return Brigadier command result code
     */
    public static int hudPosition(ReinodoceCommandService service, CommandSourceStack source, String position) {
        return CommandFeedback.sendResult(source, service.setHudPosition(position));
    }

    /**
     * Updates retained HUD line count.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param lines HUD line count
     * @return Brigadier command result code
     */
    public static int hudLines(ReinodoceCommandService service, CommandSourceStack source, int lines) {
        return CommandFeedback.sendResult(source, service.setHudLines(lines));
    }

    /**
     * Updates pinned-message overlay visibility.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether pinned-message overlay rendering should be enabled
     * @return Brigadier command result code
     */
    public static int pinnedOverlay(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setPinnedOverlayEnabled(enabled));
    }

    /**
     * Updates pinned-message overlay position.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param position pinned-message overlay position identifier
     * @return Brigadier command result code
     */
    public static int pinnedOverlayPosition(
            ReinodoceCommandService service, CommandSourceStack source, String position
    ) {
        return CommandFeedback.sendResult(source, service.setPinnedOverlayPosition(position));
    }

    /**
     * Updates whether pinned messages are also sent through normal mirrored output.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether normal mirrored output should include pinned messages
     * @return Brigadier command result code
     */
    public static int pinnedOutput(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setPinnedMessagesInOutput(enabled));
    }

    /**
     * Updates visible pinned-message overlay count.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param messages pinned-message overlay item count
     * @return Brigadier command result code
     */
    public static int pinnedOverlayMessages(
            ReinodoceCommandService service, CommandSourceStack source, int messages
    ) {
        return CommandFeedback.sendResult(source, service.setPinnedOverlayMessages(messages));
    }

    /**
     * Opens the client settings GUI.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int gui(ReinodoceCommandService service, CommandSourceStack source) {
        return CommandFeedback.sendResult(source, service.openSettingsGui());
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

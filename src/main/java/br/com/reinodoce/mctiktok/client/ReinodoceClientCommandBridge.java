package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import br.com.reinodoce.mctiktok.core.ReinodoceCoreService;

import java.util.List;

/**
 * Delegates core-only command actions for the client command service.
 */
// Command-service bridge intentionally exposes one method per public command action.
@SuppressWarnings("PMD.ExcessivePublicCount")
abstract class ReinodoceClientCommandBridge implements ReinodoceCommandService {
    protected abstract ReinodoceCoreService coreService();

    @Override
    public CommandResult connect(String username) {
        return coreService().connect(username);
    }

    @Override
    public CommandResult connectLast() {
        return coreService().connectLast();
    }

    @Override
    public CommandResult disconnect() {
        return coreService().disconnect();
    }

    @Override
    public List<String> statsLines() {
        return coreService().statsLines();
    }

    @Override
    public CommandResult resetStats() {
        return coreService().resetStats();
    }

    @Override
    public CommandResult setReconnectSeconds(int seconds) {
        return coreService().setReconnectSeconds(seconds);
    }

    @Override
    public CommandResult setAutoConnectOnStart(boolean enabled) {
        return coreService().setAutoConnectOnStart(enabled);
    }

    @Override
    public CommandResult setHudPosition(String position) {
        return coreService().setHudPosition(position);
    }

    @Override
    public CommandResult setHudLines(int lines) {
        return coreService().setHudLines(lines);
    }

    @Override
    public CommandResult setPinnedOverlayPosition(String position) {
        return coreService().setPinnedOverlayPosition(position);
    }

    @Override
    public CommandResult setPinnedMessagesInOutput(boolean enabled) {
        return coreService().setPinnedMessagesInOutput(enabled);
    }

    @Override
    public CommandResult setPinnedOverlayMessages(int messages) {
        return coreService().setPinnedOverlayMessages(messages);
    }

    @Override
    public List<String> languageLines() {
        return coreService().languageLines();
    }

    @Override
    public CommandResult setLanguage(String language) {
        return coreService().setLanguage(language);
    }
}

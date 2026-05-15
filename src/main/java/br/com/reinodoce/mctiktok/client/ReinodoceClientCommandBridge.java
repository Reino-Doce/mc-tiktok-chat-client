package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
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

    @Override
    public CommandResult setFollowerRule(boolean enabled) {
        return coreService().setFollowerRule(enabled);
    }

    @Override
    public CommandResult setMinMemberLevelRule(int level) {
        return coreService().setMinMemberLevelRule(level);
    }

    @Override
    public CommandResult addBlockedWord(String word) {
        return coreService().addBlockedWord(word);
    }

    @Override
    public CommandResult removeBlockedWord(String word) {
        return coreService().removeBlockedWord(word);
    }

    @Override
    public List<String> blockedWordLines() {
        return coreService().blockedWordLines();
    }

    @Override
    public CommandResult addBlockedUser(String username) {
        return coreService().addBlockedUser(username);
    }

    @Override
    public CommandResult removeBlockedUser(String username) {
        return coreService().removeBlockedUser(username);
    }

    @Override
    public List<String> blockedUserLines() {
        return coreService().blockedUserLines();
    }

    @Override
    public CommandResult setMaxMessageLengthRule(int length) {
        return coreService().setMaxMessageLengthRule(length);
    }

    @Override
    public CommandResult setDuplicateCooldownRule(int seconds) {
        return coreService().setDuplicateCooldownRule(seconds);
    }

    @Override
    public CommandResult setSyntheticGift(int value) {
        return coreService().setSyntheticGift(value);
    }

    @Override
    public CommandResult setSyntheticGiftComboMode(String mode) {
        return coreService().setSyntheticGiftComboMode(mode);
    }

    @Override
    public CommandResult setSyntheticFollow(boolean enabled) {
        return coreService().setSyntheticFollow(enabled);
    }

    @Override
    public CommandResult setSyntheticJoin(boolean enabled) {
        return coreService().setSyntheticJoin(enabled);
    }

    @Override
    public CommandResult setSyntheticMemberLevel(boolean enabled) {
        return coreService().setSyntheticMemberLevel(enabled);
    }

    @Override
    public CommandResult setAlertSound(AlertEventType eventType, boolean enabled) {
        return coreService().setAlertSound(eventType, enabled);
    }

    @Override
    public CommandResult setAlertSoundId(AlertEventType eventType, String soundId) {
        return coreService().setAlertSoundId(eventType, soundId);
    }

    @Override
    public CommandResult setAlertToast(AlertEventType eventType, boolean enabled) {
        return coreService().setAlertToast(eventType, enabled);
    }

    @Override
    public CommandResult setAlertToastTemplate(AlertEventType eventType, String template) {
        return coreService().setAlertToastTemplate(eventType, template);
    }

    @Override
    public CommandResult setAlertMediaMode(AlertEventType eventType, String mediaMode) {
        return coreService().setAlertMediaMode(eventType, mediaMode);
    }

    @Override
    public CommandResult setAlertCustomImage(AlertEventType eventType, String customImage) {
        return coreService().setAlertCustomImage(eventType, customImage);
    }

    @Override
    public CommandResult setAlertGiftMinValue(int value) {
        return coreService().setAlertGiftMinValue(value);
    }
}

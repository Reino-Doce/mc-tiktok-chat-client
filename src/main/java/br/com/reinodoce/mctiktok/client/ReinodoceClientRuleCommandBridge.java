package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.command.CommandResult;

import java.util.List;

/**
 * Delegates moderation rule command actions for the client command service.
 */
// Command-service bridge intentionally exposes one method per public rule action.
@SuppressWarnings("PMD.ExcessivePublicCount")
abstract class ReinodoceClientRuleCommandBridge extends ReinodoceClientCommandBridge {
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
    public CommandResult importBlockedWords(String words) {
        return coreService().importBlockedWords(words);
    }

    @Override
    public List<String> blockedWordExportLines() {
        return coreService().blockedWordExportLines();
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
    public CommandResult importBlockedUsers(String usernames) {
        return coreService().importBlockedUsers(usernames);
    }

    @Override
    public List<String> blockedUserExportLines() {
        return coreService().blockedUserExportLines();
    }

    @Override
    public List<String> blockedUserLines() {
        return coreService().blockedUserLines();
    }

    @Override
    public CommandResult setEmoteOnlyFilterRule(boolean enabled) {
        return coreService().setEmoteOnlyFilterRule(enabled);
    }

    @Override
    public CommandResult setLinkFilterRule(boolean enabled) {
        return coreService().setLinkFilterRule(enabled);
    }

    @Override
    public CommandResult setUserCooldownRule(int seconds) {
        return coreService().setUserCooldownRule(seconds);
    }

    @Override
    public CommandResult setAllowlistModeRule(boolean enabled) {
        return coreService().setAllowlistModeRule(enabled);
    }

    @Override
    public CommandResult addAllowedUser(String username) {
        return coreService().addAllowedUser(username);
    }

    @Override
    public CommandResult removeAllowedUser(String username) {
        return coreService().removeAllowedUser(username);
    }

    @Override
    public List<String> allowedUserLines() {
        return coreService().allowedUserLines();
    }

    @Override
    public CommandResult setMaxMessageLengthRule(int length) {
        return coreService().setMaxMessageLengthRule(length);
    }

    @Override
    public CommandResult setDuplicateCooldownRule(int seconds) {
        return coreService().setDuplicateCooldownRule(seconds);
    }
}

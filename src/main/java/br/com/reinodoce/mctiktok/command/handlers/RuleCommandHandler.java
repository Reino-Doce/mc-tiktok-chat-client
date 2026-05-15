package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce rule ...` command execution.
 */
public final class RuleCommandHandler {
    private RuleCommandHandler() {
    }

    /**
     * Updates the follower-only display rule.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether follower-only mode should be enabled
     * @return Brigadier command result code
     */
    public static int follower(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setFollowerRule(enabled));
    }

    /**
     * Updates the minimum member-level display rule.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param level minimum allowed member level
     * @return Brigadier command result code
     */
    public static int minMemberLevel(ReinodoceCommandService service, CommandSourceStack source, int level) {
        return CommandFeedback.sendResult(source, service.setMinMemberLevelRule(level));
    }

    /**
     * Adds a blocked word filter.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param word blocked word fragment
     * @return Brigadier command result code
     */
    public static int addBlockedWord(ReinodoceCommandService service, CommandSourceStack source, String word) {
        return CommandFeedback.sendResult(source, service.addBlockedWord(word));
    }

    /**
     * Removes a blocked word filter.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param word blocked word fragment
     * @return Brigadier command result code
     */
    public static int removeBlockedWord(ReinodoceCommandService service, CommandSourceStack source, String word) {
        return CommandFeedback.sendResult(source, service.removeBlockedWord(word));
    }

    /**
     * Imports blocked word filters from comma-separated input.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param words comma-separated blocked word fragments
     * @return Brigadier command result code
     */
    public static int importBlockedWords(ReinodoceCommandService service, CommandSourceStack source, String words) {
        return CommandFeedback.sendResult(source, service.importBlockedWords(words));
    }

    /**
     * Exports blocked word filters as comma-separated text.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int exportBlockedWords(ReinodoceCommandService service, CommandSourceStack source) {
        for (String line : service.blockedWordExportLines()) {
            CommandFeedback.sendResult(source, CommandResult.ok(line));
        }
        return 1;
    }

    /**
     * Lists blocked word filters.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int listBlockedWords(ReinodoceCommandService service, CommandSourceStack source) {
        for (String line : service.blockedWordLines()) {
            CommandFeedback.sendResult(source, CommandResult.ok(line));
        }
        return 1;
    }

    /**
     * Adds a blocked user filter.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param username blocked username
     * @return Brigadier command result code
     */
    public static int addBlockedUser(ReinodoceCommandService service, CommandSourceStack source, String username) {
        return CommandFeedback.sendResult(source, service.addBlockedUser(username));
    }

    /**
     * Removes a blocked user filter.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param username blocked username
     * @return Brigadier command result code
     */
    public static int removeBlockedUser(ReinodoceCommandService service, CommandSourceStack source, String username) {
        return CommandFeedback.sendResult(source, service.removeBlockedUser(username));
    }

    /**
     * Imports blocked user filters from comma-separated input.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param usernames comma-separated usernames
     * @return Brigadier command result code
     */
    public static int importBlockedUsers(
            ReinodoceCommandService service, CommandSourceStack source, String usernames
    ) {
        return CommandFeedback.sendResult(source, service.importBlockedUsers(usernames));
    }

    /**
     * Exports blocked user filters as comma-separated text.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int exportBlockedUsers(ReinodoceCommandService service, CommandSourceStack source) {
        for (String line : service.blockedUserExportLines()) {
            CommandFeedback.sendResult(source, CommandResult.ok(line));
        }
        return 1;
    }

    /**
     * Lists blocked user filters.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int listBlockedUsers(ReinodoceCommandService service, CommandSourceStack source) {
        for (String line : service.blockedUserLines()) {
            CommandFeedback.sendResult(source, CommandResult.ok(line));
        }
        return 1;
    }

    /**
     * Updates emote-only filtering.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether emote-only comments should be hidden
     * @return Brigadier command result code
     */
    public static int emoteOnly(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setEmoteOnlyFilterRule(enabled));
    }

    /**
     * Updates link filtering.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether comments containing links should be hidden
     * @return Brigadier command result code
     */
    public static int links(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setLinkFilterRule(enabled));
    }

    /**
     * Updates repeated-user cooldown.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param seconds cooldown seconds
     * @return Brigadier command result code
     */
    public static int userCooldown(ReinodoceCommandService service, CommandSourceStack source, int seconds) {
        return CommandFeedback.sendResult(source, service.setUserCooldownRule(seconds));
    }

    /**
     * Updates user allowlist mode.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether allowlist mode should be enabled
     * @return Brigadier command result code
     */
    public static int allowlistMode(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setAllowlistModeRule(enabled));
    }

    /**
     * Adds an allowed user filter.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param username allowed username
     * @return Brigadier command result code
     */
    public static int addAllowedUser(ReinodoceCommandService service, CommandSourceStack source, String username) {
        return CommandFeedback.sendResult(source, service.addAllowedUser(username));
    }

    /**
     * Removes an allowed user filter.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param username allowed username
     * @return Brigadier command result code
     */
    public static int removeAllowedUser(ReinodoceCommandService service, CommandSourceStack source, String username) {
        return CommandFeedback.sendResult(source, service.removeAllowedUser(username));
    }

    /**
     * Lists allowed user filters.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int listAllowedUsers(ReinodoceCommandService service, CommandSourceStack source) {
        for (String line : service.allowedUserLines()) {
            CommandFeedback.sendResult(source, CommandResult.ok(line));
        }
        return 1;
    }

    /**
     * Updates maximum accepted message length.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param length maximum length
     * @return Brigadier command result code
     */
    public static int maxLength(ReinodoceCommandService service, CommandSourceStack source, int length) {
        return CommandFeedback.sendResult(source, service.setMaxMessageLengthRule(length));
    }

    /**
     * Updates duplicate message cooldown.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param seconds cooldown seconds
     * @return Brigadier command result code
     */
    public static int duplicateCooldown(ReinodoceCommandService service, CommandSourceStack source, int seconds) {
        return CommandFeedback.sendResult(source, service.setDuplicateCooldownRule(seconds));
    }
}

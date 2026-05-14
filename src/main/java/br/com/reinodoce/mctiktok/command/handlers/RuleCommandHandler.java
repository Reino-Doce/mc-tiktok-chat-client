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

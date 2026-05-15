package br.com.reinodoce.mctiktok.rules;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.util.UsernameValidator;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Applies operator-configured visibility and synthetic-output rules to TikTok events.
 */
public class MessageRuleEngine {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?:https?://|www\\.|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}\\b)");
    /**
     * Checks whether a live comment should be displayed.
     *
     * @param config active configuration
     * @param user TikTok user model
     * @param memberLevel resolved member level
     * @return true when the comment should be emitted
     */
    public boolean shouldDisplayComment(ReinodoceConfig config, User user, int memberLevel) {
        return shouldDisplayComment(config, user, memberLevel, "", "");
    }

    /**
     * Checks whether a live comment should be displayed.
     *
     * @param config active configuration
     * @param user TikTok user model
     * @param memberLevel resolved member level
     * @param username resolved display username
     * @param message plain comment body
     * @return true when the comment should be emitted
     */
    public boolean shouldDisplayComment(
            ReinodoceConfig config, User user, int memberLevel, String username, String message
    ) {
        return shouldDisplayComment(config, user, memberLevel, username, message, false);
    }

    /**
     * Checks whether a live comment should be displayed.
     *
     * @param config active configuration
     * @param user TikTok user model
     * @param memberLevel resolved member level
     * @param username resolved display username
     * @param message plain comment body
     * @param hasInlineMedia whether the comment contains inline media/emotes
     * @return true when the comment should be emitted
     */
    public boolean shouldDisplayComment(
            ReinodoceConfig config,
            User user,
            int memberLevel,
            String username,
            String message,
            boolean hasInlineMedia
    ) {
        if (config == null) {
            return true;
        }

        String canonicalUsername = canonicalUsername(user, username);
        return passesCommentAccountRules(config, user, memberLevel, canonicalUsername)
                && passesCommentContentRules(config, message, hasInlineMedia);
    }

    /**
     * Checks whether a gift should produce synthetic chat output.
     *
     * @param config active configuration
     * @param gift TikTok gift model
     * @return true when the gift passes the configured minimum value
     */
    public boolean shouldEmitGift(ReinodoceConfig config, Gift gift) {
        if (config == null) {
            return false;
        }

        int minValue = config.getSyntheticGiftMinValue();
        if (minValue <= 0) {
            return false;
        }
        return minValue == 1 || (gift != null && gift.getDiamondCost() >= minValue);
    }

    /**
     * Checks whether a gift should produce synthetic chat output for a specific user.
     *
     * @param config active configuration
     * @param gift TikTok gift model
     * @param user TikTok user model
     * @param username resolved display username
     * @return true when the gift passes configured value and allowlist rules
     */
    public boolean shouldEmitGift(ReinodoceConfig config, Gift gift, User user, String username) {
        return shouldEmitGift(config, gift) && shouldRouteSyntheticUser(config, user, username);
    }

    /**
     * Checks whether synthetic output for a user-originated event should be routed.
     *
     * @param config active configuration
     * @param user TikTok user model
     * @param username resolved display username
     * @return true when user routing rules allow output
     */
    public boolean shouldRouteSyntheticUser(ReinodoceConfig config, User user, String username) {
        return config == null || passesAllowlist(config, canonicalUsername(user, username));
    }

    private static boolean passesCommentAccountRules(
            ReinodoceConfig config, User user, int memberLevel, String username
    ) {
        if (config.isRuleFollowerOnly() && (user == null || !user.isFollower())) {
            return false;
        }
        if (!passesMemberLevel(config, memberLevel)) {
            return false;
        }
        return passesBlockedUser(config, username) && passesAllowlist(config, username);
    }

    private static boolean passesCommentContentRules(
            ReinodoceConfig config, String message, boolean hasInlineMedia
    ) {
        if (!passesBlockedWords(config, message)) {
            return false;
        }
        if (!passesMaxLength(config, message)) {
            return false;
        }
        return passesLinkFilter(config, message) && passesEmoteOnlyFilter(config, message, hasInlineMedia);
    }

    private static boolean passesMemberLevel(ReinodoceConfig config, int memberLevel) {
        return config.getRuleMinMemberLevel() <= 0 || memberLevel >= config.getRuleMinMemberLevel();
    }

    private static boolean passesBlockedUser(ReinodoceConfig config, String username) {
        return !config.getRuleBlockedUsers().contains(username);
    }

    private static boolean passesAllowlist(ReinodoceConfig config, String username) {
        return !config.isRuleAllowlistMode() || isAllowedUsername(config, username);
    }

    private static boolean isAllowedUsername(ReinodoceConfig config, String username) {
        return !username.isBlank() && config.getRuleAllowedUsers().contains(username);
    }

    private static String canonicalUsername(User user, String username) {
        String handle = user == null ? "" : UsernameValidator.normalize(user.getName());
        if (!UsernameValidator.isValid(handle)) {
            handle = UsernameValidator.normalize(username);
        }
        return UsernameValidator.isValid(handle) ? handle.toLowerCase(Locale.ROOT) : "";
    }

    private static boolean passesBlockedWords(ReinodoceConfig config, String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        for (String blockedWord : config.getRuleBlockedWords()) {
            if (!blockedWord.isBlank() && normalized.contains(blockedWord)) {
                return false;
            }
        }
        return true;
    }

    private static boolean passesMaxLength(ReinodoceConfig config, String message) {
        int maxLength = config.getRuleMaxMessageLength();
        return maxLength <= 0 || message == null || message.length() <= maxLength;
    }

    private static boolean passesLinkFilter(ReinodoceConfig config, String message) {
        return !config.isRuleLinkFilterEnabled()
                || message == null
                || !URL_PATTERN.matcher(message).find();
    }

    private static boolean passesEmoteOnlyFilter(
            ReinodoceConfig config, String message, boolean hasInlineMedia
    ) {
        if (!config.isRuleEmoteOnlyFilterEnabled()) {
            return true;
        }
        String text = message == null ? "" : message.trim();
        if (text.isBlank()) {
            return !hasInlineMedia;
        }
        return containsLetterOrDigit(text);
    }

    private static boolean containsLetterOrDigit(String value) {
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            if (Character.isLetterOrDigit(codePoint)) {
                return true;
            }
            index += Character.charCount(codePoint);
        }
        return false;
    }

}

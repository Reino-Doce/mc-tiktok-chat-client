package br.com.reinodoce.mctiktok.rules;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.Locale;

/**
 * Applies operator-configured visibility and synthetic-output rules to TikTok events.
 */
public class MessageRuleEngine {
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
        if (config == null) {
            return true;
        }

        if (config.isRuleFollowerOnly() && (user == null || !user.isFollower())) {
            return false;
        }

        return passesMemberLevel(config, memberLevel)
                && passesBlockedUser(config, username)
                && passesBlockedWords(config, message)
                && passesMaxLength(config, message);
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

    private static boolean passesMemberLevel(ReinodoceConfig config, int memberLevel) {
        return config.getRuleMinMemberLevel() <= 0 || memberLevel >= config.getRuleMinMemberLevel();
    }

    private static boolean passesBlockedUser(ReinodoceConfig config, String username) {
        String normalized = MessageSanitizer.sanitize(username).toLowerCase(Locale.ROOT);
        return !config.getRuleBlockedUsers().contains(normalized);
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
}

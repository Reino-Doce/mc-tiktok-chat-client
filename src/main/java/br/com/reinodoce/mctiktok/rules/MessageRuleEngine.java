package br.com.reinodoce.mctiktok.rules;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;

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
        if (config == null) {
            return true;
        }

        if (config.isRuleFollowerOnly() && (user == null || !user.isFollower())) {
            return false;
        }

        return config.getRuleMinMemberLevel() <= 0 || memberLevel >= config.getRuleMinMemberLevel();
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
}

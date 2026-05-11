package br.com.reinodoce.mctiktok.rules;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;

public class MessageRuleEngine {
    public boolean shouldDisplayComment(ReinodoceConfig config, User user, int memberLevel) {
        if (config == null) {
            return true;
        }

        if (config.isRuleFollowerOnly() && (user == null || !user.isFollower())) {
            return false;
        }

        return config.getRuleMinMemberLevel() <= 0 || memberLevel >= config.getRuleMinMemberLevel();
    }

    public boolean shouldEmitGift(ReinodoceConfig config, Gift gift) {
        if (config == null) {
            return false;
        }

        int minValue = config.getSynteticGiftMinValue();
        if (minValue <= 0) {
            return false;
        }
        return minValue == 1 || (gift != null && gift.getDiamondCost() >= minValue);
    }
}

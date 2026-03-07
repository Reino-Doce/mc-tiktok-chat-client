package br.com.reinodoce.mctiktok.rules;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.data.models.users.UserAttribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageRuleEngineTest {

    private final MessageRuleEngine engine = new MessageRuleEngine();

    @Test
    void commentRulesRespectFollowerAndMemberLevel() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setRuleFollowerOnly(true);
        config.setRuleMinMemberLevel(2);

        User follower = new User(10L, "alice");
        follower.addAttribute(UserAttribute.Follower);

        User nonFollower = new User(11L, "bob");

        assertTrue(engine.shouldDisplayComment(config, follower, 3));
        assertFalse(engine.shouldDisplayComment(config, follower, 1));
        assertFalse(engine.shouldDisplayComment(config, nonFollower, 3));
        assertFalse(engine.shouldDisplayComment(config, null, 3));
    }

    @Test
    void giftRuleUsesConfiguredMinimum() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        Gift cheapGift = new Gift(1, "Rose", 1, "");
        Gift expensiveGift = new Gift(2, "Galaxy", 10, "");

        config.setSynteticGiftMinValue(0);
        assertFalse(engine.shouldEmitGift(config, cheapGift));

        config.setSynteticGiftMinValue(1);
        assertTrue(engine.shouldEmitGift(config, cheapGift));

        config.setSynteticGiftMinValue(5);
        assertFalse(engine.shouldEmitGift(config, cheapGift));
        assertTrue(engine.shouldEmitGift(config, expensiveGift));
    }
}

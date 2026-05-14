package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.List;
import java.util.function.Supplier;

final class TikTokGiftEmitter {
    private static final String GIFT_UNKNOWN_KEY = "reinodoce.chat.gift_unknown";
    private static final String EMPTY_GIFT_PICTURE = "";

    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final MessageRuleEngine ruleEngine;
    private final MessageDeduplicator giftDeduplicator;
    private final RichLiveMessageFactory messageFactory;
    private final SessionStatsTracker statsTracker;

    TikTokGiftEmitter(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            MessageRuleEngine ruleEngine,
            MessageDeduplicator giftDeduplicator,
            RichLiveMessageFactory messageFactory,
            SessionStatsTracker statsTracker
    ) {
        this.configSupplier = configSupplier;
        this.chatGateway = chatGateway;
        this.ruleEngine = ruleEngine;
        this.giftDeduplicator = giftDeduplicator;
        this.messageFactory = messageFactory;
        this.statsTracker = statsTracker;
    }

    void emit(List<GiftComboAggregator.GiftEmission> emissions) {
        emit(configSupplier.get(), emissions);
    }

    void emitFromAsyncFlush(GiftComboAggregator.GiftEmission emission) {
        ReinodoceConfig config = configSupplier.get();
        if (!ruleEngine.shouldEmitGift(config, toGift(emission))) {
            return;
        }
        emit(config, List.of(emission));
    }

    GiftComboAggregator.GiftSnapshot toSnapshot(TikTokGiftEvent event) {
        User user = event.getUser();
        Gift gift = event.getGift();
        long userId = user == null || user.getId() == null ? 0L : user.getId();
        int giftId = gift == null ? 0 : gift.getId();
        int diamonds = gift == null ? 0 : Math.max(0, gift.getDiamondCost());
        int combo = Math.max(1, event.getCombo());
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(user));
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(user);
        String giftName = MessageSanitizer.sanitize(
                gift == null ? Translations.tr(GIFT_UNKNOWN_KEY) : gift.getName());
        String giftIconUrl = TikTokMediaResolver.resolveGiftIconUrl(gift);
        return new GiftComboAggregator.GiftSnapshot(
                new GiftComboAggregator.GiftKey(userId, giftId),
                username,
                avatarUrl,
                giftName,
                giftIconUrl,
                diamonds,
                combo,
                event.getMessageId());
    }

    private void emit(ReinodoceConfig config, List<GiftComboAggregator.GiftEmission> emissions) {
        for (GiftComboAggregator.GiftEmission emission : emissions) {
            if (giftDeduplicator.isDuplicate(emission.messageId(), emission.count())) {
                continue;
            }
            String username = TikTokUserNames.sanitizeUserName(emission.username());
            String giftName = MessageSanitizer.sanitize(emission.giftName());
            int count = Math.max(1, emission.count());
            if (config.isChatEmotesEnabled()) {
                chatGateway.sendSyntheticGift(
                        config,
                        messageFactory.richGiftMessage(
                                username, emission.avatarUrl(), giftName, emission.giftIconUrl(), count));
            } else {
                chatGateway.sendSyntheticGift(config, username, giftName, count);
            }
            statsTracker.recordGift(username, count, emission.diamondCost());
        }
    }

    private static Gift toGift(GiftComboAggregator.GiftEmission emission) {
        return new Gift(0, emission.giftName(), emission.diamondCost(), EMPTY_GIFT_PICTURE);
    }
}

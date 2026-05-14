package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftComboEvent;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokFollowEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokJoinEvent;
import io.github.jwdeveloper.tiktok.data.models.gifts.GiftComboStateType;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.function.LongPredicate;
import java.util.function.Supplier;

final class TikTokEventDispatcher {
    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final MessageRuleEngine ruleEngine;
    private final MemberLevelResolver memberLevelResolver;
    private final RenderedCommentTracker renderedTracker;
    private final RichLiveMessageFactory messageFactory;
    private final TikTokGiftEmitter giftEmitter;
    private final GiftComboAggregator giftComboAggregator;
    private final LongPredicate tokenCheck;

    TikTokEventDispatcher(Dependencies dependencies, LongPredicate tokenCheck) {
        this.configSupplier = dependencies.configSupplier();
        this.chatGateway = dependencies.chatGateway();
        this.ruleEngine = dependencies.ruleEngine();
        this.memberLevelResolver = dependencies.memberLevelResolver();
        this.renderedTracker = dependencies.renderedTracker();
        this.messageFactory = dependencies.messageFactory();
        this.giftEmitter = dependencies.giftEmitter();
        this.giftComboAggregator = dependencies.giftComboAggregator();
        this.tokenCheck = tokenCheck;
    }

    record Dependencies(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            RenderedCommentTracker renderedTracker,
            RichLiveMessageFactory messageFactory,
            TikTokGiftEmitter giftEmitter,
            GiftComboAggregator giftComboAggregator
    ) {
    }

    void onComment(long token, TikTokCommentEvent event) {
        String message = MessageSanitizer.sanitize(event.getText());
        if (!tokenCheck.test(token) || message.isBlank()) {
            return;
        }
        ReinodoceConfig config = configSupplier.get();
        User user = event.getUser();
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(user));
        if (!ruleEngine.shouldDisplayComment(config, user, memberLevelResolver.resolveLevel(user))
                || renderedTracker.wasRecentlyRendered(username, message)) {
            return;
        }
        renderedTracker.remember(username, message);
        sendComment(config, username, user, message);
    }

    void onFollow(long token, TikTokFollowEvent event) {
        ReinodoceConfig config = configSupplier.get();
        if (!tokenCheck.test(token) || !config.isSyntheticFollowEnabled()) {
            return;
        }
        sendSyntheticAuthorNotice(SyntheticAuthorKind.FOLLOW, config, event.getUser());
    }

    void onJoin(long token, TikTokJoinEvent event) {
        ReinodoceConfig config = configSupplier.get();
        if (!tokenCheck.test(token) || !config.isSyntheticJoinEnabled()) {
            return;
        }
        sendSyntheticAuthorNotice(SyntheticAuthorKind.JOIN, config, event.getUser());
    }

    void onGift(long token, TikTokGiftEvent event) {
        ReinodoceConfig config = configSupplier.get();
        if (!tokenCheck.test(token) || !ruleEngine.shouldEmitGift(config, event.getGift())) {
            return;
        }
        GiftComboMode mode = GiftComboMode.fromString(config.getSyntheticGiftComboMode());
        giftEmitter.emit(giftComboAggregator.handleGift(mode, giftEmitter.toSnapshot(event)));
    }

    void onGiftCombo(long token, TikTokGiftComboEvent event) {
        ReinodoceConfig config = configSupplier.get();
        if (!tokenCheck.test(token) || !ruleEngine.shouldEmitGift(config, event.getGift())) {
            return;
        }
        GiftComboMode mode = GiftComboMode.fromString(config.getSyntheticGiftComboMode());
        boolean finished = event.getComboState() == GiftComboStateType.Finished;
        giftEmitter.emit(giftComboAggregator.handleCombo(mode, giftEmitter.toSnapshot(event), finished));
    }

    private void sendComment(ReinodoceConfig config, String username, User user, String message) {
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendLiveComment(
                    config,
                    messageFactory.richTextMessage(
                            0L, username, TikTokMediaResolver.resolveUserAvatarUrl(user), message));
        } else {
            chatGateway.sendLiveComment(config, username, message);
        }
    }

    private void sendSyntheticAuthorNotice(SyntheticAuthorKind kind, ReinodoceConfig config, User user) {
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(user));
        if (config.isChatEmotesEnabled()) {
            RichLiveMessage rich = messageFactory.richAuthorOnlyMessage(
                    username, TikTokMediaResolver.resolveUserAvatarUrl(user));
            kind.sendRich(chatGateway, config, rich);
        } else {
            kind.sendPlain(chatGateway, config, username);
        }
    }

    private enum SyntheticAuthorKind {
        FOLLOW {
            @Override
            void sendRich(ChatEventSink sink, ReinodoceConfig config, RichLiveMessage rich) {
                sink.sendSyntheticFollow(config, rich);
            }

            @Override
            void sendPlain(ChatEventSink sink, ReinodoceConfig config, String username) {
                sink.sendSyntheticFollow(config, username);
            }
        },
        JOIN {
            @Override
            void sendRich(ChatEventSink sink, ReinodoceConfig config, RichLiveMessage rich) {
                sink.sendSyntheticJoin(config, rich);
            }

            @Override
            void sendPlain(ChatEventSink sink, ReinodoceConfig config, String username) {
                sink.sendSyntheticJoin(config, username);
            }
        };

        abstract void sendRich(ChatEventSink sink, ReinodoceConfig config, RichLiveMessage rich);

        abstract void sendPlain(ChatEventSink sink, ReinodoceConfig config, String username);
    }
}

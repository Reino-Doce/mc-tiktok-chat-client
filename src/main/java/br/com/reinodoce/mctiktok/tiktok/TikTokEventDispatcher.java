package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.logging.SessionLogEvent;
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
    private final Dependencies dependencies;
    private final LongPredicate tokenCheck;

    TikTokEventDispatcher(Dependencies dependencies, LongPredicate tokenCheck) {
        this.dependencies = dependencies;
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
            GiftComboAggregator giftComboAggregator,
            SessionStatsTracker statsTracker,
            ModerationDuplicateTracker moderationDuplicateTracker,
            SessionEventLogger sessionEventLogger,
            AlertService alertService
    ) {
    }

    void onComment(long token, TikTokCommentEvent event) {
        String message = MessageSanitizer.sanitize(event.getText());
        if (!tokenCheck.test(token) || message.isBlank()) {
            return;
        }
        ReinodoceConfig config = dependencies.configSupplier().get();
        User user = event.getUser();
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(user));
        int memberLevel = dependencies.memberLevelResolver().resolveLevel(user);
        if (!dependencies.ruleEngine().shouldDisplayComment(
                config, user, memberLevel, username, message)
                || dependencies.renderedTracker().wasRecentlyRendered(username, message)
                || dependencies.moderationDuplicateTracker().isDuplicate(
                        message, config.getRuleDuplicateCooldownSeconds())) {
            return;
        }
        dependencies.renderedTracker().remember(username, message);
        sendComment(config, username, user, message);
        dependencies.sessionEventLogger().log(SessionLogEvent.chat(username, message, memberLevel));
        dependencies.moderationDuplicateTracker().remember(message, config.getRuleDuplicateCooldownSeconds());
        dependencies.statsTracker().recordComment(TikTokUserNames.resolveUserId(user), username);
    }

    void onFollow(long token, TikTokFollowEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (!tokenCheck.test(token) || !config.isSyntheticFollowEnabled()) {
            return;
        }
        SyntheticAuthorNotice notice = sendSyntheticAuthorNotice(SyntheticAuthorKind.FOLLOW, config, event.getUser());
        dependencies.alertService().follow(config, notice.username(), notice.avatarUrl());
        dependencies.statsTracker().recordFollow();
    }

    void onJoin(long token, TikTokJoinEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (!tokenCheck.test(token) || !config.isSyntheticJoinEnabled()) {
            return;
        }
        SyntheticAuthorNotice notice = sendSyntheticAuthorNotice(SyntheticAuthorKind.JOIN, config, event.getUser());
        dependencies.alertService().join(config, notice.username(), notice.avatarUrl());
        dependencies.statsTracker().recordJoin();
    }

    void onGift(long token, TikTokGiftEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (!tokenCheck.test(token) || !dependencies.ruleEngine().shouldEmitGift(config, event.getGift())) {
            return;
        }
        GiftComboMode mode = GiftComboMode.fromString(config.getSyntheticGiftComboMode());
        dependencies.giftEmitter().emit(dependencies.giftComboAggregator().handleGift(
                mode, dependencies.giftEmitter().toSnapshot(event)));
    }

    void onGiftCombo(long token, TikTokGiftComboEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (!tokenCheck.test(token) || !dependencies.ruleEngine().shouldEmitGift(config, event.getGift())) {
            return;
        }
        GiftComboMode mode = GiftComboMode.fromString(config.getSyntheticGiftComboMode());
        boolean finished = event.getComboState() == GiftComboStateType.Finished;
        dependencies.giftEmitter().emit(dependencies.giftComboAggregator().handleCombo(
                mode, dependencies.giftEmitter().toSnapshot(event), finished));
    }

    void clearPendingGifts() {
        dependencies.giftComboAggregator().clear();
    }

    private void sendComment(ReinodoceConfig config, String username, User user, String message) {
        if (config.isChatEmotesEnabled()) {
            dependencies.chatGateway().sendLiveComment(
                    config,
                    dependencies.messageFactory().richTextMessage(
                            0L, username, TikTokMediaResolver.resolveUserAvatarUrl(user), message));
        } else {
            dependencies.chatGateway().sendLiveComment(config, username, message);
        }
    }

    private SyntheticAuthorNotice sendSyntheticAuthorNotice(SyntheticAuthorKind kind, ReinodoceConfig config, User user) {
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(user));
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(user);
        if (config.isChatEmotesEnabled()) {
            RichLiveMessage rich = dependencies.messageFactory().richAuthorOnlyMessage(
                    username, avatarUrl);
            kind.sendRich(dependencies.chatGateway(), config, rich);
        } else {
            kind.sendPlain(dependencies.chatGateway(), config, username);
        }
        dependencies.sessionEventLogger().log(kind.logEvent(username));
        return new SyntheticAuthorNotice(username, avatarUrl);
    }

    private record SyntheticAuthorNotice(String username, String avatarUrl) {
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

            @Override
            SessionLogEvent logEvent(String username) {
                return SessionLogEvent.follow(username);
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

            @Override
            SessionLogEvent logEvent(String username) {
                return SessionLogEvent.join(username);
            }
        };

        abstract void sendRich(ChatEventSink sink, ReinodoceConfig config, RichLiveMessage rich);

        abstract void sendPlain(ChatEventSink sink, ReinodoceConfig config, String username);

        abstract SessionLogEvent logEvent(String username);
    }
}

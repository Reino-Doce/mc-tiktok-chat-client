package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.logging.SessionLogEvent;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
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
            MessageDeduplicator commentDeduplicator,
            RichLiveMessageFactory messageFactory,
            TikTokGiftEmitter giftEmitter,
            GiftComboAggregator giftComboAggregator,
            SessionStatsTracker statsTracker,
            ModerationDuplicateTracker moderationDuplicateTracker,
            UserCooldownTracker userCooldownTracker,
            BurstOutputController burstOutputController,
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
        if (shouldSuppressComment(config, user, username, memberLevel, message)
                || isDuplicateTransportComment(event.getMessageId())) {
            return;
        }
        dependencies.sessionEventLogger().log(SessionLogEvent.chat(username, message, memberLevel));
        dependencies.statsTracker().recordComment(TikTokUserNames.resolveUserId(user), username);
        if (dependencies.burstOutputController().shouldShowComment(config)) {
            dependencies.renderedTracker().remember(username, message);
            sendComment(config, username, user, message);
            dependencies.moderationDuplicateTracker().remember(message, config.getRuleDuplicateCooldownSeconds());
            dependencies.userCooldownTracker().remember(user, username, config.getRuleUserCooldownSeconds());
        }
    }

    private boolean shouldSuppressComment(
            ReinodoceConfig config,
            User user,
            String username,
            int memberLevel,
            String message
    ) {
        return !dependencies.ruleEngine().shouldDisplayComment(
                config, user, memberLevel, username, message)
                || dependencies.renderedTracker().wasRecentlyRendered(username, message)
                || dependencies.moderationDuplicateTracker().isDuplicate(
                        message, config.getRuleDuplicateCooldownSeconds())
                || dependencies.userCooldownTracker().isCoolingDown(
                        user, username, config.getRuleUserCooldownSeconds());
    }

    private boolean isDuplicateTransportComment(long messageId) {
        // Keep TikTok transport-id dedupe ahead of burst gating so hidden duplicates do not reach stats/logs.
        return dependencies.commentDeduplicator().isDuplicate(messageId, 1);
    }

    void onFollow(long token, TikTokFollowEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (!tokenCheck.test(token) || !config.isSyntheticFollowEnabled()) {
            return;
        }
        SyntheticAuthorNotice notice = syntheticAuthorNotice(event.getUser());
        if (!dependencies.ruleEngine().shouldRouteSyntheticUser(config, event.getUser(), notice.username())) {
            return;
        }
        dependencies.statsTracker().recordFollow();
        dependencies.sessionEventLogger().log(SessionLogEvent.follow(notice.username()));
        if (dependencies.burstOutputController().shouldShowSynthetic(
                config,
                BurstOutputController.SyntheticBurstKind.FOLLOW,
                this::flushSyntheticSummary)) {
            sendSyntheticAuthorNotice(SyntheticAuthorKind.FOLLOW, config, notice);
        }
        dependencies.alertService().follow(config, notice.username(), notice.avatarUrl());
    }

    void onJoin(long token, TikTokJoinEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (!tokenCheck.test(token) || !config.isSyntheticJoinEnabled()) {
            return;
        }
        SyntheticAuthorNotice notice = syntheticAuthorNotice(event.getUser());
        if (!dependencies.ruleEngine().shouldRouteSyntheticUser(config, event.getUser(), notice.username())) {
            return;
        }
        dependencies.statsTracker().recordJoin();
        dependencies.sessionEventLogger().log(SessionLogEvent.join(notice.username()));
        if (dependencies.burstOutputController().shouldShowSynthetic(
                config,
                BurstOutputController.SyntheticBurstKind.JOIN,
                this::flushSyntheticSummary)) {
            sendSyntheticAuthorNotice(SyntheticAuthorKind.JOIN, config, notice);
        }
        dependencies.alertService().join(config, notice.username(), notice.avatarUrl());
    }

    void onGift(long token, TikTokGiftEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(event.getUser()));
        if (!tokenCheck.test(token)
                || !dependencies.ruleEngine().shouldEmitGift(config, event.getGift(), event.getUser(), username)) {
            return;
        }
        GiftComboMode mode = GiftComboMode.fromString(config.getSyntheticGiftComboMode());
        dependencies.giftEmitter().emit(dependencies.giftComboAggregator().handleGift(
                mode, dependencies.giftEmitter().toSnapshot(event)));
    }

    void onGiftCombo(long token, TikTokGiftComboEvent event) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        String username = TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(event.getUser()));
        if (!tokenCheck.test(token)
                || !dependencies.ruleEngine().shouldEmitGift(config, event.getGift(), event.getUser(), username)) {
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

    void clearPendingBurstOutput() {
        dependencies.burstOutputController().clear();
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

    private void sendSyntheticAuthorNotice(
            SyntheticAuthorKind kind, ReinodoceConfig config, SyntheticAuthorNotice notice
    ) {
        if (config.isChatEmotesEnabled()) {
            RichLiveMessage rich = dependencies.messageFactory().richAuthorOnlyMessage(
                    notice.username(), notice.avatarUrl());
            kind.sendRich(dependencies.chatGateway(), config, rich);
        } else {
            kind.sendPlain(dependencies.chatGateway(), config, notice.username());
        }
    }

    private void flushSyntheticSummary(BurstOutputController.SyntheticSummary summary) {
        ReinodoceConfig config = dependencies.configSupplier().get();
        if (shouldSendSyntheticSummary(config, summary.kind())) {
            sendSyntheticSummary(config, summary);
        }
    }

    static boolean shouldSendSyntheticSummary(
            ReinodoceConfig config, BurstOutputController.SyntheticBurstKind kind
    ) {
        boolean enabled = config != null && OutputMode.fromString(config.getOutputMode()) != OutputMode.OFF;
        if (enabled) {
            enabled = switch (kind) {
                case FOLLOW -> config.isSyntheticFollowEnabled();
                case JOIN -> config.isSyntheticJoinEnabled();
            };
        }
        return enabled;
    }

    private void sendSyntheticSummary(
            ReinodoceConfig config, BurstOutputController.SyntheticSummary summary
    ) {
        String messageKey = switch (summary.kind()) {
            case FOLLOW -> "reinodoce.chat.follow_burst_summary";
            case JOIN -> "reinodoce.chat.join_burst_summary";
            default -> throw new IllegalStateException("Unexpected synthetic burst kind: " + summary.kind());
        };
        String username = dependencies.messageFactory().translate("reinodoce.chat.burst_summary_user");
        String message = dependencies.messageFactory().translate(
                messageKey, summary.groupedCount(), summary.suppressedCount());
        dependencies.chatGateway().sendLiveComment(config, username, message);
    }

    private static SyntheticAuthorNotice syntheticAuthorNotice(User user) {
        return new SyntheticAuthorNotice(
                TikTokUserNames.sanitizeUserName(TikTokUserNames.resolveUserName(user)),
                TikTokMediaResolver.resolveUserAvatarUrl(user));
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

package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.logging.SessionLogEvent;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.function.Supplier;

final class LiveCommentEmitter {
    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final MessageRuleEngine ruleEngine;
    private final MessageDeduplicator commentDeduplicator;
    private final RenderedCommentTracker renderedTracker;
    private final RichLiveMessageFactory messageFactory;
    private final SessionStatsTracker statsTracker;
    private final ModerationDuplicateTracker moderationDuplicateTracker;
    private final UserCooldownTracker userCooldownTracker;
    private final BurstOutputController burstOutputController;
    private final SessionEventLogger sessionEventLogger;

    LiveCommentEmitter(Dependencies dependencies) {
        this.configSupplier = dependencies.configSupplier();
        this.chatGateway = dependencies.chatGateway();
        this.ruleEngine = dependencies.ruleEngine();
        this.commentDeduplicator = dependencies.commentDeduplicator();
        this.renderedTracker = dependencies.renderedTracker();
        this.messageFactory = dependencies.messageFactory();
        this.statsTracker = dependencies.statsTracker();
        this.moderationDuplicateTracker = dependencies.moderationDuplicateTracker();
        this.userCooldownTracker = dependencies.userCooldownTracker();
        this.burstOutputController = dependencies.burstOutputController();
        this.sessionEventLogger = dependencies.sessionEventLogger();
    }

    void emit(EmissionContext context) {
        if (context.richMessage() == null) {
            return;
        }
        ReinodoceConfig config = configSupplier.get();
        String plainText = MessageSanitizer.sanitize(context.richMessage().plainText());
        if (!shouldLogAccepted(config, context, plainText)) {
            return;
        }
        sessionEventLogger.log(logEvent(context, plainText));
        statsTracker.recordComment(TikTokUserNames.resolveUserId(context.user()), context.username());
        if (burstOutputController.shouldShowComment(config)) {
            renderedTracker.remember(context.username(), plainText);
            sendComment(config, context, plainText);
            moderationDuplicateTracker.remember(plainText, config.getRuleDuplicateCooldownSeconds());
            userCooldownTracker.remember(context.user(), context.username(), config.getRuleUserCooldownSeconds());
        }
    }

    private boolean shouldLogAccepted(ReinodoceConfig config, EmissionContext context, String plainText) {
        return shouldAccept(config, context, plainText) && isUniqueTransportComment(context);
    }

    private boolean shouldAccept(ReinodoceConfig config, EmissionContext context, String plainText) {
        if (!ruleEngine.shouldDisplayComment(
                config,
                context.user(),
                context.memberLevel(),
                context.username(),
                plainText,
                context.richMessage().hasInlineMedia())) {
            return false;
        }
        if (plainText.isBlank() && !context.richMessage().hasInlineMedia()) {
            return false;
        }
        return !moderationDuplicateTracker.isDuplicate(plainText, config.getRuleDuplicateCooldownSeconds())
                && !userCooldownTracker.isCoolingDown(
                        context.user(), context.username(), config.getRuleUserCooldownSeconds());
    }

    private boolean isUniqueTransportComment(EmissionContext context) {
        // Keep TikTok transport-id dedupe ahead of burst gating so hidden duplicates do not reach stats/logs.
        return !commentDeduplicator.isDuplicate(context.richMessage().messageId(), 1);
    }

    private void sendComment(ReinodoceConfig config, EmissionContext context, String plainText) {
        if (config.isChatEmotesEnabled()) {
            sendRichComment(config, context);
            return;
        }
        if (!plainText.isBlank()) {
            sendPlainComment(config, context, plainText);
        }
    }

    private void sendRichComment(ReinodoceConfig config, EmissionContext context) {
        RichLiveMessage enriched = messageFactory.enrichCommentMessage(
                context.username(), context.avatarUrl(), context.richMessage());
        if (context.starComment()) {
            chatGateway.sendStarComment(config, enriched);
        } else {
            chatGateway.sendLiveComment(config, enriched);
        }
    }

    private void sendPlainComment(ReinodoceConfig config, EmissionContext context, String plainText) {
        if (context.starComment()) {
            chatGateway.sendStarComment(config, context.username(), plainText);
        } else {
            chatGateway.sendLiveComment(config, context.username(), plainText);
        }
    }

    private static SessionLogEvent logEvent(EmissionContext context, String plainText) {
        if (context.starComment()) {
            return SessionLogEvent.starComment(context.username(), plainText, context.memberLevel());
        }
        return SessionLogEvent.chat(context.username(), plainText, context.memberLevel());
    }

    record EmissionContext(
            User user,
            String username,
            String avatarUrl,
            int memberLevel,
            RichLiveMessage richMessage,
            boolean starComment
    ) {
    }

    record Dependencies(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            MessageRuleEngine ruleEngine,
            MessageDeduplicator commentDeduplicator,
            RenderedCommentTracker renderedTracker,
            RichLiveMessageFactory messageFactory,
            SessionStatsTracker statsTracker,
            ModerationDuplicateTracker moderationDuplicateTracker,
            UserCooldownTracker userCooldownTracker,
            BurstOutputController burstOutputController,
            SessionEventLogger sessionEventLogger
    ) {
    }
}

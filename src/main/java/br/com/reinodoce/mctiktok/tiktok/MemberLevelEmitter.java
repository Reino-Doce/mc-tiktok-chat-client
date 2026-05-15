package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.logging.SessionLogEvent;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.util.function.Supplier;

final class MemberLevelEmitter {
    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final MessageRuleEngine ruleEngine;
    private final RichLiveMessageFactory messageFactory;
    private final SessionStatsTracker statsTracker;
    private final SessionEventLogger sessionEventLogger;
    private final AlertService alertService;

    MemberLevelEmitter(
            Supplier<ReinodoceConfig> configSupplier,
            TikTokRuntimeServices runtimeServices,
            MessageRuleEngine ruleEngine,
            RichLiveMessageFactory messageFactory,
            SessionStatsTracker statsTracker
    ) {
        this.configSupplier = configSupplier;
        this.chatGateway = runtimeServices.chatGateway();
        this.ruleEngine = ruleEngine;
        this.messageFactory = messageFactory;
        this.statsTracker = statsTracker;
        this.sessionEventLogger = runtimeServices.sessionEventLogger();
        this.alertService = runtimeServices.alertService();
    }

    boolean emit(MemberLevelResolver.LevelUpdate update) {
        if (!update.isLevelIncrease()) {
            logSkipped("not-level-increase", update);
            return false;
        }
        ReinodoceConfig config = configSupplier.get();
        if (!config.isSyntheticMemberLevelEnabled()) {
            logSkipped("disabled", update);
            return false;
        }
        String username = TikTokUserNames.sanitizeUserName(update.username());
        if (!ruleEngine.shouldRouteSyntheticUser(config, null, update.accountUsername())) {
            logSkipped("allowlist", update);
            return false;
        }
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendSyntheticMemberLevel(
                    config,
                    messageFactory.richAuthorOnlyMessage(username, update.avatarUrl()),
                    update.newLevel());
        } else {
            chatGateway.sendSyntheticMemberLevel(config, username, update.newLevel());
        }
        sessionEventLogger.log(SessionLogEvent.memberLevel(username, update.newLevel()));
        statsTracker.recordMemberLevel();
        alertService.memberLevel(config, username, update.newLevel(), update.avatarUrl());
        return true;
    }

    private static void logSkipped(String reason, MemberLevelResolver.LevelUpdate update) {
        ReinodoceLogger.LOGGER.debug(
                "Member-level synthetic message skipped reason={} userId={} previousLevel={} newLevel={}",
                reason,
                update.userId(),
                update.previousLevel(),
                update.newLevel());
    }
}

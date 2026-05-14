package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.util.function.Supplier;

final class MemberLevelEmitter {
    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final RichLiveMessageFactory messageFactory;

    MemberLevelEmitter(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            RichLiveMessageFactory messageFactory
    ) {
        this.configSupplier = configSupplier;
        this.chatGateway = chatGateway;
        this.messageFactory = messageFactory;
    }

    boolean emit(MemberLevelResolver.LevelUpdate update) {
        if (!update.isLevelIncrease()) {
            logSkipped("not-level-increase", update);
            return false;
        }
        ReinodoceConfig config = configSupplier.get();
        if (!config.isSynteticMemberLevelEnabled()) {
            logSkipped("disabled", update);
            return false;
        }
        String username = TikTokUserNames.sanitizeUserName(update.username());
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendSyntheticMemberLevel(
                    config,
                    messageFactory.richAuthorOnlyMessage(username, update.avatarUrl()),
                    update.newLevel());
            return true;
        }
        chatGateway.sendSyntheticMemberLevel(config, username, update.newLevel());
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

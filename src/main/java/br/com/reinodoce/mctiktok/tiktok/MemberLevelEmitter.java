package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

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
        if (!update.isUpgrade() || update.newLevel() <= 0) {
            return false;
        }
        ReinodoceConfig config = configSupplier.get();
        if (!config.isSynteticMemberLevelEnabled()) {
            return false;
        }
        String username = TikTokUserNames.sanitizeUserName(update.username());
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendSyntheticMemberLevel(
                    config.getChatPrefix(),
                    messageFactory.richAuthorOnlyMessage(username, update.avatarUrl()),
                    update.newLevel());
            return true;
        }
        chatGateway.sendSyntheticMemberLevel(config.getChatPrefix(), username, update.newLevel());
        return true;
    }
}

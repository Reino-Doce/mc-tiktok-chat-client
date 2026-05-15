package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.pinned.PinnedLiveMessage;
import br.com.reinodoce.mctiktok.pinned.PinnedMessageSink;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastRoomPinMessage;

import java.time.Duration;
import java.util.function.Supplier;

final class PinnedMessageEmitter {
    private static final long UNKNOWN_PIN_ID = 0L;
    private static final long NO_DISPLAY_DURATION_SECONDS = 0L;
    private static final String INLINE_MEDIA_FALLBACK = "[media]";

    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final PinnedMessageSink pinnedMessageSink;
    private final MessageRuleEngine ruleEngine;
    private final MemberLevelResolver memberLevelResolver;
    private final TikTokRichMessageParser richMessageParser;
    private final RichLiveMessageFactory messageFactory;

    PinnedMessageEmitter(Dependencies dependencies) {
        this.configSupplier = dependencies.configSupplier();
        this.chatGateway = dependencies.chatGateway();
        this.pinnedMessageSink = dependencies.pinnedMessageSink();
        this.ruleEngine = dependencies.ruleEngine();
        this.memberLevelResolver = dependencies.memberLevelResolver();
        this.richMessageParser = dependencies.richMessageParser();
        this.messageFactory = dependencies.messageFactory();
    }

    void emit(WebcastRoomPinMessage roomPinMessage) {
        if (roomPinMessage == null || !roomPinMessage.hasChatMessage()) {
            return;
        }
        ReinodoceConfig config = configSupplier.get();
        WebcastChatMessage chatMessage = roomPinMessage.getChatMessage();
        User user = User.map(chatMessage.getUser(), chatMessage.getUserIdentity());
        String username = resolveUsername(chatMessage);
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(chatMessage.getUser());
        int memberLevel = resolveMemberLevel(user, username, avatarUrl, chatMessage);
        RichLiveMessage richMessage = richMessageParser.parseChatMessage(chatMessage, username);
        String plainText = MessageSanitizer.sanitize(richMessage.plainText());
        String overlayText = overlayText(plainText, richMessage);
        PinnedCommentCandidate candidate = new PinnedCommentCandidate(
                user, memberLevel, username, plainText, overlayText, richMessage.hasInlineMedia());
        if (!shouldEmit(config, candidate)) {
            return;
        }

        if (config.isPinnedOverlayEnabled()) {
            pinnedMessageSink.showPinnedMessage(config, new PinnedLiveMessage(
                    resolvePinId(roomPinMessage, richMessage),
                    username,
                    overlayText,
                    displayDuration(roomPinMessage)));
        }
        if (config.isPinnedMessagesInOutput()) {
            sendMirroredPinnedMessage(config, username, avatarUrl, overlayText, richMessage);
        }
    }

    private int resolveMemberLevel(
            User user,
            String username,
            String avatarUrl,
            WebcastChatMessage chatMessage
    ) {
        memberLevelResolver.updateLevel(
                chatMessage.getUser().getId(),
                username,
                chatMessage.getUser().getUsername(),
                avatarUrl,
                (int) chatMessage.getUser().getFansClubInfo().getFansLevel());
        return memberLevelResolver.resolveLevel(user);
    }

    private boolean shouldEmit(
            ReinodoceConfig config, PinnedCommentCandidate candidate
    ) {
        return !candidate.outputMessage().isBlank()
                && ruleEngine.shouldDisplayComment(
                        config,
                        candidate.user(),
                        candidate.memberLevel(),
                        candidate.username(),
                        candidate.ruleMessage(),
                        candidate.hasInlineMedia());
    }

    private void sendMirroredPinnedMessage(
            ReinodoceConfig config,
            String username,
            String avatarUrl,
            String plainText,
            RichLiveMessage richMessage
    ) {
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendPinnedComment(
                    config,
                    messageFactory.enrichCommentMessage(username, avatarUrl, richMessage));
        } else if (!plainText.isBlank()) {
            chatGateway.sendPinnedComment(config, username, plainText);
        }
    }

    private static String resolveUsername(WebcastChatMessage chatMessage) {
        return TikTokUserNames.sanitizeUserName(
                TikTokUserNames.chooseRawUserName(
                        chatMessage.getUser().getNickname(), chatMessage.getUser().getUsername()));
    }

    private static long resolvePinId(WebcastRoomPinMessage roomPinMessage, RichLiveMessage richMessage) {
        long pinId = roomPinMessage.getPinId();
        return pinId == UNKNOWN_PIN_ID ? richMessage.messageId() : pinId;
    }

    private static String overlayText(String plainText, RichLiveMessage richMessage) {
        if (!plainText.isBlank() || !richMessage.hasInlineMedia()) {
            return plainText;
        }
        for (RichLiveMessage.Segment segment : richMessage.bodySegments()) {
            String fallback = MessageSanitizer.sanitize(segment.plainText());
            if (!fallback.isBlank()) {
                return fallback;
            }
        }
        return INLINE_MEDIA_FALLBACK;
    }

    private static Duration displayDuration(WebcastRoomPinMessage roomPinMessage) {
        long displayDuration = roomPinMessage.getDisplayDuration();
        if (displayDuration <= NO_DISPLAY_DURATION_SECONDS) {
            return PinnedLiveMessage.DEFAULT_DURATION;
        }
        return Duration.ofSeconds(displayDuration);
    }

    record Dependencies(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            PinnedMessageSink pinnedMessageSink,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            TikTokRichMessageParser richMessageParser,
            RichLiveMessageFactory messageFactory
    ) {
    }

    private record PinnedCommentCandidate(
            User user,
            int memberLevel,
            String username,
            String ruleMessage,
            String outputMessage,
            boolean hasInlineMedia
    ) {
    }
}

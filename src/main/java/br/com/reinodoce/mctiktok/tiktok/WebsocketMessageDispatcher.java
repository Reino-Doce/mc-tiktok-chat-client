package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jwdeveloper.tiktok.data.events.websocket.TikTokWebsocketMessageEvent;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastEmoteChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastMemberMessage;

final class WebsocketMessageDispatcher {
    private static final String CHAT_METHOD = "WebcastChatMessage";
    private static final String EMOTE_METHOD = "WebcastEmoteChatMessage";
    private static final String BARRAGE_METHOD = "WebcastBarrageMessage";
    private static final String MEMBER_METHOD = "WebcastMemberMessage";

    private final TikTokRichMessageParser richMessageParser;
    private final MemberLevelResolver memberLevelResolver;
    private final MemberLevelEmitter memberLevelEmitter;
    private final LiveCommentEmitter liveCommentEmitter;
    private final BarrageMessageHandler barrageHandler;
    private final MemberMessageHandler memberHandler;

    WebsocketMessageDispatcher(
            TikTokRichMessageParser richMessageParser,
            MemberLevelResolver memberLevelResolver,
            MemberLevelEmitter memberLevelEmitter,
            LiveCommentEmitter liveCommentEmitter,
            BarrageMessageHandler barrageHandler,
            MemberMessageHandler memberHandler
    ) {
        this.richMessageParser = richMessageParser;
        this.memberLevelResolver = memberLevelResolver;
        this.memberLevelEmitter = memberLevelEmitter;
        this.liveCommentEmitter = liveCommentEmitter;
        this.barrageHandler = barrageHandler;
        this.memberHandler = memberHandler;
    }

    void dispatch(TikTokWebsocketMessageEvent event) {
        if (event == null || event.getMessage() == null || event.getMessage().getPayload() == null) {
            return;
        }
        String method = event.getMessage().getMethod();
        byte[] payload = event.getMessage().getPayload().toByteArray();
        try {
            dispatchByMethod(method, payload);
        } catch (InvalidProtocolBufferException ignored) {
            ReinodoceLogger.LOGGER.debug("Ignoring websocket payload parse failure for method {}", method);
        }
    }

    private void dispatchByMethod(String method, byte[] payload) throws InvalidProtocolBufferException {
        switch (method) {
            case CHAT_METHOD -> handleChat(WebcastChatMessage.parseFrom(payload));
            case EMOTE_METHOD -> handleEmote(WebcastEmoteChatMessage.parseFrom(payload));
            case BARRAGE_METHOD -> barrageHandler.dispatch(WebcastBarrageMessage.parseFrom(payload));
            case MEMBER_METHOD -> memberHandler.handle(WebcastMemberMessage.parseFrom(payload));
            default -> { /* unrecognized websocket method; ignored. */ }
        }
    }

    private void handleChat(WebcastChatMessage chatMessage) {
        User user = User.map(chatMessage.getUser(), chatMessage.getUserIdentity());
        String username = TikTokUserNames.sanitizeUserName(
                TikTokUserNames.chooseRawUserName(
                        chatMessage.getUser().getNickname(), chatMessage.getUser().getUsername()));
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(chatMessage.getUser());
        memberLevelEmitter.emit(memberLevelResolver.updateLevel(
                chatMessage.getUser().getId(),
                username,
                avatarUrl,
                (int) chatMessage.getUser().getFansClubInfo().getFansLevel()));
        RichLiveMessage richMessage = richMessageParser.parseChatMessage(chatMessage, username);
        liveCommentEmitter.emit(new LiveCommentEmitter.EmissionContext(
                user, username, avatarUrl, memberLevelResolver.resolveLevel(user), richMessage, false));
    }

    private void handleEmote(WebcastEmoteChatMessage emoteChatMessage) {
        User user = User.map(emoteChatMessage.getUser(), emoteChatMessage.getUserIdentity());
        String username = TikTokUserNames.sanitizeUserName(
                TikTokUserNames.chooseRawUserName(
                        emoteChatMessage.getUser().getNickname(), emoteChatMessage.getUser().getUsername()));
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(emoteChatMessage.getUser());
        RichLiveMessage richMessage = richMessageParser.parseEmoteChatMessage(emoteChatMessage, username);
        liveCommentEmitter.emit(new LiveCommentEmitter.EmissionContext(
                user, username, avatarUrl, memberLevelResolver.resolveLevel(user), richMessage, false));
    }
}

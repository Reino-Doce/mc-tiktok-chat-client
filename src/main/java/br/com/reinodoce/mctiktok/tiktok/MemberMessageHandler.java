package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastMemberMessage;

final class MemberMessageHandler {
    private final TikTokRichMessageParser richMessageParser;
    private final MemberLevelResolver memberLevelResolver;
    private final MemberLevelEmitter memberLevelEmitter;

    MemberMessageHandler(
            TikTokRichMessageParser richMessageParser,
            MemberLevelResolver memberLevelResolver,
            MemberLevelEmitter memberLevelEmitter
    ) {
        this.richMessageParser = richMessageParser;
        this.memberLevelResolver = memberLevelResolver;
        this.memberLevelEmitter = memberLevelEmitter;
    }

    void handle(WebcastMemberMessage memberMessage) {
        if (memberMessage == null) {
            return;
        }
        io.github.jwdeveloper.tiktok.messages.data.User rawUser = resolveRawUser(memberMessage);
        long userId = resolveUserId(rawUser, memberMessage);
        String username = resolveUsername(userId, rawUser);
        String avatarUrl = resolveAvatarUrl(userId, rawUser);
        String levelText = extractText(memberMessage);
        int level = MemberLevelResolver.extractLevelFromText(levelText);
        if (level <= 0) {
            logNotRendered(memberMessage, levelText);
            return;
        }
        MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(
                userId,
                username,
                rawUser == null ? "" : rawUser.getUsername(),
                avatarUrl,
                level);
        if (!memberLevelEmitter.emit(update)) {
            logNotRendered(memberMessage, levelText);
        }
    }

    private String extractText(WebcastMemberMessage memberMessage) {
        return TikTokClientFacade.extractMemberMessageText(memberMessage, richMessageParser);
    }

    private static io.github.jwdeveloper.tiktok.messages.data.User resolveRawUser(WebcastMemberMessage memberMessage) {
        return memberMessage.hasUser() && TikTokUserNames.isKnownRawUser(memberMessage.getUser())
                ? memberMessage.getUser()
                : null;
    }

    private static long resolveUserId(
            io.github.jwdeveloper.tiktok.messages.data.User rawUser, WebcastMemberMessage memberMessage
    ) {
        return rawUser != null && rawUser.getId() > 0 ? rawUser.getId() : memberMessage.getUserId();
    }

    private String resolveUsername(long userId, io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        String fallback = rawUser == null
                ? TikTokUserNames.sanitizeUserName("")
                : TikTokUserNames.chooseRawUserName(rawUser.getNickname(), rawUser.getUsername());
        return TikTokUserNames.sanitizeUserName(memberLevelResolver.getKnownUsername(userId, fallback));
    }

    private String resolveAvatarUrl(long userId, io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        String fallback = rawUser == null
                ? TikTokMediaResolver.defaultAvatarUrl()
                : TikTokMediaResolver.resolveUserAvatarUrl(rawUser);
        return memberLevelResolver.getKnownAvatarUrl(userId, fallback);
    }

    private static void logNotRendered(WebcastMemberMessage memberMessage, String text) {
        ReinodoceLogger.LOGGER.debug(
                "Recognized member payload not rendered action={} text={}",
                memberMessage.getAction(),
                MessageSanitizer.sanitize(text));
    }
}

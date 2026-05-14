package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.messages.data.CommonMessageData;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage.BarrageTypeFansLevelParam;

final class BarrageMessageHandler {
    private final TikTokRichMessageParser richMessageParser;
    private final MemberLevelResolver memberLevelResolver;
    private final MemberLevelEmitter memberLevelEmitter;
    private final LiveCommentEmitter liveCommentEmitter;

    BarrageMessageHandler(
            TikTokRichMessageParser richMessageParser,
            MemberLevelResolver memberLevelResolver,
            MemberLevelEmitter memberLevelEmitter,
            LiveCommentEmitter liveCommentEmitter
    ) {
        this.richMessageParser = richMessageParser;
        this.memberLevelResolver = memberLevelResolver;
        this.memberLevelEmitter = memberLevelEmitter;
        this.liveCommentEmitter = liveCommentEmitter;
    }

    void dispatch(WebcastBarrageMessage barrageMessage) {
        if (barrageMessage == null) {
            return;
        }
        switch (barrageMessage.getMsgType()) {
            case COMMONBARRAGE -> handleStarBarrageComment(barrageMessage);
            case FANSLEVELUPGRADE -> handleFansLevelUpgradeBarrage(barrageMessage);
            default -> { /* unrelated barrage payloads are not rendered. */ }
        }
    }

    private void handleStarBarrageComment(WebcastBarrageMessage barrageMessage) {
        TikTokRichMessageParser.ParsedText parsed = richMessageParser.parseBarrageText(barrageMessage);
        if (!parsed.hasRenderableContent()) {
            logNotRendered(barrageMessage, parsed.plainText());
            return;
        }
        io.github.jwdeveloper.tiktok.messages.data.User rawUser = resolveBarrageRawUser(parsed, barrageMessage);
        User user = rawUser == null ? null : User.map(rawUser);
        String username = resolveBarrageUsername(parsed, rawUser);
        String avatarUrl = resolveBarrageAvatarUrl(parsed, rawUser);
        int memberLevel = resolveBarrageMemberLevel(user, rawUser, barrageMessage);
        RichLiveMessage richMessage = new RichLiveMessage(
                resolveMessageId(barrageMessage.hasCommon() ? barrageMessage.getCommon() : null),
                username,
                parsed.segments());
        liveCommentEmitter.emit(new LiveCommentEmitter.EmissionContext(
                user, username, avatarUrl, memberLevel, richMessage, true));
    }

    private void handleFansLevelUpgradeBarrage(WebcastBarrageMessage barrageMessage) {
        if (!barrageMessage.hasFansLevelParam()) {
            logNotRendered(barrageMessage, "");
            return;
        }
        BarrageTypeFansLevelParam fansLevelParam = barrageMessage.getFansLevelParam();
        io.github.jwdeveloper.tiktok.messages.data.User rawUser = fansLevelParam.getUser();
        MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(
                rawUser.getId(),
                TikTokUserNames.sanitizeUserName(
                        TikTokUserNames.chooseRawUserName(rawUser.getNickname(), rawUser.getUsername())),
                TikTokMediaResolver.resolveUserAvatarUrl(rawUser),
                fansLevelParam.getCurrentGrade());
        if (!memberLevelEmitter.emit(update)) {
            logNotRendered(barrageMessage, "");
        }
    }

    private io.github.jwdeveloper.tiktok.messages.data.User resolveBarrageRawUser(
            TikTokRichMessageParser.ParsedText parsed, WebcastBarrageMessage barrageMessage
    ) {
        if (TikTokUserNames.isKnownRawUser(parsed.detectedUser())) {
            return parsed.detectedUser();
        }
        if (barrageMessage.hasUserGradeParam()
                && TikTokUserNames.isKnownRawUser(barrageMessage.getUserGradeParam().getUser())) {
            return barrageMessage.getUserGradeParam().getUser();
        }
        if (barrageMessage.hasFansLevelParam()
                && TikTokUserNames.isKnownRawUser(barrageMessage.getFansLevelParam().getUser())) {
            return barrageMessage.getFansLevelParam().getUser();
        }
        return null;
    }

    private String resolveBarrageUsername(
            TikTokRichMessageParser.ParsedText parsed,
            io.github.jwdeveloper.tiktok.messages.data.User rawUser
    ) {
        if (!MessageSanitizer.sanitize(parsed.detectedUsername()).isBlank()) {
            return TikTokUserNames.sanitizeUserName(parsed.detectedUsername());
        }
        if (rawUser != null) {
            return TikTokUserNames.sanitizeUserName(
                    TikTokUserNames.chooseRawUserName(rawUser.getNickname(), rawUser.getUsername()));
        }
        return Translations.tr(TikTokUserNames.USER_UNKNOWN_KEY);
    }

    private String resolveBarrageAvatarUrl(
            TikTokRichMessageParser.ParsedText parsed,
            io.github.jwdeveloper.tiktok.messages.data.User rawUser
    ) {
        if (!MessageSanitizer.sanitize(parsed.detectedAvatarUrl()).isBlank()) {
            return parsed.detectedAvatarUrl();
        }
        if (rawUser != null) {
            return TikTokMediaResolver.resolveUserAvatarUrl(rawUser);
        }
        return TikTokMediaResolver.defaultAvatarUrl();
    }

    int resolveBarrageMemberLevel(
            User user,
            io.github.jwdeveloper.tiktok.messages.data.User rawUser,
            WebcastBarrageMessage barrageMessage
    ) {
        int memberLevel = user == null ? 0 : memberLevelResolver.resolveLevel(user);
        if (memberLevel > 0) {
            return memberLevel;
        }
        int rawUserLevel = MemberLevelResolver.resolveRawUserLevel(rawUser);
        if (rawUserLevel > 0) {
            return rawUserLevel;
        }
        if (barrageMessage.hasFansLevelParam()) {
            return MemberLevelResolver.normalizeLevel(barrageMessage.getFansLevelParam().getCurrentGrade());
        }
        return 0;
    }

    private static long resolveMessageId(CommonMessageData common) {
        return common == null ? 0L : common.getMsgId();
    }

    private static void logNotRendered(WebcastBarrageMessage barrageMessage, String text) {
        String eventName = barrageMessage.hasEvent()
                ? MessageSanitizer.sanitize(barrageMessage.getEvent().getEventName()) : "";
        ReinodoceLogger.LOGGER.debug(
                "Recognized barrage payload not rendered msgType={} eventName={} text={}",
                barrageMessage.getMsgType(),
                eventName,
                MessageSanitizer.sanitize(text));
    }
}

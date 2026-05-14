package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jwdeveloper.tiktok.messages.data.User;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage.BarrageType;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage.BarrageTypeFansLevelParam;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage.BarrageTypeUserGradeParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarrageMessageHandlerTest {
    private static final long USER_ID = 42L;
    private static final String USERNAME = "alice";
    private static final int CURRENT_MEMBER_LEVEL = 3;
    private static final int UPGRADED_MEMBER_LEVEL = 8;

    @Test
    void fansLevelUpgradeUsesCurrentGradeLevelFromParsedPayload() throws InvalidProtocolBufferException {
        ReinodoceConfig config = enabledConfig();
        RecordingSink sink = new RecordingSink();
        BarrageMessageHandler handler = newHandler(config, sink);

        WebcastBarrageMessage message = WebcastBarrageMessage.parseFrom(
                sampleFansLevelUpgradePayload(UPGRADED_MEMBER_LEVEL, CURRENT_MEMBER_LEVEL).toByteArray());
        handler.dispatch(message);

        assertEquals(1, sink.memberLevelEvents());
        assertEquals(USERNAME, sink.username());
        assertEquals(UPGRADED_MEMBER_LEVEL, sink.memberLevel());
    }

    @Test
    void fansLevelEntranceDoesNotEmitMemberLevelOutput() {
        RecordingSink sink = new RecordingSink();
        BarrageMessageHandler handler = newHandler(enabledConfig(), sink);

        handler.dispatch(WebcastBarrageMessage.newBuilder()
                .setMsgType(BarrageType.FANSLEVELENTRANCE)
                .setFansLevelParam(fansLevelParam(CURRENT_MEMBER_LEVEL, UPGRADED_MEMBER_LEVEL))
                .build());

        assertEquals(0, sink.memberLevelEvents());
    }

    @Test
    void userGradeBarrageDoesNotEmitMemberLevelOutput() {
        RecordingSink sink = new RecordingSink();
        BarrageMessageHandler handler = newHandler(enabledConfig(), sink);

        handler.dispatch(WebcastBarrageMessage.newBuilder()
                .setMsgType(BarrageType.USERUPGRADE)
                .setUserGradeParam(BarrageTypeUserGradeParam.newBuilder()
                        .setCurrentGrade(UPGRADED_MEMBER_LEVEL)
                        .setUser(rawUser()))
                .build());

        assertEquals(0, sink.memberLevelEvents());
    }

    @Test
    void gradeUserEntranceNotificationDoesNotEmitMemberLevelOutput() {
        RecordingSink sink = new RecordingSink();
        BarrageMessageHandler handler = newHandler(enabledConfig(), sink);

        handler.dispatch(WebcastBarrageMessage.newBuilder()
                .setMsgType(BarrageType.GRADEUSERENTRANCENOTIFICATION)
                .setUserGradeParam(BarrageTypeUserGradeParam.newBuilder()
                        .setCurrentGrade(UPGRADED_MEMBER_LEVEL)
                        .setUser(rawUser()))
                .build());

        assertEquals(0, sink.memberLevelEvents());
    }

    @Test
    void syntheticMemberLevelSettingStillGatesFansLevelUpgradeOutput() {
        RecordingSink sink = new RecordingSink();
        BarrageMessageHandler handler = newHandler(ReinodoceConfig.defaults(), sink);

        handler.dispatch(sampleFansLevelUpgradePayload(CURRENT_MEMBER_LEVEL, UPGRADED_MEMBER_LEVEL));

        assertEquals(0, sink.memberLevelEvents());
    }

    private static ReinodoceConfig enabledConfig() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSyntheticMemberLevelEnabled(true);
        config.setChatEmotesEnabled(false);
        return config;
    }

    private static BarrageMessageHandler newHandler(ReinodoceConfig config, RecordingSink sink) {
        return new BarrageMessageHandler(
                new TikTokRichMessageParser(),
                new MemberLevelResolver(),
                MemberLevelEmitterTestFactory.create(config, sink),
                null);
    }

    private static WebcastBarrageMessage sampleFansLevelUpgradePayload(int currentLevel, int upgradedLevel) {
        return WebcastBarrageMessage.newBuilder()
                .setMsgType(BarrageType.FANSLEVELUPGRADE)
                .setFansLevelParam(fansLevelParam(currentLevel, upgradedLevel))
                .build();
    }

    private static BarrageTypeFansLevelParam fansLevelParam(int currentLevel, int upgradedLevel) {
        return BarrageTypeFansLevelParam.newBuilder()
                .setCurrentGrade(currentLevel)
                .setDisplayConfig(upgradedLevel)
                .setUser(rawUser())
                .build();
    }

    private static User rawUser() {
        return User.newBuilder()
                .setId(USER_ID)
                .setNickname(USERNAME)
                .setUsername(USERNAME)
                .build();
    }

    private static final class RecordingSink implements ChatEventSink {
        private String recordedUsername = "";
        private int recordedMemberLevel;
        private int recordedMemberLevelEvents;

        @Override
        public void sendLiveComment(ReinodoceConfig config, String username, String message) {
        }

        @Override
        public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendStarComment(ReinodoceConfig config, String username, String message) {
        }

        @Override
        public void sendStarComment(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count) {
        }

        @Override
        public void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticFollow(ReinodoceConfig config, String username) {
        }

        @Override
        public void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticJoin(ReinodoceConfig config, String username) {
        }

        @Override
        public void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel) {
            recordMemberLevel(username, memberLevel);
        }

        @Override
        public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
            recordMemberLevel(message.username(), memberLevel);
        }

        @Override
        public void sendSystem(String message, boolean success) {
        }

        private void recordMemberLevel(String username, int memberLevel) {
            recordedUsername = username;
            recordedMemberLevel = memberLevel;
            recordedMemberLevelEvents++;
        }

        String username() {
            return recordedUsername;
        }

        int memberLevel() {
            return recordedMemberLevel;
        }

        int memberLevelEvents() {
            return recordedMemberLevelEvents;
        }
    }
}

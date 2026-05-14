package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberLevelEmitterTest {
    private static final String USERNAME = "alice";
    private static final String AVATAR_URL = "avatar://alice";

    @Test
    void emitsFirstObservedExplicitLevelIncrease() {
        ReinodoceConfig config = enabledConfig();
        RecordingSink sink = new RecordingSink();
        MemberLevelEmitter emitter = newEmitter(config, sink);

        boolean emitted = emitter.emit(new MemberLevelResolver.LevelUpdate(
                1L, USERNAME, AVATAR_URL, 0, 2));

        assertTrue(emitted);
        assertEquals(USERNAME, sink.username());
        assertEquals(2, sink.memberLevel());
    }

    @Test
    void skipsRepeatedLevelsAndDisabledConfig() {
        ReinodoceConfig disabled = ReinodoceConfig.defaults();
        RecordingSink sink = new RecordingSink();

        assertFalse(newEmitter(enabledConfig(), sink).emit(new MemberLevelResolver.LevelUpdate(
                1L, USERNAME, AVATAR_URL, 2, 2)));
        assertFalse(newEmitter(disabled, sink).emit(new MemberLevelResolver.LevelUpdate(
                1L, USERNAME, AVATAR_URL, 1, 2)));
    }

    private static ReinodoceConfig enabledConfig() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSyntheticMemberLevelEnabled(true);
        config.setChatEmotesEnabled(false);
        return config;
    }

    private static MemberLevelEmitter newEmitter(ReinodoceConfig config, RecordingSink sink) {
        return new MemberLevelEmitter(
                () -> config,
                new TikTokRuntimeServices(
                        sink,
                        new SessionEventLogger(Path.of("build/test-session-logs")),
                        new AlertService(AlertSink.noop())),
                new RichLiveMessageFactory(new UnicodeEmojiParser()),
                new SessionStatsTracker());
    }

    private static final class RecordingSink implements ChatEventSink {
        private String recordedUsername = "";
        private int recordedMemberLevel;

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
            this.recordedUsername = username;
            this.recordedMemberLevel = memberLevel;
        }

        @Override
        public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
            this.recordedUsername = message.username();
            this.recordedMemberLevel = memberLevel;
        }

        @Override
        public void sendSystem(String message, boolean success) {
        }

        String username() {
            return recordedUsername;
        }

        int memberLevel() {
            return recordedMemberLevel;
        }
    }
}

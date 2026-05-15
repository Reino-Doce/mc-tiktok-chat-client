package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveCommentEmitterTest {
    private static final long USER_ID = 42L;
    private static final String USERNAME = "alice";

    @Test
    void burstHiddenCommentStillBlocksDuplicateCallbackStats() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setBurstControlEnabled(true);
        config.setBurstCommentsPerSecond(1);
        config.setChatEmotesEnabled(false);
        RecordingSink sink = new RecordingSink();
        SessionStatsTracker statsTracker = new SessionStatsTracker();
        LiveCommentEmitter emitter = newEmitter(config, sink, statsTracker);

        emitter.emit(context(1L, "visible"));
        emitter.emit(context(2L, "hidden"));
        emitter.emit(context(2L, "hidden"));

        assertEquals(1, sink.liveComments());
        assertEquals(2L, statsTracker.snapshot().messages());
    }

    private static LiveCommentEmitter newEmitter(
            ReinodoceConfig config,
            ChatEventSink sink,
            SessionStatsTracker statsTracker
    ) {
        AtomicLong clockMillis = new AtomicLong();
        BurstOutputController burstOutputController = new BurstOutputController(
                (task, delayMillis) -> {
                },
                clockMillis::get);
        return new LiveCommentEmitter(new LiveCommentEmitter.Dependencies(
                () -> config,
                sink,
                new MessageRuleEngine(),
                new MessageDeduplicator(Duration.ofMinutes(1)),
                new RenderedCommentTracker(),
                new RichLiveMessageFactory(new UnicodeEmojiParser()),
                statsTracker,
                new ModerationDuplicateTracker(),
                new UserCooldownTracker(),
                burstOutputController,
                new SessionEventLogger(Path.of("build/test-session-logs/live-comment-emitter"))));
    }

    private static LiveCommentEmitter.EmissionContext context(long messageId, String text) {
        return new LiveCommentEmitter.EmissionContext(
                new User(USER_ID, USERNAME),
                USERNAME,
                "",
                0,
                new RichLiveMessage(messageId, USERNAME, List.of(new RichLiveMessage.TextSegment(text))),
                false);
    }

    private static final class RecordingSink implements ChatEventSink {
        private int liveCommentCount;

        @Override
        public void sendLiveComment(ReinodoceConfig config, String username, String message) {
            liveCommentCount++;
        }

        @Override
        public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
            liveCommentCount++;
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
        }

        @Override
        public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
        }

        @Override
        public void sendSystem(String message, boolean success) {
        }

        int liveComments() {
            return liveCommentCount;
        }
    }
}

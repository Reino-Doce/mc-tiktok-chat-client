package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.pinned.PinnedLiveMessage;
import br.com.reinodoce.mctiktok.pinned.PinnedMessageSink;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TikTokFacadeWiringTest {
    @Test
    void transientResetClearsPinnedOverlayState() {
        RecordingPinnedSink pinnedSink = new RecordingPinnedSink();
        TikTokRuntimeServices runtimeServices = new TikTokRuntimeServices(
                ChatEventSink.noop(),
                new SessionEventLogger(Path.of("build/test-session-logs/wiring")),
                new AlertService(AlertSink.noop()),
                pinnedSink,
                () -> "en_us",
                () -> true);
        TikTokFacadeWiring wiring = TikTokFacadeWiring.assemble(
                ReinodoceConfig::defaults,
                runtimeServices,
                new MessageRuleEngine(),
                new MemberLevelResolver(),
                new MessageDeduplicator(Duration.ofMinutes(1)));

        wiring.resetTransientState();

        assertEquals(1, pinnedSink.clears);
    }

    private static final class RecordingPinnedSink implements PinnedMessageSink {
        private int clears;

        @Override
        public void showPinnedMessage(ReinodoceConfig config, PinnedLiveMessage message) {
        }

        @Override
        public void clearPinnedMessages() {
            clears++;
        }
    }
}

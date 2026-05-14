package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;

import java.nio.file.Path;

final class MemberLevelEmitterTestFactory {
    private MemberLevelEmitterTestFactory() {
    }

    static MemberLevelEmitter create(ReinodoceConfig config, ChatEventSink sink) {
        return new MemberLevelEmitter(
                () -> config,
                new TikTokRuntimeServices(
                        sink,
                        new SessionEventLogger(Path.of("build/test-session-logs/member-level-emitter")),
                        new AlertService(AlertSink.noop())),
                new RichLiveMessageFactory(new UnicodeEmojiParser()),
                new SessionStatsTracker());
    }
}

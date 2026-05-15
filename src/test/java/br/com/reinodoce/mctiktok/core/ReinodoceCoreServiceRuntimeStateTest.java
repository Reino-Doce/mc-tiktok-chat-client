package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.tiktok.MemberLevelResolver;
import br.com.reinodoce.mctiktok.tiktok.TikTokClientFacade;
import br.com.reinodoce.mctiktok.tiktok.TikTokRuntimeServices;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceCoreServiceRuntimeStateTest {
    private static final String EN_US = "en_us";

    @TempDir
    Path tempDir;

    @Test
    void outputModeChangeClearsRuntimeConfigState() {
        RecordingTikTokClientFacade facade = new RecordingTikTokClientFacade();
        ReinodoceCoreService service = newService("output-mode-runtime-state.json", facade);
        service.currentConfig();
        facade.reset();

        CommandResult result = service.setOutputMode("off");

        assertTrue(result.success());
        assertEquals(1, facade.configUpdates());
        assertEquals(0, facade.reconnects());
    }

    @Test
    void disablingSyntheticFollowAndJoinClearsRuntimeConfigState() {
        RecordingTikTokClientFacade facade = new RecordingTikTokClientFacade();
        ReinodoceCoreService service = newService("synthetic-runtime-state.json", facade);
        service.setSyntheticFollow(true);
        service.setSyntheticJoin(true);
        facade.reset();

        CommandResult followResult = service.setSyntheticFollow(false);
        CommandResult joinResult = service.setSyntheticJoin(false);

        assertTrue(followResult.success());
        assertTrue(joinResult.success());
        assertEquals(2, facade.configUpdates());
        assertEquals(0, facade.reconnects());
    }

    private ReinodoceCoreService newService(String fileName, TikTokClientFacade facade) {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve(fileName));
        return new ReinodoceCoreService(repository, () -> EN_US, facade);
    }

    private static final class RecordingTikTokClientFacade extends TikTokClientFacade {
        private int configUpdateCount;
        private int reconnectCount;

        RecordingTikTokClientFacade() {
            super(
                    ReinodoceConfig::defaults,
                    new TikTokRuntimeServices(
                            ChatEventSink.noop(),
                            new SessionEventLogger(Path.of("build/test-session-logs/core-runtime-state")),
                            new AlertService(AlertSink.noop())),
                    new MessageRuleEngine(),
                    new MemberLevelResolver(),
                    new MessageDeduplicator(Duration.ofSeconds(1)));
        }

        @Override
        public void onConfigUpdated() {
            configUpdateCount++;
        }

        @Override
        public void reconnectForConfigChange() {
            reconnectCount++;
        }

        void reset() {
            configUpdateCount = 0;
            reconnectCount = 0;
        }

        int configUpdates() {
            return configUpdateCount;
        }

        int reconnects() {
            return reconnectCount;
        }
    }
}

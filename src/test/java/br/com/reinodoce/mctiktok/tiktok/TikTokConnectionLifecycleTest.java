package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.state.ConnectionLifecycleState;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.NoticeThrottler;
import io.github.jwdeveloper.tiktok.live.builder.LiveClientBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TikTokConnectionLifecycleTest {
    private static final String USERNAME = "alice";

    @Test
    void configReconnectRestartsConnectedSessionWithoutRunningNetworkTask() {
        RecordingExecutorService ioExecutor = new RecordingExecutorService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        LiveSessionState sessionState = session(ConnectionLifecycleState.CONNECTED);
        TikTokConnectionLifecycle lifecycle = lifecycle(sessionState, ioExecutor, scheduler);

        try {
            lifecycle.reconnectForConfigChange();

            assertEquals(ConnectionLifecycleState.CONNECTING, sessionState.snapshot().state());
            assertEquals(USERNAME, sessionState.snapshot().username());
            assertEquals(1, ioExecutor.submittedTasks());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void configReconnectRestartsConnectingSessionWithoutRunningNetworkTask() {
        RecordingExecutorService ioExecutor = new RecordingExecutorService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        LiveSessionState sessionState = session(ConnectionLifecycleState.CONNECTING);
        TikTokConnectionLifecycle lifecycle = lifecycle(sessionState, ioExecutor, scheduler);

        try {
            lifecycle.reconnectForConfigChange();

            assertEquals(ConnectionLifecycleState.CONNECTING, sessionState.snapshot().state());
            assertEquals(USERNAME, sessionState.snapshot().username());
            assertEquals(1, ioExecutor.submittedTasks());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void configReconnectDoesNothingWhenDisconnected() {
        RecordingExecutorService ioExecutor = new RecordingExecutorService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        LiveSessionState sessionState = session(ConnectionLifecycleState.DISCONNECTED);
        TikTokConnectionLifecycle lifecycle = lifecycle(sessionState, ioExecutor, scheduler);

        try {
            lifecycle.reconnectForConfigChange();

            assertEquals(ConnectionLifecycleState.DISCONNECTED, sessionState.snapshot().state());
            assertEquals(0, ioExecutor.submittedTasks());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void configReconnectDoesNothingWithoutActiveSession() {
        RecordingExecutorService ioExecutor = new RecordingExecutorService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        LiveSessionState sessionState = session(ConnectionLifecycleState.RECONNECT_SCHEDULED);
        TikTokConnectionLifecycle lifecycle = lifecycle(sessionState, ioExecutor, scheduler);

        try {
            lifecycle.reconnectForConfigChange();

            assertEquals(ConnectionLifecycleState.RECONNECT_SCHEDULED, sessionState.snapshot().state());
            assertEquals(0, ioExecutor.submittedTasks());
        } finally {
            scheduler.shutdownNow();
        }
    }

    private static LiveSessionState session(ConnectionLifecycleState state) {
        LiveSessionState sessionState = new LiveSessionState();
        sessionState.setUsername(USERNAME);
        sessionState.setState(state);
        return sessionState;
    }

    private static TikTokConnectionLifecycle lifecycle(
            LiveSessionState sessionState,
            ExecutorService ioExecutor,
            ScheduledExecutorService scheduler
    ) {
        TikTokConnectionLifecycle.LifecycleParams params = new TikTokConnectionLifecycle.LifecycleParams(
                ReinodoceConfig::defaults,
                ChatEventSink.noop(),
                sessionState,
                ioExecutor,
                scheduler,
                new NoticeThrottler(Duration.ofSeconds(1)),
                new NoticeThrottler(Duration.ofSeconds(1)),
                () -> {
                },
                () -> {
                },
                () -> "en_us",
                new SessionEventLogger(Path.of("build/test-session-logs/lifecycle")));
        BiConsumer<LiveClientBuilder, Long> callbackBinder = (builder, token) -> {
        };
        return new TikTokConnectionLifecycle(params, callbackBinder);
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {
        private int taskCount;

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            taskCount++;
        }

        int submittedTasks() {
            return taskCount;
        }
    }
}

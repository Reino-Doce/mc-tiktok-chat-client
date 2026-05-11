package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.state.ConnectionLifecycleState;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.NoticeThrottler;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

final class TikTokReconnectScheduler {
    private final Supplier<ReinodoceConfig> configSupplier;
    private final ChatEventSink chatGateway;
    private final LiveSessionState sessionState;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduler;
    private final NoticeThrottler reconnectNoticeThrottler;
    private final BiConsumer<Long, String> reconnectAction;

    private volatile ScheduledFuture<?> reconnectTask;

    TikTokReconnectScheduler(Dependencies dependencies, BiConsumer<Long, String> reconnectAction) {
        this.configSupplier = dependencies.configSupplier();
        this.chatGateway = dependencies.chatGateway();
        this.sessionState = dependencies.sessionState();
        this.ioExecutor = dependencies.ioExecutor();
        this.scheduler = dependencies.scheduler();
        this.reconnectNoticeThrottler = dependencies.reconnectNoticeThrottler();
        this.reconnectAction = reconnectAction;
    }

    record Dependencies(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            LiveSessionState sessionState,
            ExecutorService ioExecutor,
            ScheduledExecutorService scheduler,
            NoticeThrottler reconnectNoticeThrottler
    ) {
    }

    boolean isScheduled() {
        ScheduledFuture<?> task = reconnectTask;
        return task != null && !task.isDone();
    }

    void cancel() {
        ScheduledFuture<?> task = reconnectTask;
        reconnectTask = null;
        if (task != null) {
            task.cancel(false);
        }
    }

    void scheduleReconnect(long token, String username, String reason, java.util.function.LongPredicate tokenCheck) {
        ReinodoceConfig config = configSupplier.get();
        int reconnectSeconds = config.getReconnectSeconds();
        if (reconnectSeconds <= 0 || !tokenCheck.test(token)) {
            return;
        }
        if (isScheduled()) {
            ReinodoceLogger.LOGGER.debug("Reconnect already scheduled for @{}, skipping duplicate schedule.", username);
            return;
        }
        int attempt = sessionState.incrementReconnectAttempts();
        sessionState.setState(ConnectionLifecycleState.RECONNECT_SCHEDULED);
        sessionState.setReconnectAt(Instant.now().plusSeconds(reconnectSeconds));
        announce(username, reason, reconnectSeconds, attempt);
        reconnectTask = scheduler.schedule(
                () -> execute(token, username, attempt, tokenCheck),
                reconnectSeconds,
                TimeUnit.SECONDS);
    }

    void onConfigUpdated() {
        if (configSupplier.get().getReconnectSeconds() <= 0) {
            cancel();
            sessionState.setReconnectAt(null);
            reconnectNoticeThrottler.reset();
            if (sessionState.snapshot().state() == ConnectionLifecycleState.RECONNECT_SCHEDULED) {
                sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
            }
        }
    }

    private void announce(String username, String reason, int reconnectSeconds, int attempt) {
        String notice = Translations.tr(
                "reinodoce.chat.reconnect_notice", attempt, reconnectSeconds, reason);
        if (reconnectNoticeThrottler.shouldEmit(username + ":" + reason)) {
            chatGateway.sendSystem(notice, false);
        }
        ReinodoceLogger.LOGGER.warn(
                "Scheduling reconnect attempt {} to @{} in {}s (reason={})",
                attempt, username, reconnectSeconds, reason);
    }

    private void execute(long token, String username, int attempt, java.util.function.LongPredicate tokenCheck) {
        if (!tokenCheck.test(token)) {
            return;
        }
        sessionState.setState(ConnectionLifecycleState.CONNECTING);
        sessionState.setReconnectAt(null);
        ReinodoceLogger.LOGGER.info("Executing reconnect attempt {} to @{}", attempt, username);
        ioExecutor.submit(() -> reconnectAction.accept(token, username));
    }
}

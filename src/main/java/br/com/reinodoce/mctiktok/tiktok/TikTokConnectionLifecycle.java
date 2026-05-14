package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.state.ConnectionLifecycleState;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.NoticeThrottler;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import br.com.reinodoce.mctiktok.util.UsernameValidator;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.data.events.TikTokDisconnectedEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokErrorEvent;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveOfflineHostException;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveUnknownHostException;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import io.github.jwdeveloper.tiktok.live.builder.LiveClientBuilder;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;

final class TikTokConnectionLifecycle {
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15L);

    private final LifecycleParams params;
    private final AtomicLong lifecycleToken = new AtomicLong();
    private final TikTokReconnectScheduler reconnectScheduler;
    private final BiConsumer<LiveClientBuilder, Long> callbackBinder;

    private volatile LiveClient liveClient;

    TikTokConnectionLifecycle(
            LifecycleParams params,
            BiConsumer<LiveClientBuilder, Long> callbackBinder
    ) {
        this.params = params;
        this.reconnectScheduler = new TikTokReconnectScheduler(
                new TikTokReconnectScheduler.Dependencies(
                        params.configSupplier(), params.chatGateway(), params.sessionState(),
                        params.ioExecutor(), params.scheduler(), params.reconnectNoticeThrottler()),
                this::executeReconnect);
        this.callbackBinder = callbackBinder;
    }

    CommandResult connect(String usernameInput) {
        String username = UsernameValidator.normalize(usernameInput);
        if (!UsernameValidator.isValid(username)) {
            return CommandResult.error(Translations.tr("reinodoce.error.username_invalid"));
        }
        LiveSessionState.Snapshot snapshot = params.sessionState().snapshot();
        if (isConnectionActive(snapshot.state()) && username.equalsIgnoreCase(snapshot.username())) {
            return CommandResult.ok(Translations.tr("reinodoce.command.connect.already_connected", username));
        }
        long token = beginNewLifecycle(username);
        params.ioExecutor().submit(() -> performConnect(token, username));
        return connectResult(snapshot, username);
    }

    CommandResult disconnect() {
        LiveSessionState.Snapshot snapshot = params.sessionState().snapshot();
        if (snapshot.state() == ConnectionLifecycleState.DISCONNECTED
                && liveClient == null
                && !reconnectScheduler.isScheduled()) {
            return CommandResult.ok(Translations.tr("reinodoce.command.disconnect.already"));
        }
        long token = lifecycleToken.incrementAndGet();
        resetSessionForDisconnect();
        params.ioExecutor().submit(this::resetClient);
        ReinodoceLogger.LOGGER.debug("Disconnected requested with lifecycle token {}", token);
        return CommandResult.ok(Translations.tr("reinodoce.command.disconnect.in_progress"));
    }

    void onConfigUpdated() {
        reconnectScheduler.onConfigUpdated();
    }

    boolean isTokenCurrent(long token) {
        return lifecycleToken.get() == token;
    }

    static boolean shouldRetryConnectFailure(Throwable throwable) {
        return !(throwable instanceof TikTokLiveUnknownHostException);
    }

    private void resetSessionForDisconnect() {
        LiveSessionState sessionState = params.sessionState();
        sessionState.setReconnectAt(null);
        sessionState.setUsername("");
        sessionState.setReconnectAttempts(0);
        sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
        sessionState.setLastError("");
        params.errorNoticeThrottler().reset();
        params.reconnectNoticeThrottler().reset();
    }

    private long beginNewLifecycle(String username) {
        long token = lifecycleToken.incrementAndGet();
        LiveSessionState sessionState = params.sessionState();
        sessionState.setUsername(username);
        sessionState.setLastError("");
        sessionState.setReconnectAt(null);
        sessionState.setReconnectAttempts(0);
        sessionState.setState(ConnectionLifecycleState.CONNECTING);
        params.errorNoticeThrottler().reset();
        params.reconnectNoticeThrottler().reset();
        return token;
    }

    private CommandResult connectResult(LiveSessionState.Snapshot snapshot, String username) {
        boolean switchingUser = isConnectionActive(snapshot.state())
                && !snapshot.username().isBlank()
                && !username.equalsIgnoreCase(snapshot.username());
        return CommandResult.ok(switchingUser
                ? Translations.tr("reinodoce.command.connect.switching", username)
                : Translations.tr("reinodoce.command.connect.connecting", username));
    }

    private void performConnect(long token, String username) {
        reconnectScheduler.cancel();
        disconnectCurrentClient();
        params.onReset().run();
        connectInternal(token, username);
    }

    private void resetClient() {
        reconnectScheduler.cancel();
        disconnectCurrentClient();
        params.onReset().run();
    }

    private void executeReconnect(long token, String username) {
        params.onReset().run();
        connectInternal(token, username);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void connectInternal(long token, String username) {
        if (!isTokenCurrent(token)) {
            return;
        }
        try {
            LiveClient client = buildClient(token, username);
            if (!isTokenCurrent(token)) {
                client.disconnect();
                return;
            }
            liveClient = client;
            client.connect();
        } catch (RuntimeException exception) {
            handleConnectException(token, username, exception);
        }
    }

    private LiveClient buildClient(long token, String username) {
        LiveClientBuilder builder = TikTokLive.newClient(username)
                .configure(settings -> {
                    settings.setRetryOnConnectionFailure(false);
                    settings.setPrintToConsole(false);
                    settings.setLogLevel(Level.SEVERE);
                    settings.setClientLanguage(TikTokLanguageResolver.resolve(params.clientLanguageSupplier().get()));
                    settings.getHttpSettings().setTimeout(HTTP_TIMEOUT);
                })
                .onConnected((liveClient, event) -> handleConnected(token, username, liveClient))
                .onDisconnected((liveClient, event) -> handleDisconnected(token, username, event))
                .onError((liveClient, event) -> handleError(token, event));
        callbackBinder.accept(builder, token);
        return builder.build();
    }

    private void handleConnected(long token, String username, LiveClient client) {
        if (!isTokenCurrent(token)) {
            client.disconnect();
            return;
        }
        reconnectScheduler.cancel();
        liveClient = client;
        params.onConnected().run();
        LiveSessionState sessionState = params.sessionState();
        sessionState.setState(ConnectionLifecycleState.CONNECTED);
        sessionState.setUsername(username);
        sessionState.setLastError("");
        sessionState.setReconnectAt(null);
        sessionState.setReconnectAttempts(0);
        params.reconnectNoticeThrottler().reset();
        params.chatGateway().sendSystem(Translations.tr("reinodoce.chat.connected", username), true);
        ReinodoceLogger.LOGGER.info("Connected to TikTok LIVE @{}", username);
    }

    private void handleDisconnected(long token, String username, TikTokDisconnectedEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }
        liveClient = null;
        String reason = TikTokUserNames.normalizeErrorReason(
                event == null ? null : event.getReason(),
                Translations.tr("reinodoce.error.reason.disconnect_unknown"));
        if (reconnectScheduler.isScheduled()) {
            params.sessionState().setLastError(reason);
            ReinodoceLogger.LOGGER.warn(
                    "Disconnected from TikTok LIVE @{} while reconnect is already scheduled (reason={})",
                    username, reason);
            return;
        }
        params.sessionState().setState(ConnectionLifecycleState.DISCONNECTED);
        params.sessionState().setLastError(reason);
        sendErrorSystemNotice("disconnect:" + reason, Translations.tr("reinodoce.chat.disconnected_reason", reason));
        ReinodoceLogger.LOGGER.warn("Disconnected from TikTok LIVE @{} (reason={})", username, reason);
        reconnectScheduler.scheduleReconnect(
                token, username, Translations.tr("reinodoce.reason.disconnect"), this::isTokenCurrent);
    }

    private void handleError(long token, TikTokErrorEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }
        Throwable exception = event.getException();
        if (exception == null) {
            return;
        }
        String error = TikTokUserNames.normalizeErrorReason(
                exception.getMessage(), exception.getClass().getSimpleName());
        String message = Translations.tr("reinodoce.chat.error_live", error);
        String username = params.sessionState().snapshot().username();
        params.sessionState().setLastError(message);
        params.sessionState().setState(ConnectionLifecycleState.ERROR);
        sendErrorSystemNotice(
                "runtime:" + exception.getClass().getName() + ":" + error, message);
        ReinodoceLogger.LOGGER.warn(
                "TikTok runtime connection error for @{} (retryable=true): {}", username, error, exception);
        disconnectCurrentClient();
        reconnectScheduler.scheduleReconnect(
                token, username, Translations.tr("reinodoce.reason.connection_error"), this::isTokenCurrent);
    }

    private void handleConnectException(long token, String username, Throwable exception) {
        if (!isTokenCurrent(token)) {
            return;
        }
        liveClient = null;
        boolean allowReconnect = shouldRetryConnectFailure(exception);
        String message = describeConnectFailure(username, exception, allowReconnect);
        params.sessionState().setLastError(message);
        params.sessionState().setState(ConnectionLifecycleState.ERROR);
        sendErrorSystemNotice(
                "connect:" + username + ":" + exception.getClass().getName(), message);
        ReinodoceLogger.LOGGER.warn(
                "Connect failure for @{} (reconnect={}) -> {}",
                username, allowReconnect, message, exception);
        if (allowReconnect) {
            reconnectScheduler.scheduleReconnect(
                    token, username, Translations.tr("reinodoce.reason.connection_failure"), this::isTokenCurrent);
        } else {
            params.sessionState().setReconnectAttempts(0);
        }
    }

    private static String describeConnectFailure(String username, Throwable exception, boolean allowReconnect) {
        if (!allowReconnect) {
            return Translations.tr("reinodoce.error.username_not_found", username);
        }
        if (exception instanceof TikTokLiveOfflineHostException) {
            return Translations.tr("reinodoce.error.user_offline", username);
        }
        String reason = TikTokUserNames.normalizeErrorReason(
                exception.getMessage(), Translations.tr("reinodoce.error.reason.network_unavailable"));
        return Translations.tr("reinodoce.error.connect_failed", reason);
    }

    private void disconnectCurrentClient() {
        LiveClient current = liveClient;
        liveClient = null;
        if (current != null) {
            tryDisconnect(current);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static void tryDisconnect(LiveClient client) {
        try {
            client.disconnect();
        } catch (RuntimeException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to close previous TikTok client cleanly", exception);
        }
    }

    private void sendErrorSystemNotice(String key, String message) {
        if (params.errorNoticeThrottler().shouldEmit(key)) {
            params.chatGateway().sendSystem(message, false);
        }
    }

    private static boolean isConnectionActive(ConnectionLifecycleState state) {
        return state == ConnectionLifecycleState.CONNECTED
                || state == ConnectionLifecycleState.CONNECTING
                || state == ConnectionLifecycleState.RECONNECT_SCHEDULED;
    }

    record LifecycleParams(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            LiveSessionState sessionState,
            ExecutorService ioExecutor,
            ScheduledExecutorService scheduler,
            NoticeThrottler errorNoticeThrottler,
            NoticeThrottler reconnectNoticeThrottler,
            Runnable onReset,
            Runnable onConnected,
            Supplier<String> clientLanguageSupplier
    ) {
    }
}

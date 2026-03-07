package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.state.ConnectionLifecycleState;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.ExecutorsFactory;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import br.com.reinodoce.mctiktok.util.UsernameValidator;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokDisconnectedEvent;
import io.github.jwdeveloper.tiktok.data.events.TikTokErrorEvent;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftComboEvent;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokFollowEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokJoinEvent;
import io.github.jwdeveloper.tiktok.data.events.websocket.TikTokWebsocketMessageEvent;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.gifts.GiftComboStateType;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveOfflineHostException;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveUnknownHostException;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;

public class TikTokClientFacade {
    private final Supplier<ReinodoceConfig> configSupplier;
    private final MinecraftChatGateway chatGateway;
    private final MessageRuleEngine ruleEngine;
    private final MemberLevelResolver memberLevelResolver;
    private final MessageDeduplicator giftDeduplicator;
    private final LiveSessionState sessionState;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduler;
    private final GiftComboAggregator giftComboAggregator;
    private final AtomicLong lifecycleToken;

    private volatile LiveClient liveClient;
    private volatile ScheduledFuture<?> reconnectTask;

    public TikTokClientFacade(
            Supplier<ReinodoceConfig> configSupplier,
            MinecraftChatGateway chatGateway,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            MessageDeduplicator giftDeduplicator
    ) {
        this.configSupplier = configSupplier;
        this.chatGateway = chatGateway;
        this.ruleEngine = ruleEngine;
        this.memberLevelResolver = memberLevelResolver;
        this.giftDeduplicator = giftDeduplicator;
        this.sessionState = new LiveSessionState();
        this.ioExecutor = ExecutorsFactory.newSingleThreadExecutor("reinodoce-tiktok-io");
        this.scheduler = ExecutorsFactory.newSingleThreadScheduledExecutor("reinodoce-tiktok-scheduler");
        this.giftComboAggregator = new GiftComboAggregator(scheduler, this::emitGiftFromAsyncFlush);
        this.lifecycleToken = new AtomicLong(0L);
    }

    public CommandResult connect(String usernameInput) {
        String username = UsernameValidator.normalize(usernameInput);
        if (!UsernameValidator.isValid(username)) {
            return CommandResult.error("Username invalido. Use @username com letras, numeros, ponto ou underscore.");
        }

        LiveSessionState.Snapshot snapshot = sessionState.snapshot();
        if (isConnectionActive(snapshot.state()) && username.equalsIgnoreCase(snapshot.username())) {
            return CommandResult.ok("Ja conectado em @" + username + ".");
        }

        long token = lifecycleToken.incrementAndGet();
        sessionState.setUsername(username);
        sessionState.setLastError("");
        sessionState.setReconnectAt(null);
        sessionState.setState(ConnectionLifecycleState.CONNECTING);

        ioExecutor.submit(() -> {
            cancelReconnectTask();
            disconnectCurrentClient();
            giftDeduplicator.clear();
            giftComboAggregator.clear();
            memberLevelResolver.clear();
            connectInternal(token, username);
        });

        boolean switchingUser = isConnectionActive(snapshot.state())
                && !snapshot.username().isBlank()
                && !username.equalsIgnoreCase(snapshot.username());
        if (switchingUser) {
            return CommandResult.ok("Trocando conexao para @" + username + "...");
        }
        return CommandResult.ok("Conectando em @" + username + "...");
    }

    public CommandResult disconnect() {
        LiveSessionState.Snapshot snapshot = sessionState.snapshot();
        ScheduledFuture<?> task = reconnectTask;
        if (snapshot.state() == ConnectionLifecycleState.DISCONNECTED
                && liveClient == null
                && (task == null || task.isDone())) {
            return CommandResult.ok("Ja esta desconectado.");
        }

        long token = lifecycleToken.incrementAndGet();
        sessionState.setReconnectAt(null);
        sessionState.setUsername("");
        sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
        sessionState.setLastError("");
        ioExecutor.submit(() -> {
            cancelReconnectTask();
            disconnectCurrentClient();
            giftComboAggregator.clear();
            giftDeduplicator.clear();
            memberLevelResolver.clear();
        });
        ReinodoceLogger.LOGGER.debug("Disconnected requested with lifecycle token {}", token);
        return CommandResult.ok("Desconectando...");
    }

    public LiveSessionState.Snapshot status() {
        return sessionState.snapshot();
    }

    public void onConfigUpdated() {
        ReinodoceConfig config = configSupplier.get();
        if (config.getReconnectSeconds() <= 0) {
            cancelReconnectTask();
            sessionState.setReconnectAt(null);
            if (sessionState.snapshot().state() == ConnectionLifecycleState.RECONNECT_SCHEDULED) {
                sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
            }
        }
    }

    private void connectInternal(long token, String username) {
        if (!isTokenCurrent(token)) {
            return;
        }

        try {
            LiveClient client = TikTokLive.newClient(username)
                    .configure(settings -> {
                        settings.setRetryOnConnectionFailure(false);
                        settings.setPrintToConsole(false);
                        settings.setLogLevel(Level.SEVERE);
                        settings.setClientLanguage("pt-BR");
                    })
                    .onConnected((liveClient, event) -> handleConnected(token, username, liveClient))
                    .onDisconnected((liveClient, event) -> handleDisconnected(token, username, event))
                    .onError((liveClient, event) -> handleError(token, event))
                    .onComment((liveClient, event) -> handleComment(token, event))
                    .onFollow((liveClient, event) -> handleFollow(token, event))
                    .onJoin((liveClient, event) -> handleJoin(token, event))
                    .onGift((liveClient, event) -> handleGift(token, event))
                    .onGiftCombo((liveClient, event) -> handleGiftCombo(token, event))
                    .onWebsocketMessage((liveClient, event) -> handleWebsocketMessage(token, event))
                    .build();

            if (!isTokenCurrent(token)) {
                client.disconnect();
                return;
            }

            liveClient = client;
            client.connect();
        } catch (Exception exception) {
            handleConnectException(token, username, exception);
        }
    }

    private void handleConnected(long token, String username, LiveClient client) {
        if (!isTokenCurrent(token)) {
            client.disconnect();
            return;
        }
        liveClient = client;
        sessionState.setState(ConnectionLifecycleState.CONNECTED);
        sessionState.setUsername(username);
        sessionState.setLastError("");
        sessionState.setReconnectAt(null);
        chatGateway.sendSystem("Conectado em @" + username, true);
    }

    private void handleDisconnected(long token, String username, TikTokDisconnectedEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }
        liveClient = null;
        sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
        String reason = MessageSanitizer.sanitize(event.getReason());
        if (!reason.isBlank() && !"None".equalsIgnoreCase(reason)) {
            sessionState.setLastError(reason);
            chatGateway.sendSystem("Desconectado: " + reason, false);
        } else {
            chatGateway.sendSystem("Desconectado da LIVE.", false);
        }
        scheduleReconnect(token, username, "desconexao");
    }

    private void handleError(long token, TikTokErrorEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }

        Throwable exception = event.getException();
        if (exception == null) {
            return;
        }
        String error = MessageSanitizer.sanitize(exception.getMessage());
        if (!error.isBlank()) {
            sessionState.setLastError(error);
        }
        ReinodoceLogger.LOGGER.error("TikTok error", exception);
    }

    private void handleConnectException(long token, String username, Exception exception) {
        if (!isTokenCurrent(token)) {
            return;
        }
        liveClient = null;

        String message;
        boolean allowReconnect = true;
        if (exception instanceof TikTokLiveUnknownHostException) {
            message = "Username TikTok nao encontrado: @" + username;
            allowReconnect = false;
        } else if (exception instanceof TikTokLiveOfflineHostException) {
            message = "@" + username + " esta offline no momento.";
        } else {
            message = "Falha ao conectar: " + MessageSanitizer.sanitize(exception.getMessage());
        }

        sessionState.setLastError(message);
        sessionState.setState(ConnectionLifecycleState.ERROR);
        chatGateway.sendSystem(message, false);

        if (allowReconnect) {
            scheduleReconnect(token, username, "falha de conexao");
        }
    }

    private void scheduleReconnect(long token, String username, String reason) {
        ReinodoceConfig config = configSupplier.get();
        int reconnectSeconds = config.getReconnectSeconds();
        if (reconnectSeconds <= 0 || !isTokenCurrent(token)) {
            return;
        }
        if (reconnectTask != null && !reconnectTask.isDone()) {
            return;
        }

        Instant reconnectAt = Instant.now().plusSeconds(reconnectSeconds);
        sessionState.setState(ConnectionLifecycleState.RECONNECT_SCHEDULED);
        sessionState.setReconnectAt(reconnectAt);
        chatGateway.sendSystem("Reconnect em " + reconnectSeconds + "s (" + reason + ").", false);

        reconnectTask = scheduler.schedule(() -> {
            if (!isTokenCurrent(token)) {
                return;
            }
            sessionState.setState(ConnectionLifecycleState.CONNECTING);
            sessionState.setReconnectAt(null);
            ioExecutor.submit(() -> connectInternal(token, username));
        }, reconnectSeconds, TimeUnit.SECONDS);
    }

    private void handleComment(long token, TikTokCommentEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        User user = event.getUser();
        String message = MessageSanitizer.sanitize(event.getText());
        if (message.isBlank()) {
            return;
        }

        int memberLevel = memberLevelResolver.resolveLevel(user);
        if (!ruleEngine.shouldDisplayComment(config, user, memberLevel)) {
            return;
        }

        String username = sanitizeUserName(resolveUserName(user));
        chatGateway.sendLiveComment(config.getChatPrefix(), username, message);
    }

    private void handleFollow(long token, TikTokFollowEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        if (!config.isSynteticFollowEnabled()) {
            return;
        }

        String username = sanitizeUserName(resolveUserName(event.getUser()));
        chatGateway.sendSyntheticFollow(config.getChatPrefix(), username);
    }

    private void handleJoin(long token, TikTokJoinEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        if (!config.isSynteticJoinEnabled()) {
            return;
        }

        String username = sanitizeUserName(resolveUserName(event.getUser()));
        chatGateway.sendSyntheticJoin(config.getChatPrefix(), username);
    }

    private void handleGift(long token, TikTokGiftEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        Gift gift = event.getGift();
        if (!ruleEngine.shouldEmitGift(config, gift)) {
            return;
        }

        GiftComboAggregator.GiftSnapshot snapshot = toGiftSnapshot(event);
        GiftComboMode mode = GiftComboMode.fromString(config.getSynteticGiftComboMode());
        List<GiftComboAggregator.GiftEmission> emissions = giftComboAggregator.handleGift(mode, snapshot);
        emitGiftMessages(config, emissions);
    }

    private void handleGiftCombo(long token, TikTokGiftComboEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        Gift gift = event.getGift();
        if (!ruleEngine.shouldEmitGift(config, gift)) {
            return;
        }

        GiftComboAggregator.GiftSnapshot snapshot = toGiftSnapshot(event);
        GiftComboMode mode = GiftComboMode.fromString(config.getSynteticGiftComboMode());
        boolean finished = event.getComboState() == GiftComboStateType.Finished;
        List<GiftComboAggregator.GiftEmission> emissions = giftComboAggregator.handleCombo(mode, snapshot, finished);
        emitGiftMessages(config, emissions);
    }

    private void handleWebsocketMessage(long token, TikTokWebsocketMessageEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }
        if (event == null || event.getMessage() == null || event.getMessage().getPayload() == null) {
            return;
        }

        String method = event.getMessage().getMethod();
        byte[] payload = event.getMessage().getPayload().toByteArray();
        try {
            if ("WebcastChatMessage".equals(method)) {
                WebcastChatMessage chatMessage = WebcastChatMessage.parseFrom(payload);
                long userId = chatMessage.getUser().getId();
                int level = (int) chatMessage.getUser().getFansClubInfo().getFansLevel();
                String username = sanitizeUserName(chooseRawUserName(chatMessage.getUser().getNickname(), chatMessage.getUser().getUsername()));
                MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(userId, username, level);
                maybeEmitMemberLevelUpgrade(update);
                return;
            }

            if ("WebcastBarrageMessage".equals(method)) {
                WebcastBarrageMessage barrageMessage = WebcastBarrageMessage.parseFrom(payload);
                WebcastBarrageMessage.BarrageType type = barrageMessage.getMsgType();
                if (type == WebcastBarrageMessage.BarrageType.FANSLEVELUPGRADE
                        || type == WebcastBarrageMessage.BarrageType.FANSLEVELENTRANCE) {
                    long userId = barrageMessage.getFansLevelParam().getUser().getId();
                    int level = barrageMessage.getFansLevelParam().getCurrentGrade();
                    String username = sanitizeUserName(chooseRawUserName(
                            barrageMessage.getFansLevelParam().getUser().getNickname(),
                            barrageMessage.getFansLevelParam().getUser().getUsername()
                    ));
                    MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(userId, username, level);
                    maybeEmitMemberLevelUpgrade(update);
                }
            }
        } catch (Exception ignored) {
            ReinodoceLogger.LOGGER.debug("Ignoring websocket payload parse failure for method {}", method);
        }
    }

    private void maybeEmitMemberLevelUpgrade(MemberLevelResolver.LevelUpdate update) {
        if (!update.isUpgrade() || update.newLevel() <= 0) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        if (!config.isSynteticMemberLevelEnabled()) {
            return;
        }

        chatGateway.sendSyntheticMemberLevel(config.getChatPrefix(), sanitizeUserName(update.username()), update.newLevel());
    }

    private void emitGiftMessages(ReinodoceConfig config, List<GiftComboAggregator.GiftEmission> emissions) {
        for (GiftComboAggregator.GiftEmission emission : emissions) {
            if (giftDeduplicator.isDuplicate(emission.messageId(), emission.count())) {
                continue;
            }
            chatGateway.sendSyntheticGift(
                    config.getChatPrefix(),
                    sanitizeUserName(emission.username()),
                    MessageSanitizer.sanitize(emission.giftName()),
                    Math.max(1, emission.count())
            );
        }
    }

    private void emitGiftFromAsyncFlush(GiftComboAggregator.GiftEmission emission) {
        ReinodoceConfig config = configSupplier.get();
        if (!ruleEngine.shouldEmitGift(config, toGift(emission))) {
            return;
        }
        emitGiftMessages(config, List.of(emission));
    }

    private GiftComboAggregator.GiftSnapshot toGiftSnapshot(TikTokGiftEvent event) {
        User user = event.getUser();
        Gift gift = event.getGift();
        long userId = user == null || user.getId() == null ? 0L : user.getId();
        int giftId = gift == null ? 0 : gift.getId();
        int diamonds = gift == null ? 0 : Math.max(0, gift.getDiamondCost());
        int combo = Math.max(1, event.getCombo());
        String username = sanitizeUserName(resolveUserName(user));
        String giftName = MessageSanitizer.sanitize(gift == null ? "presente" : gift.getName());
        long messageId = event.getMessageId();

        return new GiftComboAggregator.GiftSnapshot(
                new GiftComboAggregator.GiftKey(userId, giftId),
                username,
                giftName,
                diamonds,
                combo,
                messageId
        );
    }

    private Gift toGift(GiftComboAggregator.GiftEmission emission) {
        return new Gift(0, emission.giftName(), emission.diamondCost(), "");
    }

    private String resolveUserName(User user) {
        if (user == null) {
            return "desconhecido";
        }
        if (user.getProfileName() != null && !user.getProfileName().isBlank()) {
            return user.getProfileName();
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return "desconhecido";
    }

    private String chooseRawUserName(String profileName, String username) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return "desconhecido";
    }

    private String sanitizeUserName(String raw) {
        String sanitized = MessageSanitizer.sanitize(raw);
        return sanitized.isBlank() ? "desconhecido" : sanitized;
    }

    private boolean isTokenCurrent(long token) {
        return lifecycleToken.get() == token;
    }

    private boolean isConnectionActive(ConnectionLifecycleState state) {
        return state == ConnectionLifecycleState.CONNECTED
                || state == ConnectionLifecycleState.CONNECTING
                || state == ConnectionLifecycleState.RECONNECT_SCHEDULED;
    }

    private void cancelReconnectTask() {
        ScheduledFuture<?> task = reconnectTask;
        reconnectTask = null;
        if (task != null) {
            task.cancel(false);
        }
    }

    private void disconnectCurrentClient() {
        LiveClient current = liveClient;
        liveClient = null;
        if (current != null) {
            try {
                current.disconnect();
            } catch (Exception exception) {
                ReinodoceLogger.LOGGER.debug("Failed to close previous TikTok client cleanly", exception);
            }
        }
    }
}

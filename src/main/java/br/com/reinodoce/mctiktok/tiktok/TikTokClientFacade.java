package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.state.ConnectionLifecycleState;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.ExecutorsFactory;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import br.com.reinodoce.mctiktok.util.NoticeThrottler;
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
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastEmoteChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastMemberMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;

public class TikTokClientFacade {
    private static final Duration ERROR_NOTICE_COOLDOWN = Duration.ofSeconds(8);
    private static final Duration RECONNECT_NOTICE_COOLDOWN = Duration.ofSeconds(5);
    private static final Duration COMMENT_DEDUPLICATION_WINDOW = Duration.ofMinutes(2);
    private static final long COMMENT_FINGERPRINT_TTL_MILLIS = Duration.ofSeconds(6).toMillis();

    private final Supplier<ReinodoceConfig> configSupplier;
    private final MinecraftChatGateway chatGateway;
    private final MessageRuleEngine ruleEngine;
    private final MemberLevelResolver memberLevelResolver;
    private final MessageDeduplicator giftDeduplicator;
    private final MessageDeduplicator commentDeduplicator;
    private final LiveSessionState sessionState;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduler;
    private final GiftComboAggregator giftComboAggregator;
    private final AtomicLong lifecycleToken;
    private final NoticeThrottler errorNoticeThrottler;
    private final NoticeThrottler reconnectNoticeThrottler;
    private final TikTokRichMessageParser richMessageParser;
    private final UnicodeEmojiParser unicodeEmojiParser;
    private final Map<String, Long> recentRenderedCommentFingerprints;

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
        this.commentDeduplicator = new MessageDeduplicator(COMMENT_DEDUPLICATION_WINDOW);
        this.sessionState = new LiveSessionState();
        this.ioExecutor = ExecutorsFactory.newSingleThreadExecutor("reinodoce-tiktok-io");
        this.scheduler = ExecutorsFactory.newSingleThreadScheduledExecutor("reinodoce-tiktok-scheduler");
        this.giftComboAggregator = new GiftComboAggregator(scheduler, this::emitGiftFromAsyncFlush);
        this.lifecycleToken = new AtomicLong(0L);
        this.errorNoticeThrottler = new NoticeThrottler(ERROR_NOTICE_COOLDOWN);
        this.reconnectNoticeThrottler = new NoticeThrottler(RECONNECT_NOTICE_COOLDOWN);
        this.richMessageParser = new TikTokRichMessageParser();
        this.unicodeEmojiParser = new UnicodeEmojiParser();
        this.recentRenderedCommentFingerprints = new ConcurrentHashMap<>();
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
        sessionState.setReconnectAttempts(0);
        sessionState.setState(ConnectionLifecycleState.CONNECTING);
        errorNoticeThrottler.reset();
        reconnectNoticeThrottler.reset();

        ioExecutor.submit(() -> {
            cancelReconnectTask();
            disconnectCurrentClient();
            giftDeduplicator.clear();
            commentDeduplicator.clear();
            giftComboAggregator.clear();
            memberLevelResolver.clear();
            recentRenderedCommentFingerprints.clear();
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
        sessionState.setReconnectAttempts(0);
        sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
        sessionState.setLastError("");
        errorNoticeThrottler.reset();
        reconnectNoticeThrottler.reset();
        ioExecutor.submit(() -> {
            cancelReconnectTask();
            disconnectCurrentClient();
            giftComboAggregator.clear();
            giftDeduplicator.clear();
            commentDeduplicator.clear();
            memberLevelResolver.clear();
            recentRenderedCommentFingerprints.clear();
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
            reconnectNoticeThrottler.reset();
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
        cancelReconnectTask();
        liveClient = client;
        sessionState.setState(ConnectionLifecycleState.CONNECTED);
        sessionState.setUsername(username);
        sessionState.setLastError("");
        sessionState.setReconnectAt(null);
        sessionState.setReconnectAttempts(0);
        reconnectNoticeThrottler.reset();
        chatGateway.sendSystem("Conectado em @" + username, true);
        ReinodoceLogger.LOGGER.info("Connected to TikTok LIVE @{}", username);
    }

    private void handleDisconnected(long token, String username, TikTokDisconnectedEvent event) {
        if (!isTokenCurrent(token)) {
            return;
        }
        liveClient = null;
        String reason = normalizeErrorReason(event == null ? null : event.getReason(), "desconexao sem motivo informado");
        if (reconnectTask != null && !reconnectTask.isDone()) {
            sessionState.setLastError(reason);
            ReinodoceLogger.LOGGER.warn("Disconnected from TikTok LIVE @{} while reconnect is already scheduled (reason={})", username, reason);
            return;
        }

        sessionState.setState(ConnectionLifecycleState.DISCONNECTED);
        sessionState.setLastError(reason);
        sendErrorSystemNotice("disconnect:" + reason, "Desconectado: " + reason);
        ReinodoceLogger.LOGGER.warn("Disconnected from TikTok LIVE @{} (reason={})", username, reason);
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
        String error = normalizeErrorReason(exception.getMessage(), exception.getClass().getSimpleName());
        String message = "Erro na LIVE: " + error;
        String username = sessionState.snapshot().username();
        sessionState.setLastError(message);
        sessionState.setState(ConnectionLifecycleState.ERROR);
        sendErrorSystemNotice("runtime:" + exception.getClass().getName() + ":" + error, message);
        ReinodoceLogger.LOGGER.warn("TikTok runtime connection error for @{} (retryable=true): {}", username, error, exception);
        disconnectCurrentClient();
        scheduleReconnect(token, username, "erro de conexao");
    }

    private void handleConnectException(long token, String username, Exception exception) {
        if (!isTokenCurrent(token)) {
            return;
        }
        liveClient = null;

        String message;
        boolean allowReconnect = shouldRetryConnectFailure(exception);
        if (!allowReconnect) {
            message = "Username TikTok nao encontrado: @" + username + ". Verifique e tente novamente.";
        } else if (exception instanceof TikTokLiveOfflineHostException) {
            message = "@" + username + " esta offline no momento.";
        } else {
            message = "Falha ao conectar: " + normalizeErrorReason(exception.getMessage(), "erro de rede ou servico indisponivel");
        }

        sessionState.setLastError(message);
        sessionState.setState(ConnectionLifecycleState.ERROR);
        sendErrorSystemNotice("connect:" + username + ":" + exception.getClass().getName(), message);
        ReinodoceLogger.LOGGER.warn("Connect failure for @{} (reconnect={}) -> {}", username, allowReconnect, message, exception);

        if (allowReconnect) {
            scheduleReconnect(token, username, "falha de conexao");
        } else {
            sessionState.setReconnectAttempts(0);
        }
    }

    private void scheduleReconnect(long token, String username, String reason) {
        ReinodoceConfig config = configSupplier.get();
        int reconnectSeconds = config.getReconnectSeconds();
        if (reconnectSeconds <= 0 || !isTokenCurrent(token)) {
            return;
        }
        if (reconnectTask != null && !reconnectTask.isDone()) {
            ReinodoceLogger.LOGGER.debug("Reconnect already scheduled for @{}, skipping duplicate schedule.", username);
            return;
        }

        int attempt = sessionState.incrementReconnectAttempts();
        Instant reconnectAt = Instant.now().plusSeconds(reconnectSeconds);
        sessionState.setState(ConnectionLifecycleState.RECONNECT_SCHEDULED);
        sessionState.setReconnectAt(reconnectAt);

        String reconnectNotice = "Reconnect tentativa " + attempt + " em " + reconnectSeconds + "s (" + reason + ").";
        if (reconnectNoticeThrottler.shouldEmit(username + ":" + reason)) {
            chatGateway.sendSystem(reconnectNotice, false);
        }
        ReinodoceLogger.LOGGER.warn("Scheduling reconnect attempt {} to @{} in {}s (reason={})", attempt, username, reconnectSeconds, reason);

        reconnectTask = scheduler.schedule(() -> {
            if (!isTokenCurrent(token)) {
                return;
            }
            sessionState.setState(ConnectionLifecycleState.CONNECTING);
            sessionState.setReconnectAt(null);
            ReinodoceLogger.LOGGER.info("Executing reconnect attempt {} to @{}", attempt, username);
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
        if (wasRecentlyRenderedComment(username, message)) {
            return;
        }

        rememberRenderedComment(username, message);
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendLiveComment(
                    config.getChatPrefix(),
                    richTextMessage(0L, username, TikTokMediaResolver.resolveUserAvatarUrl(user), message)
            );
            return;
        }
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
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendSyntheticFollow(
                    config.getChatPrefix(),
                    richAuthorOnlyMessage(username, TikTokMediaResolver.resolveUserAvatarUrl(event.getUser()))
            );
            return;
        }
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
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendSyntheticJoin(
                    config.getChatPrefix(),
                    richAuthorOnlyMessage(username, TikTokMediaResolver.resolveUserAvatarUrl(event.getUser()))
            );
            return;
        }
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
                handleRawChatMessage(token, chatMessage);
                return;
            }

            if ("WebcastEmoteChatMessage".equals(method)) {
                WebcastEmoteChatMessage emoteChatMessage = WebcastEmoteChatMessage.parseFrom(payload);
                handleRawEmoteMessage(token, emoteChatMessage);
                return;
            }

            if ("WebcastBarrageMessage".equals(method)) {
                handleRawBarrageMessage(token, WebcastBarrageMessage.parseFrom(payload));
                return;
            }

            if ("WebcastMemberMessage".equals(method)) {
                handleRawMemberMessage(token, WebcastMemberMessage.parseFrom(payload));
            }
        } catch (Exception ignored) {
            ReinodoceLogger.LOGGER.debug("Ignoring websocket payload parse failure for method {}", method);
        }
    }

    private void handleRawChatMessage(long token, WebcastChatMessage chatMessage) {
        io.github.jwdeveloper.tiktok.data.models.users.User user = io.github.jwdeveloper.tiktok.data.models.users.User.map(
                chatMessage.getUser(),
                chatMessage.getUserIdentity()
        );
        String username = sanitizeUserName(chooseRawUserName(chatMessage.getUser().getNickname(), chatMessage.getUser().getUsername()));
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(chatMessage.getUser());
        MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(
                chatMessage.getUser().getId(),
                username,
                avatarUrl,
                (int) chatMessage.getUser().getFansClubInfo().getFansLevel()
        );
        maybeEmitMemberLevelUpgrade(update);

        emitLiveComment(
                token,
                user,
                username,
                avatarUrl,
                memberLevelResolver.resolveLevel(user),
                richMessageParser.parseChatMessage(chatMessage, username),
                false
        );
    }

    private void handleRawEmoteMessage(long token, WebcastEmoteChatMessage emoteChatMessage) {
        io.github.jwdeveloper.tiktok.data.models.users.User user = io.github.jwdeveloper.tiktok.data.models.users.User.map(
                emoteChatMessage.getUser(),
                emoteChatMessage.getUserIdentity()
        );
        String username = sanitizeUserName(chooseRawUserName(emoteChatMessage.getUser().getNickname(), emoteChatMessage.getUser().getUsername()));
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(emoteChatMessage.getUser());
        emitLiveComment(
                token,
                user,
                username,
                avatarUrl,
                memberLevelResolver.resolveLevel(user),
                richMessageParser.parseEmoteChatMessage(emoteChatMessage, username),
                false
        );
    }

    private void handleRawBarrageMessage(long token, WebcastBarrageMessage barrageMessage) {
        if (barrageMessage == null) {
            return;
        }

        WebcastBarrageMessage.BarrageType type = barrageMessage.getMsgType();
        switch (type) {
            case COMMONBARRAGE -> handleStarBarrageComment(token, barrageMessage);
            case USERUPGRADE, GRADEUSERENTRANCENOTIFICATION -> handleUserGradeBarrage(barrageMessage);
            case FANSLEVELUPGRADE, FANSLEVELENTRANCE -> handleFansLevelBarrage(barrageMessage);
            default -> {
            }
        }
    }

    private void handleStarBarrageComment(long token, WebcastBarrageMessage barrageMessage) {
        TikTokRichMessageParser.ParsedText parsed = richMessageParser.parseBarrageText(barrageMessage);
        if (!parsed.hasRenderableContent()) {
            logRecognizedBarrageNotRendered(barrageMessage, parsed.plainText());
            return;
        }

        io.github.jwdeveloper.tiktok.messages.data.User rawUser = resolveBarrageRawUser(parsed, barrageMessage);
        io.github.jwdeveloper.tiktok.data.models.users.User user = rawUser == null ? null : io.github.jwdeveloper.tiktok.data.models.users.User.map(rawUser);
        String username = resolveBarrageUsername(parsed, rawUser);
        String avatarUrl = resolveBarrageAvatarUrl(parsed, rawUser);
        int memberLevel = resolveBarrageMemberLevel(user, barrageMessage);
        RichLiveMessage richMessage = new RichLiveMessage(resolveMessageId(barrageMessage.hasCommon() ? barrageMessage.getCommon() : null), username, parsed.segments());

        emitLiveComment(token, user, username, avatarUrl, memberLevel, richMessage, true);
    }

    private void handleUserGradeBarrage(WebcastBarrageMessage barrageMessage) {
        if (!barrageMessage.hasUserGradeParam()) {
            logRecognizedBarrageNotRendered(barrageMessage, "");
            return;
        }

        io.github.jwdeveloper.tiktok.messages.data.User rawUser = barrageMessage.getUserGradeParam().getUser();
        MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(
                rawUser.getId(),
                sanitizeUserName(chooseRawUserName(rawUser.getNickname(), rawUser.getUsername())),
                TikTokMediaResolver.resolveUserAvatarUrl(rawUser),
                barrageMessage.getUserGradeParam().getCurrentGrade()
        );
        if (!maybeEmitMemberLevelUpgrade(update)) {
            logRecognizedBarrageNotRendered(barrageMessage, "");
        }
    }

    private void handleFansLevelBarrage(WebcastBarrageMessage barrageMessage) {
        if (!barrageMessage.hasFansLevelParam()) {
            logRecognizedBarrageNotRendered(barrageMessage, "");
            return;
        }

        io.github.jwdeveloper.tiktok.messages.data.User rawUser = barrageMessage.getFansLevelParam().getUser();
        MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(
                rawUser.getId(),
                sanitizeUserName(chooseRawUserName(rawUser.getNickname(), rawUser.getUsername())),
                TikTokMediaResolver.resolveUserAvatarUrl(rawUser),
                barrageMessage.getFansLevelParam().getCurrentGrade()
        );
        if (!maybeEmitMemberLevelUpgrade(update)) {
            logRecognizedBarrageNotRendered(barrageMessage, "");
        }
    }

    private void handleRawMemberMessage(long token, WebcastMemberMessage memberMessage) {
        if (!isTokenCurrent(token) || memberMessage == null) {
            return;
        }

        io.github.jwdeveloper.tiktok.messages.data.User rawUser = memberMessage.hasUser() && isKnownRawUser(memberMessage.getUser())
                ? memberMessage.getUser()
                : null;
        long userId = rawUser != null && rawUser.getId() > 0 ? rawUser.getId() : memberMessage.getUserId();
        String fallbackUsername = rawUser == null ? "desconhecido" : chooseRawUserName(rawUser.getNickname(), rawUser.getUsername());
        String username = sanitizeUserName(memberLevelResolver.getKnownUsername(userId, fallbackUsername));
        String avatarUrl = memberLevelResolver.getKnownAvatarUrl(
                userId,
                rawUser == null ? TikTokMediaResolver.defaultAvatarUrl() : TikTokMediaResolver.resolveUserAvatarUrl(rawUser)
        );
        String levelText = extractMemberMessageText(memberMessage, richMessageParser);
        int level = MemberLevelResolver.extractLevelFromText(levelText);
        if (level <= 0) {
            logRecognizedMemberNotRendered(memberMessage, levelText);
            return;
        }

        MemberLevelResolver.LevelUpdate update = memberLevelResolver.updateLevel(userId, username, avatarUrl, level);
        if (!maybeEmitMemberLevelUpgrade(update)) {
            logRecognizedMemberNotRendered(memberMessage, levelText);
        }
    }

    private void emitLiveComment(
            long token,
            io.github.jwdeveloper.tiktok.data.models.users.User user,
            String username,
            String avatarUrl,
            int memberLevel,
            RichLiveMessage richMessage,
            boolean starComment
    ) {
        if (!isTokenCurrent(token) || richMessage == null) {
            return;
        }

        ReinodoceConfig config = configSupplier.get();
        if (!ruleEngine.shouldDisplayComment(config, user, memberLevel)) {
            return;
        }

        String plainText = MessageSanitizer.sanitize(richMessage.plainText());
        if (plainText.isBlank() && !richMessage.hasInlineMedia()) {
            return;
        }
        if (commentDeduplicator.isDuplicate(richMessage.messageId(), 1)) {
            return;
        }

        rememberRenderedComment(username, plainText);
        if (config.isChatEmotesEnabled()) {
            RichLiveMessage enriched = enrichCommentMessage(username, avatarUrl, richMessage);
            if (starComment) {
                chatGateway.sendStarComment(config.getChatPrefix(), enriched);
            } else {
                chatGateway.sendLiveComment(config.getChatPrefix(), enriched);
            }
            return;
        }

        if (!plainText.isBlank()) {
            if (starComment) {
                chatGateway.sendStarComment(config.getChatPrefix(), username, plainText);
            } else {
                chatGateway.sendLiveComment(config.getChatPrefix(), username, plainText);
            }
        }
    }

    private boolean maybeEmitMemberLevelUpgrade(MemberLevelResolver.LevelUpdate update) {
        if (!update.isUpgrade() || update.newLevel() <= 0) {
            return false;
        }

        ReinodoceConfig config = configSupplier.get();
        if (!config.isSynteticMemberLevelEnabled()) {
            return false;
        }

        String username = sanitizeUserName(update.username());
        if (config.isChatEmotesEnabled()) {
            chatGateway.sendSyntheticMemberLevel(
                    config.getChatPrefix(),
                    richAuthorOnlyMessage(username, update.avatarUrl()),
                    update.newLevel()
            );
            return true;
        }
        chatGateway.sendSyntheticMemberLevel(config.getChatPrefix(), username, update.newLevel());
        return true;
    }

    private void emitGiftMessages(ReinodoceConfig config, List<GiftComboAggregator.GiftEmission> emissions) {
        for (GiftComboAggregator.GiftEmission emission : emissions) {
            if (giftDeduplicator.isDuplicate(emission.messageId(), emission.count())) {
                continue;
            }
            String username = sanitizeUserName(emission.username());
            String giftName = MessageSanitizer.sanitize(emission.giftName());
            if (config.isChatEmotesEnabled()) {
                chatGateway.sendSyntheticGift(
                        config.getChatPrefix(),
                        richGiftMessage(
                                username,
                                emission.avatarUrl(),
                                giftName,
                                emission.giftIconUrl(),
                                Math.max(1, emission.count())
                        )
                );
                continue;
            }
            chatGateway.sendSyntheticGift(config.getChatPrefix(), username, giftName, Math.max(1, emission.count()));
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
        String avatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(user);
        String giftName = MessageSanitizer.sanitize(gift == null ? "presente" : gift.getName());
        String giftIconUrl = TikTokMediaResolver.resolveGiftIconUrl(gift);
        long messageId = event.getMessageId();

        return new GiftComboAggregator.GiftSnapshot(
                new GiftComboAggregator.GiftKey(userId, giftId),
                username,
                avatarUrl,
                giftName,
                giftIconUrl,
                diamonds,
                combo,
                messageId
        );
    }

    private Gift toGift(GiftComboAggregator.GiftEmission emission) {
        return new Gift(0, emission.giftName(), emission.diamondCost(), "");
    }

    private RichLiveMessage enrichCommentMessage(String username, String avatarUrl, RichLiveMessage richMessage) {
        return new RichLiveMessage(
                richMessage.messageId(),
                authorSegments(username, avatarUrl),
                unicodeEmojiParser.expandSegments(richMessage.bodySegments())
        );
    }

    private RichLiveMessage richTextMessage(long messageId, String username, String avatarUrl, String bodyText) {
        return new RichLiveMessage(messageId, authorSegments(username, avatarUrl), unicodeEmojiParser.parseText(bodyText));
    }

    private RichLiveMessage richAuthorOnlyMessage(String username, String avatarUrl) {
        return new RichLiveMessage(0L, authorSegments(username, avatarUrl), List.of());
    }

    private RichLiveMessage richGiftMessage(String username, String avatarUrl, String giftName, String giftIconUrl, int count) {
        return new RichLiveMessage(0L, authorSegments(username, avatarUrl), giftSegments(giftName, giftIconUrl, count));
    }

    private List<RichLiveMessage.Segment> authorSegments(String username, String avatarUrl) {
        List<RichLiveMessage.Segment> segments = new java.util.ArrayList<>();
        segments.add(new RichLiveMessage.AvatarSegment(
                avatarUrl == null || avatarUrl.isBlank() ? TikTokMediaResolver.defaultAvatarUrl() : avatarUrl,
                ""
        ));
        segments.add(new RichLiveMessage.TextSegment(" "));
        segments.addAll(unicodeEmojiParser.parseText(username));
        return segments;
    }

    private List<RichLiveMessage.Segment> giftSegments(String giftName, String giftIconUrl, int count) {
        List<RichLiveMessage.Segment> segments = new java.util.ArrayList<>();
        segments.add(new RichLiveMessage.TextSegment("enviou "));
        if (giftIconUrl != null && !giftIconUrl.isBlank()) {
            segments.add(new RichLiveMessage.GiftIconSegment(giftName, giftIconUrl, ""));
            segments.add(new RichLiveMessage.TextSegment(" "));
        }
        segments.addAll(unicodeEmojiParser.parseText(giftName));
        segments.add(new RichLiveMessage.TextSegment(" x" + Math.max(1, count)));
        return segments;
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

    private io.github.jwdeveloper.tiktok.messages.data.User resolveBarrageRawUser(
            TikTokRichMessageParser.ParsedText parsed,
            WebcastBarrageMessage barrageMessage
    ) {
        if (parsed.detectedUser() != null && isKnownRawUser(parsed.detectedUser())) {
            return parsed.detectedUser();
        }
        if (barrageMessage.hasUserGradeParam() && isKnownRawUser(barrageMessage.getUserGradeParam().getUser())) {
            return barrageMessage.getUserGradeParam().getUser();
        }
        if (barrageMessage.hasFansLevelParam() && isKnownRawUser(barrageMessage.getFansLevelParam().getUser())) {
            return barrageMessage.getFansLevelParam().getUser();
        }
        return null;
    }

    private String resolveBarrageUsername(TikTokRichMessageParser.ParsedText parsed, io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        if (!MessageSanitizer.sanitize(parsed.detectedUsername()).isBlank()) {
            return sanitizeUserName(parsed.detectedUsername());
        }
        if (rawUser != null) {
            return sanitizeUserName(chooseRawUserName(rawUser.getNickname(), rawUser.getUsername()));
        }
        return "desconhecido";
    }

    private String resolveBarrageAvatarUrl(TikTokRichMessageParser.ParsedText parsed, io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        if (!MessageSanitizer.sanitize(parsed.detectedAvatarUrl()).isBlank()) {
            return parsed.detectedAvatarUrl();
        }
        if (rawUser != null) {
            return TikTokMediaResolver.resolveUserAvatarUrl(rawUser);
        }
        return TikTokMediaResolver.defaultAvatarUrl();
    }

    private int resolveBarrageMemberLevel(io.github.jwdeveloper.tiktok.data.models.users.User user, WebcastBarrageMessage barrageMessage) {
        int memberLevel = user == null ? 0 : memberLevelResolver.resolveLevel(user);
        if (memberLevel > 0) {
            return memberLevel;
        }
        if (barrageMessage.hasFansLevelParam()) {
            return Math.max(0, barrageMessage.getFansLevelParam().getCurrentGrade());
        }
        if (barrageMessage.hasUserGradeParam()) {
            return Math.max(0, barrageMessage.getUserGradeParam().getCurrentGrade());
        }
        return 0;
    }

    static String extractMemberMessageText(WebcastMemberMessage memberMessage, TikTokRichMessageParser richMessageParser) {
        if (memberMessage.hasAnchorDisplayText()) {
            String anchorText = MessageSanitizer.sanitize(richMessageParser.parseText(memberMessage.getAnchorDisplayText()).plainText());
            if (!anchorText.isBlank()) {
                return anchorText;
            }
        }

        String actionDescription = MessageSanitizer.sanitize(memberMessage.getActionDescription());
        if (!actionDescription.isBlank()) {
            return actionDescription;
        }

        return MessageSanitizer.sanitize(memberMessage.getPopStr());
    }

    private boolean isKnownRawUser(io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        if (rawUser == null) {
            return false;
        }
        return rawUser.getId() > 0
                || !MessageSanitizer.sanitize(rawUser.getNickname()).isBlank()
                || !MessageSanitizer.sanitize(rawUser.getUsername()).isBlank();
    }

    private long resolveMessageId(io.github.jwdeveloper.tiktok.messages.data.CommonMessageData common) {
        return common == null ? 0L : common.getMsgId();
    }

    private String sanitizeUserName(String raw) {
        String sanitized = MessageSanitizer.sanitize(raw);
        return sanitized.isBlank() ? "desconhecido" : sanitized;
    }

    private void sendErrorSystemNotice(String key, String message) {
        if (errorNoticeThrottler.shouldEmit(key)) {
            chatGateway.sendSystem(message, false);
        }
    }

    private String normalizeErrorReason(String rawReason, String fallback) {
        String reason = MessageSanitizer.sanitize(rawReason);
        if (reason.isBlank() || "None".equalsIgnoreCase(reason)) {
            return fallback;
        }
        return reason;
    }

    static boolean shouldRetryConnectFailure(Throwable throwable) {
        return !(throwable instanceof TikTokLiveUnknownHostException);
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

    private boolean wasRecentlyRenderedComment(String username, String message) {
        long now = System.currentTimeMillis();
        cleanupRenderedCommentFingerprints(now);
        return recentRenderedCommentFingerprints.containsKey(commentFingerprint(username, message));
    }

    private void rememberRenderedComment(String username, String message) {
        String sanitizedMessage = MessageSanitizer.sanitize(message);
        if (sanitizedMessage.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        cleanupRenderedCommentFingerprints(now);
        recentRenderedCommentFingerprints.put(commentFingerprint(username, sanitizedMessage), now);
    }

    private void cleanupRenderedCommentFingerprints(long now) {
        recentRenderedCommentFingerprints.entrySet().removeIf(entry -> now - entry.getValue() > COMMENT_FINGERPRINT_TTL_MILLIS);
    }

    private String commentFingerprint(String username, String message) {
        return sanitizeUserName(username) + "|" + MessageSanitizer.sanitize(message);
    }

    private void logRecognizedBarrageNotRendered(WebcastBarrageMessage barrageMessage, String text) {
        String eventName = barrageMessage.hasEvent() ? MessageSanitizer.sanitize(barrageMessage.getEvent().getEventName()) : "";
        ReinodoceLogger.LOGGER.debug(
                "Recognized barrage payload not rendered msgType={} eventName={} text={}",
                barrageMessage.getMsgType(),
                eventName,
                MessageSanitizer.sanitize(text)
        );
    }

    private void logRecognizedMemberNotRendered(WebcastMemberMessage memberMessage, String text) {
        ReinodoceLogger.LOGGER.debug(
                "Recognized member payload not rendered action={} text={}",
                memberMessage.getAction(),
                MessageSanitizer.sanitize(text)
        );
    }
}

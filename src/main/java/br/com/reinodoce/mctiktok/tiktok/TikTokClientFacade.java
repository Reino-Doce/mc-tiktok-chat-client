package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import io.github.jwdeveloper.tiktok.live.builder.LiveClientBuilder;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastMemberMessage;

import java.util.function.Supplier;

/**
 * Facade over the TikTok LIVE client lifecycle and event dispatch graph.
 */
public class TikTokClientFacade {
    private final LiveSessionState sessionState;
    private final SessionStatsTracker statsTracker;
    private final TikTokConnectionLifecycle lifecycle;
    private final TikTokEventDispatcher eventDispatcher;
    private final WebsocketMessageDispatcher websocketDispatcher;

    /**
     * Creates a facade and wires the TikTok event handlers.
     *
     * @param configSupplier current configuration supplier
     * @param chatGateway chat event sink
     * @param ruleEngine message rule engine
     * @param memberLevelResolver member-level resolver
     * @param giftDeduplicator gift deduplicator
     * @param languageSupplier selected client language supplier
     */
    public TikTokClientFacade(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            MessageDeduplicator giftDeduplicator,
            Supplier<String> languageSupplier
    ) {
        TikTokFacadeWiring wiring = TikTokFacadeWiring.assemble(
                configSupplier, chatGateway, ruleEngine, memberLevelResolver, giftDeduplicator, languageSupplier);
        wiring.bindFacade(this);
        this.sessionState = wiring.sessionState();
        this.statsTracker = wiring.statsTracker();
        this.lifecycle = wiring.lifecycle();
        this.eventDispatcher = wiring.eventDispatcher();
        this.websocketDispatcher = wiring.websocketDispatcher();
    }

    /**
     * Starts a TikTok LIVE connection.
     *
     * @param usernameInput normalized username
     * @return command result
     */
    public CommandResult connect(String usernameInput) {
        return lifecycle.connect(usernameInput);
    }

    /**
     * Stops the active TikTok LIVE connection.
     *
     * @return command result
     */
    public CommandResult disconnect() {
        return lifecycle.disconnect();
    }

    /**
     * Returns current connection status.
     *
     * @return session snapshot
     */
    public LiveSessionState.Snapshot status() {
        return sessionState.snapshot();
    }

    /**
     * Returns current session statistics.
     *
     * @return stats snapshot
     */
    public SessionStatsTracker.Snapshot stats() {
        return statsTracker.snapshot();
    }

    /**
     * Resets current session statistics without changing connection state.
     */
    public void resetStats() {
        statsTracker.reset();
    }

    /**
     * Records a local lifecycle error without attempting a network connection.
     *
     * @param message error text to expose through status
     */
    public void recordLocalError(String message) {
        sessionState.setLastError(message);
    }

    /**
     * Applies runtime configuration changes to the lifecycle manager.
     */
    public void onConfigUpdated() {
        lifecycle.onConfigUpdated();
    }

    static boolean shouldRetryConnectFailure(Throwable throwable) {
        return TikTokConnectionLifecycle.shouldRetryConnectFailure(throwable);
    }

    static String extractMemberMessageText(WebcastMemberMessage memberMessage, TikTokRichMessageParser richMessageParser) {
        if (memberMessage.hasAnchorDisplayText()) {
            String anchorText = MessageSanitizer.sanitize(
                    richMessageParser.parseText(memberMessage.getAnchorDisplayText()).plainText());
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

    void bindCallbacks(LiveClientBuilder builder, long token) {
        builder.onComment((liveClient, event) -> eventDispatcher.onComment(token, event));
        builder.onFollow((liveClient, event) -> eventDispatcher.onFollow(token, event));
        builder.onJoin((liveClient, event) -> eventDispatcher.onJoin(token, event));
        builder.onGift((liveClient, event) -> eventDispatcher.onGift(token, event));
        builder.onGiftCombo((liveClient, event) -> eventDispatcher.onGiftCombo(token, event));
        builder.onWebsocketMessage((liveClient, event) -> {
            if (lifecycle.isTokenCurrent(token)) {
                websocketDispatcher.dispatch(event);
            }
        });
    }
}

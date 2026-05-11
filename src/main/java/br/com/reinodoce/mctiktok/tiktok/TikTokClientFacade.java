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

public class TikTokClientFacade {
    private final LiveSessionState sessionState;
    private final TikTokConnectionLifecycle lifecycle;
    private final TikTokEventDispatcher eventDispatcher;
    private final WebsocketMessageDispatcher websocketDispatcher;

    public TikTokClientFacade(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            MessageDeduplicator giftDeduplicator
    ) {
        TikTokFacadeWiring wiring = TikTokFacadeWiring.assemble(
                configSupplier, chatGateway, ruleEngine, memberLevelResolver, giftDeduplicator);
        wiring.bindFacade(this);
        this.sessionState = wiring.sessionState();
        this.lifecycle = wiring.lifecycle();
        this.eventDispatcher = wiring.eventDispatcher();
        this.websocketDispatcher = wiring.websocketDispatcher();
    }

    public CommandResult connect(String usernameInput) {
        return lifecycle.connect(usernameInput);
    }

    public CommandResult disconnect() {
        return lifecycle.disconnect();
    }

    public LiveSessionState.Snapshot status() {
        return sessionState.snapshot();
    }

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

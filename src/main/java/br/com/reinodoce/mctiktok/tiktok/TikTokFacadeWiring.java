package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.util.ExecutorsFactory;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import br.com.reinodoce.mctiktok.util.NoticeThrottler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Dependency-injection factory for {@link TikTokClientFacade}. Couples many helper classes by design;
 * suppressed for ClassDataAbstractionCoupling / CouplingBetweenObjects in the static-analysis configs.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"})
record TikTokFacadeWiring(
        LiveSessionState sessionState,
        SessionStatsTracker statsTracker,
        TikTokEventDispatcher eventDispatcher,
        WebsocketMessageDispatcher websocketDispatcher,
        LifecycleHookBinding lifecycleBinding
) {
    private static final int ERROR_NOTICE_COOLDOWN_SECONDS = 8;
    private static final int RECONNECT_NOTICE_COOLDOWN_SECONDS = 5;
    private static final int COMMENT_DEDUPLICATION_WINDOW_MINUTES = 2;

    static TikTokFacadeWiring assemble(
            Supplier<ReinodoceConfig> configSupplier,
            ChatEventSink chatGateway,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            MessageDeduplicator giftDeduplicator,
            Supplier<String> languageSupplier
    ) {
        AssemblyContext context = new AssemblyContext(
                configSupplier, chatGateway, ruleEngine, memberLevelResolver, giftDeduplicator, languageSupplier);
        return context.assemble();
    }

    TikTokConnectionLifecycle lifecycle() {
        return lifecycleBinding.lifecycle();
    }

    void bindFacade(TikTokClientFacade facade) {
        lifecycleBinding.build(facade);
    }

    @SuppressWarnings("PMD.CouplingBetweenObjects")
    private static final class AssemblyContext {
        private final Supplier<ReinodoceConfig> configSupplier;
        private final ChatEventSink chatGateway;
        private final MessageRuleEngine ruleEngine;
        private final MemberLevelResolver memberLevelResolver;
        private final MessageDeduplicator giftDeduplicator;
        private final Supplier<String> languageSupplier;
        private final MessageDeduplicator commentDeduplicator;
        private final LiveSessionState sessionState;
        private final SharedExecutors executors;

        AssemblyContext(
                Supplier<ReinodoceConfig> configSupplier,
                ChatEventSink chatGateway,
                MessageRuleEngine ruleEngine,
                MemberLevelResolver memberLevelResolver,
                MessageDeduplicator giftDeduplicator,
                Supplier<String> languageSupplier
        ) {
            this.configSupplier = configSupplier;
            this.chatGateway = chatGateway;
            this.ruleEngine = ruleEngine;
            this.memberLevelResolver = memberLevelResolver;
            this.giftDeduplicator = giftDeduplicator;
            this.languageSupplier = languageSupplier;
            this.commentDeduplicator = new MessageDeduplicator(
                    Duration.ofMinutes(COMMENT_DEDUPLICATION_WINDOW_MINUTES));
            this.sessionState = new LiveSessionState();
            this.executors = SharedExecutors.create();
        }

        TikTokFacadeWiring assemble() {
            Emitters emitters = buildEmitters();
            Handlers handlers = buildHandlers(emitters);
            LifecycleHookBinding binding = buildLifecycleBinding(emitters);
            TikTokEventDispatcher.Dependencies deps = new TikTokEventDispatcher.Dependencies(
                    configSupplier, chatGateway, ruleEngine, memberLevelResolver,
                    emitters.renderedTracker(), emitters.messageFactory(),
                    emitters.giftEmitter(), emitters.giftComboAggregator(),
                    emitters.statsTracker(), emitters.moderationDuplicateTracker());
            TikTokEventDispatcher dispatcher = new TikTokEventDispatcher(deps, binding::isTokenCurrentLazy);
            return new TikTokFacadeWiring(
                    sessionState, emitters.statsTracker(), dispatcher, handlers.websocketDispatcher(), binding);
        }

        private Emitters buildEmitters() {
            RenderedCommentTracker renderedTracker = new RenderedCommentTracker();
            SessionStatsTracker statsTracker = new SessionStatsTracker();
            ModerationDuplicateTracker moderationDuplicateTracker = new ModerationDuplicateTracker();
            RichLiveMessageFactory messageFactory = new RichLiveMessageFactory(new UnicodeEmojiParser());
            LiveCommentEmitter.Dependencies liveCommentDependencies = new LiveCommentEmitter.Dependencies(
                    configSupplier, chatGateway, ruleEngine, commentDeduplicator,
                    renderedTracker, messageFactory, statsTracker, moderationDuplicateTracker);
            LiveCommentEmitter liveCommentEmitter = new LiveCommentEmitter(liveCommentDependencies);
            MemberLevelEmitter memberLevelEmitter = new MemberLevelEmitter(
                    configSupplier, chatGateway, messageFactory, statsTracker);
            TikTokGiftEmitter giftEmitter = new TikTokGiftEmitter(
                    configSupplier, chatGateway, ruleEngine, giftDeduplicator, messageFactory, statsTracker);
            GiftComboAggregator giftComboAggregator = new GiftComboAggregator(
                    executors.scheduler(), giftEmitter::emitFromAsyncFlush);
            return new Emitters(
                    renderedTracker, statsTracker, moderationDuplicateTracker,
                    messageFactory, liveCommentEmitter, memberLevelEmitter,
                    giftEmitter, new TikTokRichMessageParser(), giftComboAggregator);
        }

        private Handlers buildHandlers(Emitters emitters) {
            BarrageMessageHandler barrageHandler = new BarrageMessageHandler(
                    emitters.parser(), memberLevelResolver,
                    emitters.memberLevelEmitter(), emitters.liveCommentEmitter());
            MemberMessageHandler memberHandler = new MemberMessageHandler(
                    emitters.parser(), memberLevelResolver, emitters.memberLevelEmitter());
            WebsocketMessageDispatcher websocketDispatcher = new WebsocketMessageDispatcher(
                    emitters.parser(), memberLevelResolver,
                    emitters.liveCommentEmitter(),
                    barrageHandler, memberHandler);
            return new Handlers(websocketDispatcher);
        }

        private LifecycleHookBinding buildLifecycleBinding(Emitters emitters) {
            Runnable reset = () -> resetTransientState(emitters);
            TikTokConnectionLifecycle.LifecycleParams params = new TikTokConnectionLifecycle.LifecycleParams(
                    configSupplier, chatGateway, sessionState,
                    executors.ioExecutor(), executors.scheduler(),
                    new NoticeThrottler(Duration.ofSeconds(ERROR_NOTICE_COOLDOWN_SECONDS)),
                    new NoticeThrottler(Duration.ofSeconds(RECONNECT_NOTICE_COOLDOWN_SECONDS)),
                    reset,
                    emitters.statsTracker()::reset,
                    languageSupplier);
            return new LifecycleHookBinding(params);
        }

        private void resetTransientState(Emitters emitters) {
            giftDeduplicator.clear();
            commentDeduplicator.clear();
            emitters.giftComboAggregator().clear();
            memberLevelResolver.clear();
            emitters.renderedTracker().clear();
            emitters.moderationDuplicateTracker().clear();
        }
    }

    private record SharedExecutors(ExecutorService ioExecutor, ScheduledExecutorService scheduler) {
        static SharedExecutors create() {
            return new SharedExecutors(
                    ExecutorsFactory.newSingleThreadExecutor("reinodoce-tiktok-io"),
                    ExecutorsFactory.newSingleThreadScheduledExecutor("reinodoce-tiktok-scheduler"));
        }
    }

    private record Emitters(
            RenderedCommentTracker renderedTracker,
            SessionStatsTracker statsTracker,
            ModerationDuplicateTracker moderationDuplicateTracker,
            RichLiveMessageFactory messageFactory,
            LiveCommentEmitter liveCommentEmitter,
            MemberLevelEmitter memberLevelEmitter,
            TikTokGiftEmitter giftEmitter,
            TikTokRichMessageParser parser,
            GiftComboAggregator giftComboAggregator
    ) {
    }

    private record Handlers(WebsocketMessageDispatcher websocketDispatcher) {
    }

    static final class LifecycleHookBinding {
        private final TikTokConnectionLifecycle.LifecycleParams params;
        private TikTokConnectionLifecycle builtLifecycle;

        LifecycleHookBinding(TikTokConnectionLifecycle.LifecycleParams params) {
            this.params = params;
        }

        void build(TikTokClientFacade facade) {
            this.builtLifecycle = new TikTokConnectionLifecycle(params, facade::bindCallbacks);
        }

        TikTokConnectionLifecycle lifecycle() {
            return builtLifecycle;
        }

        boolean isTokenCurrentLazy(long token) {
            return builtLifecycle != null && builtLifecycle.isTokenCurrent(token);
        }
    }
}

package br.com.reinodoce.mctiktok.tiktok;

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
            TikTokRuntimeServices runtimeServices,
            MessageRuleEngine ruleEngine,
            MemberLevelResolver memberLevelResolver,
            MessageDeduplicator giftDeduplicator
    ) {
        AssemblyContext context = new AssemblyContext(
                configSupplier, runtimeServices, ruleEngine, memberLevelResolver,
                giftDeduplicator);
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
        private final TikTokRuntimeServices runtimeServices;
        private final MessageRuleEngine ruleEngine;
        private final MemberLevelResolver memberLevelResolver;
        private final MessageDeduplicator giftDeduplicator;
        private final MessageDeduplicator commentDeduplicator;
        private final LiveSessionState sessionState;
        private final SharedExecutors executors;

        AssemblyContext(
                Supplier<ReinodoceConfig> configSupplier,
                TikTokRuntimeServices runtimeServices,
                MessageRuleEngine ruleEngine,
                MemberLevelResolver memberLevelResolver,
                MessageDeduplicator giftDeduplicator
        ) {
            this.configSupplier = configSupplier;
            this.runtimeServices = runtimeServices;
            this.ruleEngine = ruleEngine;
            this.memberLevelResolver = memberLevelResolver;
            this.giftDeduplicator = giftDeduplicator;
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
                    configSupplier, runtimeServices.chatGateway(), ruleEngine, memberLevelResolver,
                    emitters.renderedTracker(), emitters.messageFactory(),
                    emitters.giftEmitter(), emitters.giftComboAggregator(),
                    emitters.statsTracker(), emitters.moderationDuplicateTracker(),
                    runtimeServices.sessionEventLogger(), runtimeServices.alertService());
            TikTokEventDispatcher dispatcher = new TikTokEventDispatcher(deps, binding::isTokenCurrentLazy);
            return new TikTokFacadeWiring(
                    sessionState, emitters.statsTracker(), dispatcher, handlers.websocketDispatcher(), binding);
        }

        private Emitters buildEmitters() {
            RenderedCommentTracker renderedTracker = new RenderedCommentTracker();
            SessionStatsTracker statsTracker = new SessionStatsTracker();
            ModerationDuplicateTracker moderationDuplicateTracker = new ModerationDuplicateTracker();
            RichLiveMessageFactory messageFactory = new RichLiveMessageFactory(
                    new UnicodeEmojiParser(),
                    runtimeServices.languageSupplier(),
                    runtimeServices.runtimeLanguageSupplier());
            LiveCommentEmitter.Dependencies liveCommentDependencies = new LiveCommentEmitter.Dependencies(
                    configSupplier, runtimeServices.chatGateway(), ruleEngine, commentDeduplicator,
                    renderedTracker, messageFactory, statsTracker,
                    moderationDuplicateTracker, runtimeServices.sessionEventLogger());
            LiveCommentEmitter liveCommentEmitter = new LiveCommentEmitter(liveCommentDependencies);
            MemberLevelEmitter memberLevelEmitter = new MemberLevelEmitter(
                    configSupplier, runtimeServices, messageFactory, statsTracker);
            TikTokGiftEmitter giftEmitter = new TikTokGiftEmitter(
                    configSupplier, runtimeServices, ruleEngine, giftDeduplicator,
                    messageFactory, statsTracker);
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
                    configSupplier, runtimeServices.chatGateway(), sessionState,
                    executors.ioExecutor(), executors.scheduler(),
                    new NoticeThrottler(Duration.ofSeconds(ERROR_NOTICE_COOLDOWN_SECONDS)),
                    new NoticeThrottler(Duration.ofSeconds(RECONNECT_NOTICE_COOLDOWN_SECONDS)),
                    reset,
                    emitters.statsTracker()::reset,
                    runtimeServices.languageSupplier(),
                    runtimeServices.sessionEventLogger());
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

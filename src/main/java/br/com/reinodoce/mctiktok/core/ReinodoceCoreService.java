package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.state.RuntimeSettingsState;
import br.com.reinodoce.mctiktok.tiktok.MemberLevelResolver;
import br.com.reinodoce.mctiktok.tiktok.TikTokClientFacade;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import br.com.reinodoce.mctiktok.util.UsernameValidator;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Core command service that owns configuration, connection lifecycle, and operator-facing state.
 */
public class ReinodoceCoreService implements ReinodoceCommandService {
    private static final int DEDUPLICATION_WINDOW_MINUTES = 3;

    private final ReinodoceConfigRepository configRepository;
    private final RuntimeSettingsState settingsState;
    private final TikTokClientFacade tikTokClientFacade;
    private final AtomicBoolean initialized;

    /**
     * Creates the core service.
     *
     * @param chatEventSink chat sink used for rendered TikTok events
     * @param configRepository persisted configuration repository
     */
    public ReinodoceCoreService(ChatEventSink chatEventSink, ReinodoceConfigRepository configRepository) {
        this(chatEventSink, configRepository, () -> Locale.getDefault().toLanguageTag());
    }

    /**
     * Creates the core service.
     *
     * @param chatEventSink chat sink used for rendered TikTok events
     * @param configRepository persisted configuration repository
     * @param languageSupplier selected client language supplier
     */
    public ReinodoceCoreService(
            ChatEventSink chatEventSink,
            ReinodoceConfigRepository configRepository,
            Supplier<String> languageSupplier
    ) {
        this.configRepository = Objects.requireNonNull(configRepository, "configRepository");
        this.settingsState = new RuntimeSettingsState();
        MessageRuleEngine ruleEngine = new MessageRuleEngine();
        MemberLevelResolver memberLevelResolver = new MemberLevelResolver();
        MessageDeduplicator deduplicator = new MessageDeduplicator(Duration.ofMinutes(DEDUPLICATION_WINDOW_MINUTES));
        this.tikTokClientFacade = new TikTokClientFacade(
                settingsState::getSnapshot,
                Objects.requireNonNull(chatEventSink, "chatEventSink"),
                ruleEngine,
                memberLevelResolver,
                deduplicator,
                Objects.requireNonNull(languageSupplier, "languageSupplier")
        );
        this.initialized = new AtomicBoolean(false);
    }

    /**
     * Loads persisted configuration and applies it to runtime collaborators once.
     */
    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        ReinodoceConfig loaded = configRepository.load();
        settingsState.set(loaded);
        tikTokClientFacade.onConfigUpdated();
    }

    @Override
    public CommandResult connect(String username) {
        ensureInitialized();
        String normalized = UsernameValidator.normalize(username);
        CommandResult result = tikTokClientFacade.connect(normalized);
        if (result.success()) {
            ReinodoceConfig updated = settingsState.getSnapshot();
            updated.setLastUsername(normalized);
            persist(updated);
        }
        return result;
    }

    @Override
    public CommandResult disconnect() {
        ensureInitialized();
        return tikTokClientFacade.disconnect();
    }

    @Override
    public List<String> statusLines() {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        LiveSessionState.Snapshot snapshot = tikTokClientFacade.status();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
        String emptyValue = Translations.tr("reinodoce.status.empty_value");

        List<String> lines = new ArrayList<>();
        lines.add(Translations.tr("reinodoce.status.state", snapshot.state()));
        lines.add(Translations.tr("reinodoce.status.username",
                snapshot.username().isBlank() ? emptyValue : "@" + snapshot.username()));
        lines.add(Translations.tr("reinodoce.status.last_error",
                snapshot.lastError().isBlank() ? emptyValue : snapshot.lastError()));
        lines.add(Translations.tr("reinodoce.status.reconnect_seconds", config.getReconnectSeconds()));
        lines.add(Translations.tr("reinodoce.status.reconnect_attempts", snapshot.reconnectAttempts()));
        lines.add(Translations.tr("reinodoce.status.rule_follower", config.isRuleFollowerOnly()));
        lines.add(Translations.tr("reinodoce.status.rule_min_member_level", config.getRuleMinMemberLevel()));
        lines.add(Translations.tr("reinodoce.status.synthetic_gift", config.getSyntheticGiftMinValue()));
        lines.add(Translations.tr("reinodoce.status.synthetic_gift_combo", config.getSyntheticGiftComboMode()));
        lines.add(Translations.tr("reinodoce.status.synthetic_follow", config.isSyntheticFollowEnabled()));
        lines.add(Translations.tr("reinodoce.status.synthetic_join", config.isSyntheticJoinEnabled()));
        lines.add(Translations.tr("reinodoce.status.synthetic_member_level", config.isSyntheticMemberLevelEnabled()));
        lines.add(Translations.tr("reinodoce.status.chat_prefix", config.getChatPrefix()));
        lines.add(Translations.tr("reinodoce.status.chat_format", config.getChatFormat()));
        lines.add(Translations.tr("reinodoce.status.chat_emotes", config.isChatEmotesEnabled()));
        lines.add(Translations.tr("reinodoce.status.chat_log", config.isChatLogEnabled()));
        if (snapshot.reconnectAt() != null) {
            lines.add(Translations.tr("reinodoce.status.next_reconnect", formatter.format(snapshot.reconnectAt())));
        }
        return lines;
    }

    @Override
    public CommandResult setReconnectSeconds(int seconds) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setReconnectSeconds(seconds);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.set.reconnect", config.getReconnectSeconds()));
    }

    @Override
    public CommandResult setFollowerRule(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleFollowerOnly(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.follower", enabled));
    }

    @Override
    public CommandResult setMinMemberLevelRule(int level) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleMinMemberLevel(level);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.min_member_level", config.getRuleMinMemberLevel()));
    }

    @Override
    public CommandResult setSyntheticGift(int value) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSyntheticGiftMinValue(value);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.synthetic_gift", config.getSyntheticGiftMinValue()));
    }

    @Override
    public CommandResult setSyntheticGiftComboMode(String mode) {
        ensureInitialized();
        GiftComboMode parsed = GiftComboMode.fromString(mode);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSyntheticGiftComboMode(parsed.id());
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.synthetic_gift_combo", parsed.id()));
    }

    @Override
    public CommandResult setSyntheticFollow(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSyntheticFollowEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.synthetic_follow", enabled));
    }

    @Override
    public CommandResult setSyntheticJoin(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSyntheticJoinEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.synthetic_join", enabled));
    }

    @Override
    public CommandResult setSyntheticMemberLevel(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSyntheticMemberLevelEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.synthetic_member_level", enabled));
    }

    @Override
    public CommandResult setChatEmotesEnabled(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setChatEmotesEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.chat_emotes", enabled));
    }

    @Override
    public CommandResult setChatLogEnabled(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setChatLogEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.chat_log", enabled));
    }

    @Override
    public CommandResult setChatPrefix(String prefix) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setChatPrefix(prefix);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.chat_prefix", config.getChatPrefix()));
    }

    @Override
    public CommandResult setChatFormat(String format) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setChatFormat(format);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.chat_format", config.getChatFormat()));
    }

    @Override
    public CommandResult reload() {
        ensureInitialized();
        ReinodoceConfig loaded = configRepository.load();
        settingsState.set(loaded);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.reload.ok"));
    }

    private void ensureInitialized() {
        if (!initialized.get()) {
            initialize();
        }
    }

    private void persist(ReinodoceConfig config) {
        settingsState.set(config);
        configRepository.save(config);
    }
}

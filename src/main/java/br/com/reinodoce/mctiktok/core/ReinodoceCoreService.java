package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.alert.AlertSoundId;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import br.com.reinodoce.mctiktok.config.HudPosition;
import br.com.reinodoce.mctiktok.config.LanguageSetting;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.logging.SessionLogFormat;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.state.RuntimeSettingsState;
import br.com.reinodoce.mctiktok.tiktok.MemberLevelResolver;
import br.com.reinodoce.mctiktok.tiktok.SessionStatsTracker;
import br.com.reinodoce.mctiktok.tiktok.TikTokClientFacade;
import br.com.reinodoce.mctiktok.tiktok.TikTokRuntimeServices;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import br.com.reinodoce.mctiktok.util.UsernameValidator;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Core command service that owns configuration, connection lifecycle, and operator-facing state.
 */
// Command facade intentionally exposes one method per public command action.
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",
    "PMD.CyclomaticComplexity",
    "PMD.ExcessivePublicCount",
    "PMD.TooManyMethods"
})
public class ReinodoceCoreService implements ReinodoceCommandService {
    private static final int DEDUPLICATION_WINDOW_MINUTES = 3;
    private static final Path DEFAULT_SESSION_LOG_DIRECTORY = Path.of("logs", "reinodoce");
    private static final String DEFAULT_DISPLAY_VALUE = "default";

    private final ReinodoceConfigRepository configRepository;
    private final RuntimeSettingsState settingsState;
    private final TikTokClientFacade tikTokClientFacade;
    private final CoreDiagnosticsExporter diagnosticsExporter;
    private final Supplier<String> clientLanguageSupplier;
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
        this(chatEventSink, AlertSink.noop(), configRepository, languageSupplier, DEFAULT_SESSION_LOG_DIRECTORY);
    }

    /**
     * Creates the core service.
     *
     * @param chatEventSink chat sink used for rendered TikTok events
     * @param configRepository persisted configuration repository
     * @param languageSupplier selected client language supplier
     * @param sessionLogDirectory local session log directory
     */
    public ReinodoceCoreService(
            ChatEventSink chatEventSink,
            ReinodoceConfigRepository configRepository,
            Supplier<String> languageSupplier,
            Path sessionLogDirectory
    ) {
        this(chatEventSink, AlertSink.noop(), configRepository, languageSupplier, sessionLogDirectory);
    }

    /**
     * Creates the core service.
     *
     * @param chatEventSink chat sink used for rendered TikTok events
     * @param alertSink local alert sink
     * @param configRepository persisted configuration repository
     * @param languageSupplier selected client language supplier
     * @param sessionLogDirectory local session log directory
     */
    public ReinodoceCoreService(
            ChatEventSink chatEventSink,
            AlertSink alertSink,
            ReinodoceConfigRepository configRepository,
            Supplier<String> languageSupplier,
            Path sessionLogDirectory
    ) {
        this(
                new TikTokRuntimeServices.SideEffects(chatEventSink, alertSink, sessionLogDirectory),
                configRepository,
                languageSupplier);
    }

    /**
     * Creates the core service.
     *
     * @param runtimeSideEffects rendered event sinks and local side effects
     * @param configRepository persisted configuration repository
     * @param languageSupplier selected client language supplier
     */
    public ReinodoceCoreService(
            TikTokRuntimeServices.SideEffects runtimeSideEffects,
            ReinodoceConfigRepository configRepository,
            Supplier<String> languageSupplier
    ) {
        TikTokRuntimeServices.SideEffects sideEffects = Objects.requireNonNull(runtimeSideEffects, "runtimeSideEffects");
        this.configRepository = Objects.requireNonNull(configRepository, "configRepository");
        this.settingsState = new RuntimeSettingsState();
        this.clientLanguageSupplier = Objects.requireNonNull(languageSupplier, "languageSupplier");
        this.tikTokClientFacade = createTikTokClientFacade(sideEffects.withLanguageSuppliers(
                this::effectiveLanguageUnchecked,
                this::useRuntimeLanguageForEffectiveLanguage));
        this.diagnosticsExporter = new CoreDiagnosticsExporter(sideEffects.sessionLogDirectory());
        this.initialized = new AtomicBoolean(false);
    }

    ReinodoceCoreService(
            ReinodoceConfigRepository configRepository,
            Supplier<String> languageSupplier,
            TikTokClientFacade tikTokClientFacade
    ) {
        this.configRepository = Objects.requireNonNull(configRepository, "configRepository");
        this.settingsState = new RuntimeSettingsState();
        this.clientLanguageSupplier = Objects.requireNonNull(languageSupplier, "languageSupplier");
        this.tikTokClientFacade = Objects.requireNonNull(tikTokClientFacade, "tikTokClientFacade");
        this.diagnosticsExporter = new CoreDiagnosticsExporter(DEFAULT_SESSION_LOG_DIRECTORY);
        this.initialized = new AtomicBoolean(false);
    }

    private TikTokClientFacade createTikTokClientFacade(TikTokRuntimeServices runtimeServices) {
        MessageRuleEngine ruleEngine = new MessageRuleEngine();
        MemberLevelResolver memberLevelResolver = new MemberLevelResolver();
        MessageDeduplicator deduplicator = new MessageDeduplicator(Duration.ofMinutes(DEDUPLICATION_WINDOW_MINUTES));
        return new TikTokClientFacade(
                settingsState::getSnapshot,
                runtimeServices,
                ruleEngine,
                memberLevelResolver,
                deduplicator
        );
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
        autoConnectIfConfigured(loaded);
    }

    /**
     * Returns the current runtime configuration snapshot.
     *
     * @return current configuration
     */
    public ReinodoceConfig currentConfig() {
        ensureInitialized();
        return settingsState.getSnapshot();
    }

    /**
     * Replaces configuration from a validated client settings GUI draft.
     *
     * @param config updated configuration
     * @return command result
     */
    public CommandResult replaceConfig(ReinodoceConfig config) {
        ensureInitialized();
        ReinodoceConfig updated = Objects.requireNonNull(config, "config");
        String previousEffectiveLanguage = effectiveLanguage(settingsState.getSnapshot());
        persist(updated);
        tikTokClientFacade.onConfigUpdated();
        reconnectIfLanguageChanged(previousEffectiveLanguage, updated);
        return CommandResult.ok(Translations.tr("reinodoce.command.settings_gui.saved"));
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
    public CommandResult connectLast() {
        ensureInitialized();
        String username = UsernameValidator.normalize(settingsState.getSnapshot().getLastUsername());
        if (username.isBlank()) {
            return CommandResult.error(Translations.tr("reinodoce.command.connect.missing_saved"));
        }
        if (!UsernameValidator.isValid(username)) {
            return CommandResult.error(Translations.tr("reinodoce.error.username_invalid"));
        }
        return connect(username);
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
        lines.add(Translations.tr("reinodoce.status.auto_connect", config.isAutoConnectOnStart()));
        addOutputStatusLines(lines, config);
        lines.add(Translations.tr("reinodoce.status.reconnect_attempts", snapshot.reconnectAttempts()));
        lines.add(Translations.tr("reinodoce.status.rule_follower", config.isRuleFollowerOnly()));
        lines.add(Translations.tr("reinodoce.status.rule_min_member_level", config.getRuleMinMemberLevel()));
        lines.add(Translations.tr("reinodoce.status.rule_blocked_words", config.getRuleBlockedWords().size()));
        lines.add(Translations.tr("reinodoce.status.rule_blocked_users", config.getRuleBlockedUsers().size()));
        lines.add(Translations.tr("reinodoce.status.rule_max_message_length", config.getRuleMaxMessageLength()));
        lines.add(Translations.tr("reinodoce.status.rule_duplicate_cooldown",
                config.getRuleDuplicateCooldownSeconds()));
        lines.add(Translations.tr("reinodoce.status.synthetic_gift", config.getSyntheticGiftMinValue()));
        lines.add(Translations.tr("reinodoce.status.synthetic_gift_combo", config.getSyntheticGiftComboMode()));
        lines.add(Translations.tr("reinodoce.status.synthetic_follow", config.isSyntheticFollowEnabled()));
        lines.add(Translations.tr("reinodoce.status.synthetic_join", config.isSyntheticJoinEnabled()));
        lines.add(Translations.tr("reinodoce.status.synthetic_member_level", config.isSyntheticMemberLevelEnabled()));
        addAlertStatusLines(lines, config);
        lines.add(Translations.tr("reinodoce.status.chat_prefix", config.getChatPrefix()));
        lines.add(Translations.tr("reinodoce.status.chat_format", config.getChatFormat()));
        lines.add(Translations.tr("reinodoce.status.chat_emotes", config.isChatEmotesEnabled()));
        lines.add(Translations.tr("reinodoce.status.chat_log", config.isChatLogEnabled()));
        lines.add(Translations.tr("reinodoce.status.mask_usernames", config.isMaskUsernamesInOutput()));
        lines.add(Translations.tr("reinodoce.status.session_logging", config.isSessionLoggingEnabled()));
        lines.add(Translations.tr("reinodoce.status.session_logging_format", config.getSessionLoggingFormat()));
        lines.add(Translations.tr("reinodoce.status.session_logging_retention",
                config.getSessionLoggingRetentionDays(), config.getSessionLoggingRetentionFiles()));
        lines.add(Translations.tr("reinodoce.status.session_logging_privacy",
                config.isSessionLoggingAnonymized(), config.isSessionLoggingMetadataOnly()));
        SessionStatsTracker.Snapshot stats = tikTokClientFacade.stats();
        lines.add(Translations.tr("reinodoce.status.session_stats",
                stats.messages(), stats.uniqueChatters(), stats.gifts(), stats.diamonds()));
        if (snapshot.reconnectAt() != null) {
            lines.add(Translations.tr("reinodoce.status.next_reconnect", formatter.format(snapshot.reconnectAt())));
        }
        return lines;
    }

    @Override
    public List<String> statsLines() {
        ensureInitialized();
        SessionStatsTracker.Snapshot stats = tikTokClientFacade.stats();
        String emptyValue = Translations.tr("reinodoce.status.empty_value");
        String topGifter = stats.topGifter().isBlank()
                ? emptyValue
                : Translations.tr("reinodoce.stats.top_gifter_value",
                        stats.topGifter(), stats.topGifterDiamonds());

        List<String> lines = new ArrayList<>();
        lines.add(Translations.tr("reinodoce.stats.title"));
        lines.add(Translations.tr("reinodoce.stats.messages", stats.messages()));
        lines.add(Translations.tr("reinodoce.stats.unique_chatters", stats.uniqueChatters()));
        lines.add(Translations.tr("reinodoce.stats.follows", stats.follows()));
        lines.add(Translations.tr("reinodoce.stats.joins", stats.joins()));
        lines.add(Translations.tr("reinodoce.stats.gifts", stats.gifts()));
        lines.add(Translations.tr("reinodoce.stats.diamonds", stats.diamonds()));
        lines.add(Translations.tr("reinodoce.stats.member_levels", stats.memberLevels()));
        lines.add(Translations.tr("reinodoce.stats.top_gifter", topGifter));
        return lines;
    }

    @Override
    public CommandResult resetStats() {
        ensureInitialized();
        tikTokClientFacade.resetStats();
        return CommandResult.ok(Translations.tr("reinodoce.command.stats.reset"));
    }

    @Override
    public CommandResult exportDiagnostics() {
        return exportDiagnostics(Map.of());
    }

    /**
     * Writes a sanitized support diagnostics report with client-side renderer diagnostics.
     *
     * @param clientDiagnostics client-side renderer diagnostics
     * @return command result
     */
    public CommandResult exportDiagnostics(Map<String, Object> clientDiagnostics) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        return diagnosticsExporter.export(
                config,
                tikTokClientFacade,
                effectiveLanguage(config),
                configRepository.configPath(),
                clientDiagnostics);
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
    public CommandResult setAutoConnectOnStart(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAutoConnectOnStart(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.auto_connect", enabled));
    }

    @Override
    public CommandResult setOutputMode(String mode) {
        ensureInitialized();
        OutputMode parsed = OutputMode.fromString(mode);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setOutputMode(parsed.id());
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.output_mode", parsed.id()));
    }

    @Override
    public CommandResult setHudPosition(String position) {
        ensureInitialized();
        HudPosition parsed = HudPosition.fromString(position);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setHudPosition(parsed.id());
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.hud_position", parsed.id()));
    }

    @Override
    public CommandResult setHudLines(int lines) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setHudLines(lines);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.hud_lines", config.getHudLines()));
    }

    @Override
    public CommandResult setPinnedOverlayEnabled(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setPinnedOverlayEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.pinned_overlay", enabled));
    }

    @Override
    public CommandResult setPinnedOverlayPosition(String position) {
        ensureInitialized();
        HudPosition parsed = HudPosition.fromString(position);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setPinnedOverlayPosition(parsed.id());
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.pinned_overlay_position", parsed.id()));
    }

    @Override
    public CommandResult setPinnedMessagesInOutput(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setPinnedMessagesInOutput(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.pinned_output", enabled));
    }

    @Override
    public CommandResult setPinnedOverlayMessages(int messages) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setPinnedOverlayMessages(messages);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.pinned_overlay_messages",
                config.getPinnedOverlayMessages()));
    }

    @Override
    public List<String> languageLines() {
        ensureInitialized();
        return List.of(languageStatusLine(settingsState.getSnapshot()));
    }

    @Override
    public CommandResult setLanguage(String language) {
        ensureInitialized();
        String normalized = LanguageSetting.parse(language).orElse("");
        if (normalized.isBlank()) {
            return CommandResult.error(Translations.tr(
                    "reinodoce.command.language.invalid",
                    language == null ? "" : language));
        }
        String previousEffectiveLanguage = effectiveLanguage(settingsState.getSnapshot());
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setLanguage(normalized);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        reconnectIfLanguageChanged(previousEffectiveLanguage, config);
        return CommandResult.ok(languageSetLine(config));
    }

    @Override
    public CommandResult openSettingsGui() {
        ensureInitialized();
        return CommandResult.error(Translations.tr("reinodoce.command.settings_gui.client_only"));
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
    public CommandResult addBlockedWord(String word) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        String normalized = config.addRuleBlockedWord(word);
        if (normalized.isBlank()) {
            return CommandResult.error(Translations.tr("reinodoce.command.rule.block_word.invalid"));
        }
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.rule.block_word.add", normalized));
    }

    @Override
    public CommandResult removeBlockedWord(String word) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        String normalized = config.removeRuleBlockedWord(word);
        if (normalized.isBlank()) {
            return CommandResult.error(Translations.tr("reinodoce.command.rule.block_word.invalid"));
        }
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.rule.block_word.remove", normalized));
    }

    @Override
    public List<String> blockedWordLines() {
        ensureInitialized();
        return listLines(
                Translations.tr("reinodoce.command.rule.block_word.list"),
                settingsState.getSnapshot().getRuleBlockedWords());
    }

    @Override
    public CommandResult addBlockedUser(String username) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        String normalized = config.addRuleBlockedUser(username);
        if (normalized.isBlank()) {
            return CommandResult.error(Translations.tr("reinodoce.error.username_invalid"));
        }
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.rule.block_user.add", normalized));
    }

    @Override
    public CommandResult removeBlockedUser(String username) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        String normalized = config.removeRuleBlockedUser(username);
        if (normalized.isBlank()) {
            return CommandResult.error(Translations.tr("reinodoce.error.username_invalid"));
        }
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.rule.block_user.remove", normalized));
    }

    @Override
    public List<String> blockedUserLines() {
        ensureInitialized();
        return listLines(
                Translations.tr("reinodoce.command.rule.block_user.list"),
                settingsState.getSnapshot().getRuleBlockedUsers());
    }

    @Override
    public CommandResult setMaxMessageLengthRule(int length) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleMaxMessageLength(length);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.max_message_length", config.getRuleMaxMessageLength()));
    }

    @Override
    public CommandResult setDuplicateCooldownRule(int seconds) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleDuplicateCooldownSeconds(seconds);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.duplicate_cooldown", config.getRuleDuplicateCooldownSeconds()));
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
    public CommandResult setAlertSound(AlertEventType eventType, boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertSoundEnabled(eventType, enabled);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_sound", eventType.id(), enabled));
    }

    @Override
    public CommandResult setAlertSoundId(AlertEventType eventType, String soundId) {
        ensureInitialized();
        String normalized = AlertSoundId.normalize(soundId);
        if (!AlertSoundId.isValid(normalized)) {
            return CommandResult.error(Translations.tr(
                    "reinodoce.command.alert_sound_id.invalid",
                    soundId == null ? "" : soundId));
        }
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertSoundId(eventType, normalized);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_sound_id", eventType.id(), config.getAlertSoundId(eventType)));
    }

    @Override
    public CommandResult setAlertToast(AlertEventType eventType, boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertToastEnabled(eventType, enabled);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_toast", eventType.id(), enabled));
    }

    @Override
    public CommandResult setAlertToastTemplate(AlertEventType eventType, String template) {
        ensureInitialized();
        String normalized = template == null ? "" : template.trim();
        if (DEFAULT_DISPLAY_VALUE.equalsIgnoreCase(normalized)) {
            normalized = ReinodoceConfig.DEFAULT_ALERT_TOAST_TEMPLATE;
        } else if (!ReinodoceConfig.isValidAlertToastTemplate(normalized)) {
            return CommandResult.error(Translations.tr(
                    "reinodoce.command.alert_template.invalid",
                    template == null ? "" : template));
        }
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertToastTemplate(eventType, normalized);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_template",
                eventType.id(),
                displayDefault(config.getAlertToastTemplate(eventType))));
    }

    @Override
    public CommandResult setAlertMediaMode(AlertEventType eventType, String mediaMode) {
        ensureInitialized();
        String parsed = ReinodoceConfig.parseAlertMediaModeId(mediaMode).orElse("");
        if (parsed.isBlank()) {
            return CommandResult.error(Translations.tr(
                    "reinodoce.command.alert_media_mode.invalid",
                    mediaMode == null ? "" : mediaMode));
        }
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertMediaMode(eventType, parsed);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_media_mode", eventType.id(), config.getAlertMediaMode(eventType)));
    }

    @Override
    public CommandResult setAlertCustomImage(AlertEventType eventType, String customImage) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertCustomImage(eventType, customImage);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_custom_image",
                eventType.id(),
                displayDefault(config.getAlertCustomImage(eventType))));
    }

    @Override
    public CommandResult setAlertGiftMinValue(int value) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setAlertGiftMinValue(value);
        persist(config);
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.alert_gift_min_value", config.getAlertGiftMinValue()));
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
    public CommandResult setMaskUsernamesInOutput(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setMaskUsernamesInOutput(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.mask_usernames", enabled));
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
    public CommandResult setSessionLoggingEnabled(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSessionLoggingEnabled(enabled);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.set.session_logging", enabled));
    }

    @Override
    public CommandResult setSessionLoggingFormat(String format) {
        ensureInitialized();
        SessionLogFormat parsed = SessionLogFormat.fromString(format);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSessionLoggingFormat(parsed.id());
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.set.session_logging_format", parsed.id()));
    }

    @Override
    public CommandResult setSessionLoggingRetentionDays(int days) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSessionLoggingRetentionDays(days);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.session_logging_retention_days",
                config.getSessionLoggingRetentionDays()));
    }

    @Override
    public CommandResult setSessionLoggingRetentionFiles(int files) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSessionLoggingRetentionFiles(files);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr(
                "reinodoce.command.set.session_logging_retention_files",
                config.getSessionLoggingRetentionFiles()));
    }

    @Override
    public CommandResult setSessionLoggingAnonymized(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSessionLoggingAnonymized(enabled);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.set.session_logging_anonymized", enabled));
    }

    @Override
    public CommandResult setSessionLoggingMetadataOnly(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSessionLoggingMetadataOnly(enabled);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.set.session_logging_metadata_only", enabled));
    }

    @Override
    public CommandResult reload() {
        ensureInitialized();
        String previousEffectiveLanguage = effectiveLanguage(settingsState.getSnapshot());
        ReinodoceConfig loaded = configRepository.load();
        settingsState.set(loaded);
        tikTokClientFacade.onConfigUpdated();
        reconnectIfLanguageChanged(previousEffectiveLanguage, loaded);
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

    private String effectiveLanguageUnchecked() {
        return effectiveLanguage(settingsState.getSnapshot());
    }

    private String effectiveLanguage(ReinodoceConfig config) {
        return LanguageSetting.resolveEffective(config.getLanguage(), clientLanguageSupplier.get());
    }

    private boolean useRuntimeLanguageForEffectiveLanguage() {
        ReinodoceConfig config = settingsState.getSnapshot();
        return LanguageSetting.isAuto(config.getLanguage())
                || LanguageSetting.normalizeLocale(clientLanguageSupplier.get())
                .filter(config.getLanguage()::equals)
                .isPresent();
    }

    private String languageStatusLine(ReinodoceConfig config) {
        String effectiveLanguage = effectiveLanguage(config);
        if (LanguageSetting.isAuto(config.getLanguage())) {
            return Translations.tr("reinodoce.command.language.auto", effectiveLanguage);
        }
        return Translations.tr("reinodoce.command.language.override", config.getLanguage(), effectiveLanguage);
    }

    private String languageSetLine(ReinodoceConfig config) {
        String effectiveLanguage = effectiveLanguage(config);
        if (LanguageSetting.isAuto(config.getLanguage())) {
            return Translations.tr("reinodoce.command.language.set_auto", effectiveLanguage);
        }
        return Translations.tr("reinodoce.command.language.set_override", config.getLanguage(), effectiveLanguage);
    }

    private void reconnectIfLanguageChanged(String previousEffectiveLanguage, ReinodoceConfig config) {
        if (!previousEffectiveLanguage.equals(effectiveLanguage(config))) {
            tikTokClientFacade.reconnectForConfigChange();
        }
    }

    private void autoConnectIfConfigured(ReinodoceConfig config) {
        if (config.isAutoConnectOnStart()) {
            autoConnectSavedUsername(UsernameValidator.normalize(config.getLastUsername()));
        }
    }

    private void autoConnectSavedUsername(String username) {
        if (username.isBlank()) {
            return;
        }
        if (!UsernameValidator.isValid(username)) {
            tikTokClientFacade.recordLocalError(Translations.tr(
                    "reinodoce.command.connect.auto_skipped_invalid", username));
            return;
        }
        CommandResult result = connect(username);
        if (!result.success()) {
            tikTokClientFacade.recordLocalError(result.message());
        }
    }

    private static List<String> listLines(String title, List<String> values) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        if (values.isEmpty()) {
            lines.add(Translations.tr("reinodoce.status.empty_value"));
        } else {
            lines.addAll(values);
        }
        return lines;
    }

    private static String displayDefault(String value) {
        return value == null || value.isBlank() ? DEFAULT_DISPLAY_VALUE : value;
    }

    private void addOutputStatusLines(List<String> lines, ReinodoceConfig config) {
        lines.add(Translations.tr("reinodoce.status.language", config.getLanguage(), effectiveLanguage(config)));
        lines.add(Translations.tr("reinodoce.status.output_mode", config.getOutputMode()));
        lines.add(Translations.tr("reinodoce.status.hud_position", config.getHudPosition()));
        lines.add(Translations.tr("reinodoce.status.hud_lines", config.getHudLines()));
        lines.add(Translations.tr(
                "reinodoce.status.pinned_overlay",
                config.isPinnedOverlayEnabled(),
                config.getPinnedOverlayPosition(),
                config.getPinnedOverlayMessages(),
                config.isPinnedMessagesInOutput()));
    }

    private static void addAlertStatusLines(List<String> lines, ReinodoceConfig config) {
        lines.add(Translations.tr(
                "reinodoce.status.alert_gift",
                config.isAlertSoundEnabled(AlertEventType.GIFT),
                config.getAlertSoundId(AlertEventType.GIFT),
                config.isAlertToastEnabled(AlertEventType.GIFT),
                config.getAlertMediaMode(AlertEventType.GIFT),
                displayDefault(config.getAlertToastTemplate(AlertEventType.GIFT)),
                displayDefault(config.getAlertCustomImage(AlertEventType.GIFT)),
                config.getAlertGiftMinValue()));
        lines.add(alertStatusLine("reinodoce.status.alert_follow", config, AlertEventType.FOLLOW));
        lines.add(alertStatusLine("reinodoce.status.alert_join", config, AlertEventType.JOIN));
        lines.add(alertStatusLine("reinodoce.status.alert_member_level", config, AlertEventType.MEMBER_LEVEL));
    }

    private static String alertStatusLine(String translationKey, ReinodoceConfig config, AlertEventType eventType) {
        return Translations.tr(
                translationKey,
                config.isAlertSoundEnabled(eventType),
                config.getAlertSoundId(eventType),
                config.isAlertToastEnabled(eventType),
                config.getAlertMediaMode(eventType),
                displayDefault(config.getAlertToastTemplate(eventType)),
                displayDefault(config.getAlertCustomImage(eventType)));
    }
}

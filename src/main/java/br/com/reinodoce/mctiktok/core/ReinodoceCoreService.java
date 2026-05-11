package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReinodoceCoreService {
    private final ReinodoceConfigRepository configRepository;
    private final RuntimeSettingsState settingsState;
    private final TikTokClientFacade tikTokClientFacade;
    private final AtomicBoolean initialized;

    public ReinodoceCoreService(ChatEventSink chatEventSink, ReinodoceConfigRepository configRepository) {
        this.configRepository = Objects.requireNonNull(configRepository, "configRepository");
        this.settingsState = new RuntimeSettingsState();
        MessageRuleEngine ruleEngine = new MessageRuleEngine();
        MemberLevelResolver memberLevelResolver = new MemberLevelResolver();
        MessageDeduplicator deduplicator = new MessageDeduplicator(Duration.ofMinutes(3));
        this.tikTokClientFacade = new TikTokClientFacade(
                settingsState::getSnapshot,
                Objects.requireNonNull(chatEventSink, "chatEventSink"),
                ruleEngine,
                memberLevelResolver,
                deduplicator
        );
        this.initialized = new AtomicBoolean(false);
    }

    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        ReinodoceConfig loaded = configRepository.load();
        settingsState.set(loaded);
        tikTokClientFacade.onConfigUpdated();
    }

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

    public CommandResult disconnect() {
        ensureInitialized();
        return tikTokClientFacade.disconnect();
    }

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
        lines.add(Translations.tr("reinodoce.status.syntetic_gift", config.getSynteticGiftMinValue()));
        lines.add(Translations.tr("reinodoce.status.syntetic_gift_combo", config.getSynteticGiftComboMode()));
        lines.add(Translations.tr("reinodoce.status.syntetic_follow", config.isSynteticFollowEnabled()));
        lines.add(Translations.tr("reinodoce.status.syntetic_join", config.isSynteticJoinEnabled()));
        lines.add(Translations.tr("reinodoce.status.syntetic_member_level", config.isSynteticMemberLevelEnabled()));
        lines.add(Translations.tr("reinodoce.status.chat_emotes", config.isChatEmotesEnabled()));
        if (snapshot.reconnectAt() != null) {
            lines.add(Translations.tr("reinodoce.status.next_reconnect", formatter.format(snapshot.reconnectAt())));
        }
        return lines;
    }

    public CommandResult setReconnectSeconds(int seconds) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setReconnectSeconds(seconds);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok(Translations.tr("reinodoce.command.set.reconnect", config.getReconnectSeconds()));
    }

    public CommandResult setFollowerRule(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleFollowerOnly(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.follower", enabled));
    }

    public CommandResult setMinMemberLevelRule(int level) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleMinMemberLevel(level);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.min_member_level", config.getRuleMinMemberLevel()));
    }

    public CommandResult setSynteticGift(int value) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticGiftMinValue(value);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.syntetic_gift", config.getSynteticGiftMinValue()));
    }

    public CommandResult setSynteticGiftComboMode(String mode) {
        ensureInitialized();
        GiftComboMode parsed = GiftComboMode.fromString(mode);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticGiftComboMode(parsed.id());
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.syntetic_gift_combo", parsed.id()));
    }

    public CommandResult setSynteticFollow(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticFollowEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.syntetic_follow", enabled));
    }

    public CommandResult setSynteticJoin(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticJoinEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.syntetic_join", enabled));
    }

    public CommandResult setSynteticMemberLevel(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticMemberLevelEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.syntetic_member_level", enabled));
    }

    public CommandResult setChatEmotesEnabled(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setChatEmotesEnabled(enabled);
        persist(config);
        return CommandResult.ok(Translations.tr("reinodoce.command.set.chat_emotes", enabled));
    }

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

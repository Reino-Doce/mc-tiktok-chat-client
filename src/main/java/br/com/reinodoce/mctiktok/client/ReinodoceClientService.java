package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.chat.LiveMessageFormatter;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class ReinodoceClientService {
    private final ReinodoceConfigRepository configRepository;
    private final RuntimeSettingsState settingsState;
    private final TikTokClientFacade tikTokClientFacade;
    private final AtomicBoolean initialized;

    public ReinodoceClientService(MinecraftPlatformBridge platformBridge) {
        this.configRepository = new ReinodoceConfigRepository();
        this.settingsState = new RuntimeSettingsState();
        MessageRuleEngine ruleEngine = new MessageRuleEngine();
        MemberLevelResolver memberLevelResolver = new MemberLevelResolver();
        MessageDeduplicator deduplicator = new MessageDeduplicator(Duration.ofMinutes(3));
        MinecraftChatGateway chatGateway = new MinecraftChatGateway(platformBridge, new LiveMessageFormatter());
        this.tikTokClientFacade = new TikTokClientFacade(settingsState::getSnapshot, chatGateway, ruleEngine, memberLevelResolver, deduplicator);
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

        List<String> lines = new ArrayList<>();
        lines.add("Estado: " + snapshot.state());
        lines.add("Username atual: " + (snapshot.username().isBlank() ? "-" : "@" + snapshot.username()));
        lines.add("Ultimo erro: " + (snapshot.lastError().isBlank() ? "-" : snapshot.lastError()));
        lines.add("Reconnect: " + config.getReconnectSeconds() + "s");
        lines.add("Rule follower: " + config.isRuleFollowerOnly());
        lines.add("Rule min-member-level: " + config.getRuleMinMemberLevel());
        lines.add("Syntetic gift: " + config.getSynteticGiftMinValue());
        lines.add("Syntetic gift-combo: " + config.getSynteticGiftComboMode());
        lines.add("Syntetic follow: " + config.isSynteticFollowEnabled());
        lines.add("Syntetic join: " + config.isSynteticJoinEnabled());
        lines.add("Syntetic member-level: " + config.isSynteticMemberLevelEnabled());
        if (snapshot.reconnectAt() != null) {
            lines.add("Proximo reconnect: " + formatter.format(snapshot.reconnectAt()));
        }
        return lines;
    }

    public CommandResult setReconnectSeconds(int seconds) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setReconnectSeconds(seconds);
        persist(config);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok("Reconnect atualizado para " + config.getReconnectSeconds() + "s.");
    }

    public CommandResult setFollowerRule(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleFollowerOnly(enabled);
        persist(config);
        return CommandResult.ok("Rule follower = " + enabled + ".");
    }

    public CommandResult setMinMemberLevelRule(int level) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setRuleMinMemberLevel(level);
        persist(config);
        return CommandResult.ok("Rule min-member-level = " + config.getRuleMinMemberLevel() + ".");
    }

    public CommandResult setSynteticGift(int value) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticGiftMinValue(value);
        persist(config);
        return CommandResult.ok("Syntetic gift = " + config.getSynteticGiftMinValue() + ".");
    }

    public CommandResult setSynteticGiftComboMode(String mode) {
        ensureInitialized();
        GiftComboMode parsed = GiftComboMode.fromString(mode);
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticGiftComboMode(parsed.id());
        persist(config);
        return CommandResult.ok("Syntetic gift-combo = " + parsed.id() + ".");
    }

    public CommandResult setSynteticFollow(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticFollowEnabled(enabled);
        persist(config);
        return CommandResult.ok("Syntetic follow = " + enabled + ".");
    }

    public CommandResult setSynteticJoin(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticJoinEnabled(enabled);
        persist(config);
        return CommandResult.ok("Syntetic join = " + enabled + ".");
    }

    public CommandResult setSynteticMemberLevel(boolean enabled) {
        ensureInitialized();
        ReinodoceConfig config = settingsState.getSnapshot();
        config.setSynteticMemberLevelEnabled(enabled);
        persist(config);
        return CommandResult.ok("Syntetic member-level = " + enabled + ".");
    }

    public CommandResult reload() {
        ensureInitialized();
        ReinodoceConfig loaded = configRepository.load();
        settingsState.set(loaded);
        tikTokClientFacade.onConfigUpdated();
        return CommandResult.ok("Configuracao recarregada.");
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

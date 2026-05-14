package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.MinecraftAlertGateway;
import br.com.reinodoce.mctiktok.chat.LiveMessageFormatter;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.client.font.InlineMediaFontHooks;
import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.gui.ReinodoceSettingsScreen;
import br.com.reinodoce.mctiktok.client.hud.HudMessageStore;
import br.com.reinodoce.mctiktok.client.hud.LocalHudOverlay;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import br.com.reinodoce.mctiktok.config.HudPosition;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.core.ReinodoceCoreService;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Client service that composes core TikTok logic with Minecraft inline media rendering.
 */
// Command-service facade intentionally mirrors the public command contract.
@SuppressWarnings("PMD.ExcessivePublicCount")
public class ReinodoceClientService implements ReinodoceCommandService {
    private final ReinodoceCoreService coreService;
    private final InlineMediaCache inlineMediaCache;
    private final InlineMediaTokenRegistry inlineMediaTokenRegistry;
    private final HudMessageStore hudMessageStore;
    private final LocalHudOverlay localHudOverlay;
    private final MinecraftPlatformBridge platformBridge;

    /**
     * Creates a client service for the supplied Minecraft platform adapter.
     *
     * @param platformBridge Minecraft platform adapter
     */
    public ReinodoceClientService(MinecraftPlatformBridge platformBridge) {
        MinecraftPlatformBridge safePlatformBridge = Objects.requireNonNull(platformBridge, "platformBridge");
        this.platformBridge = safePlatformBridge;
        this.inlineMediaCache = new InlineMediaCache();
        this.inlineMediaTokenRegistry = new InlineMediaTokenRegistry(inlineMediaCache);
        this.hudMessageStore = new HudMessageStore();
        this.localHudOverlay = new LocalHudOverlay(hudMessageStore);
        InlineMediaFontHooks.installRegistry(inlineMediaTokenRegistry);
        this.coreService = new ReinodoceCoreService(
                new MinecraftChatGateway(
                        safePlatformBridge,
                        new LiveMessageFormatter(inlineMediaTokenRegistry),
                        inlineMediaCache,
                        hudMessageStore
                ),
                new MinecraftAlertGateway(safePlatformBridge),
                new ReinodoceConfigRepository(),
                safePlatformBridge::selectedLanguageCode,
                safePlatformBridge.logsDirectory().resolve("reinodoce")
        );
    }

    ReinodoceClientService(
            MinecraftPlatformBridge platformBridge,
            ReinodoceCoreService coreService,
            HudMessageStore hudMessageStore
    ) {
        this.platformBridge = Objects.requireNonNull(platformBridge, "platformBridge");
        this.inlineMediaCache = new InlineMediaCache();
        this.inlineMediaTokenRegistry = new InlineMediaTokenRegistry(inlineMediaCache);
        this.hudMessageStore = Objects.requireNonNull(hudMessageStore, "hudMessageStore");
        this.localHudOverlay = new LocalHudOverlay(this.hudMessageStore);
        this.coreService = Objects.requireNonNull(coreService, "coreService");
    }

    /**
     * Initializes core service state.
     */
    public void initialize() {
        coreService.initialize();
    }

    /**
     * Renders the local HUD output overlay.
     *
     * @param graphics GUI graphics context
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     */
    public void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight) {
        ReinodoceConfig config = coreService.currentConfig();
        if (OutputMode.fromString(config.getOutputMode()) != OutputMode.HUD) {
            return;
        }
        localHudOverlay.render(
                graphics,
                screenWidth,
                screenHeight,
                HudPosition.fromString(config.getHudPosition()),
                config.getHudLines());
    }

    /**
     * Saves a complete settings GUI draft through the core config path.
     *
     * @param config settings draft
     * @return command result
     */
    public CommandResult saveSettingsDraft(ReinodoceConfig config) {
        CommandResult result = coreService.replaceConfig(config);
        if (result.success()) {
            clearHudMessagesIfOutputHidden(config.getOutputMode());
        }
        return result;
    }

    /**
     * Returns the current client settings snapshot for config-screen entry points.
     *
     * @return current configuration
     */
    public ReinodoceConfig currentConfig() {
        return coreService.currentConfig();
    }

    @Override
    public CommandResult connect(String username) {
        return coreService.connect(username);
    }

    @Override
    public CommandResult connectLast() {
        return coreService.connectLast();
    }

    @Override
    public CommandResult disconnect() {
        return coreService.disconnect();
    }

    @Override
    public List<String> statusLines() {
        List<String> lines = new ArrayList<>(coreService.statusLines());
        lines.add("Inline media renderer: font-coremod");
        lines.add("Coremod loaded: " + InlineMediaFontHooks.coremodLoaded());
        lines.add("Token registry: " + inlineMediaTokenRegistry.size());
        InlineMediaCache.Snapshot mediaSnapshot = inlineMediaCache.snapshot();
        lines.add("Media cache: resident=" + mediaSnapshot.resident()
                + " ready=" + mediaSnapshot.ready()
                + " loading=" + mediaSnapshot.loading()
                + " error=" + mediaSnapshot.error());
        lines.add("Downloads: started=" + mediaSnapshot.downloadsStarted()
                + " success=" + mediaSnapshot.downloadsSucceeded()
                + " fail=" + mediaSnapshot.downloadsFailed()
                + " disk=" + mediaSnapshot.diskHits()
                + " diskReloads=" + mediaSnapshot.diskReloads()
                + " memory=" + mediaSnapshot.memoryHits());
        lines.add("Cache evictions: ttl=" + mediaSnapshot.ttlEvictions() + " capacity=" + mediaSnapshot.capacityEvictions());
        return lines;
    }

    @Override
    public List<String> statsLines() {
        return coreService.statsLines();
    }

    @Override
    public CommandResult resetStats() {
        return coreService.resetStats();
    }

    @Override
    public CommandResult setReconnectSeconds(int seconds) {
        return coreService.setReconnectSeconds(seconds);
    }

    @Override
    public CommandResult setAutoConnectOnStart(boolean enabled) {
        return coreService.setAutoConnectOnStart(enabled);
    }

    @Override
    public CommandResult setOutputMode(String mode) {
        CommandResult result = coreService.setOutputMode(mode);
        if (result.success()) {
            clearHudMessagesIfOutputHidden(mode);
        }
        return result;
    }

    @Override
    public CommandResult setHudPosition(String position) {
        return coreService.setHudPosition(position);
    }

    @Override
    public CommandResult setHudLines(int lines) {
        return coreService.setHudLines(lines);
    }

    @Override
    public List<String> languageLines() {
        return coreService.languageLines();
    }

    @Override
    public CommandResult setLanguage(String language) {
        return coreService.setLanguage(language);
    }

    @Override
    public CommandResult openSettingsGui() {
        platformBridge.runOnClientThread(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ReinodoceSettingsScreen(this, currentConfig(), minecraft.screen));
        });
        return CommandResult.ok(Translations.tr("reinodoce.command.settings_gui.open"));
    }

    @Override
    public CommandResult setFollowerRule(boolean enabled) {
        return coreService.setFollowerRule(enabled);
    }

    @Override
    public CommandResult setMinMemberLevelRule(int level) {
        return coreService.setMinMemberLevelRule(level);
    }

    @Override
    public CommandResult addBlockedWord(String word) {
        return coreService.addBlockedWord(word);
    }

    @Override
    public CommandResult removeBlockedWord(String word) {
        return coreService.removeBlockedWord(word);
    }

    @Override
    public List<String> blockedWordLines() {
        return coreService.blockedWordLines();
    }

    @Override
    public CommandResult addBlockedUser(String username) {
        return coreService.addBlockedUser(username);
    }

    @Override
    public CommandResult removeBlockedUser(String username) {
        return coreService.removeBlockedUser(username);
    }

    @Override
    public List<String> blockedUserLines() {
        return coreService.blockedUserLines();
    }

    @Override
    public CommandResult setMaxMessageLengthRule(int length) {
        return coreService.setMaxMessageLengthRule(length);
    }

    @Override
    public CommandResult setDuplicateCooldownRule(int seconds) {
        return coreService.setDuplicateCooldownRule(seconds);
    }

    @Override
    public CommandResult setSyntheticGift(int value) {
        return coreService.setSyntheticGift(value);
    }

    @Override
    public CommandResult setSyntheticGiftComboMode(String mode) {
        return coreService.setSyntheticGiftComboMode(mode);
    }

    @Override
    public CommandResult setSyntheticFollow(boolean enabled) {
        return coreService.setSyntheticFollow(enabled);
    }

    @Override
    public CommandResult setSyntheticJoin(boolean enabled) {
        return coreService.setSyntheticJoin(enabled);
    }

    @Override
    public CommandResult setSyntheticMemberLevel(boolean enabled) {
        return coreService.setSyntheticMemberLevel(enabled);
    }

    @Override
    public CommandResult setAlertSound(AlertEventType eventType, boolean enabled) {
        return coreService.setAlertSound(eventType, enabled);
    }

    @Override
    public CommandResult setAlertSoundId(AlertEventType eventType, String soundId) {
        return coreService.setAlertSoundId(eventType, soundId);
    }

    @Override
    public CommandResult setAlertToast(AlertEventType eventType, boolean enabled) {
        return coreService.setAlertToast(eventType, enabled);
    }

    @Override
    public CommandResult setAlertToastTemplate(AlertEventType eventType, String template) {
        return coreService.setAlertToastTemplate(eventType, template);
    }

    @Override
    public CommandResult setAlertMediaMode(AlertEventType eventType, String mediaMode) {
        return coreService.setAlertMediaMode(eventType, mediaMode);
    }

    @Override
    public CommandResult setAlertCustomImage(AlertEventType eventType, String customImage) {
        return coreService.setAlertCustomImage(eventType, customImage);
    }

    @Override
    public CommandResult setAlertGiftMinValue(int value) {
        return coreService.setAlertGiftMinValue(value);
    }

    @Override
    public CommandResult setChatEmotesEnabled(boolean enabled) {
        return coreService.setChatEmotesEnabled(enabled);
    }

    @Override
    public CommandResult setChatLogEnabled(boolean enabled) {
        return coreService.setChatLogEnabled(enabled);
    }

    @Override
    public CommandResult setChatPrefix(String prefix) {
        return coreService.setChatPrefix(prefix);
    }

    @Override
    public CommandResult setChatFormat(String format) {
        return coreService.setChatFormat(format);
    }

    @Override
    public CommandResult setSessionLoggingEnabled(boolean enabled) {
        return coreService.setSessionLoggingEnabled(enabled);
    }

    @Override
    public CommandResult setSessionLoggingFormat(String format) {
        return coreService.setSessionLoggingFormat(format);
    }

    @Override
    public CommandResult reload() {
        CommandResult result = coreService.reload();
        if (result.success()) {
            clearHudMessagesIfOutputHidden(coreService.currentConfig().getOutputMode());
        }
        return result;
    }

    private void clearHudMessagesIfOutputHidden(String outputMode) {
        if (OutputMode.fromString(outputMode) != OutputMode.HUD) {
            hudMessageStore.clear();
        }
    }
}

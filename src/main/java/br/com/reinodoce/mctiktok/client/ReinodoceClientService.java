package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.alert.MinecraftAlertGateway;
import br.com.reinodoce.mctiktok.chat.LiveMessageFormatter;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.client.font.InlineMediaFontHooks;
import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.gui.ReinodoceSettingsScreen;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.core.ReinodoceCoreService;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import br.com.reinodoce.mctiktok.tiktok.TikTokRuntimeServices;
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
public class ReinodoceClientService extends ReinodoceClientPrivacyCommandBridge {
    private final ReinodoceCoreService core;
    private final InlineMediaCache inlineMediaCache;
    private final InlineMediaTokenRegistry inlineMediaTokenRegistry;
    private final ClientOverlayServices overlays;
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
        this.overlays = new ClientOverlayServices();
        InlineMediaFontHooks.installRegistry(inlineMediaTokenRegistry);
        this.core = new ReinodoceCoreService(
                new TikTokRuntimeServices.SideEffects(
                        new MinecraftChatGateway(
                                safePlatformBridge,
                                new LiveMessageFormatter(inlineMediaTokenRegistry),
                                inlineMediaCache,
                                overlays.chatHudStore()
                        ),
                        new MinecraftAlertGateway(safePlatformBridge),
                        overlays.pinnedMessageSink(safePlatformBridge),
                        safePlatformBridge.logsDirectory().resolve("reinodoce")),
                new ReinodoceConfigRepository(),
                safePlatformBridge::selectedLanguageCode
        );
    }

    ReinodoceClientService(
            MinecraftPlatformBridge platformBridge,
            ReinodoceCoreService coreService,
            ClientOverlayServices overlays
    ) {
        this.platformBridge = Objects.requireNonNull(platformBridge, "platformBridge");
        this.inlineMediaCache = new InlineMediaCache();
        this.inlineMediaTokenRegistry = new InlineMediaTokenRegistry(inlineMediaCache);
        this.overlays = Objects.requireNonNull(overlays, "overlays");
        this.core = Objects.requireNonNull(coreService, "coreService");
    }

    /**
     * Initializes core service state.
     */
    public void initialize() {
        core.initialize();
    }

    /**
     * Renders the local HUD output overlay.
     *
     * @param graphics GUI graphics context
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     */
    public void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight) {
        ReinodoceConfig config = core.currentConfig();
        overlays.renderHud(graphics, screenWidth, screenHeight, config);
    }

    /**
     * Renders all local screen overlays owned by this mod.
     *
     * @param graphics GUI graphics context
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     */
    public void renderOverlays(GuiGraphics graphics, int screenWidth, int screenHeight) {
        overlays.renderOverlays(graphics, screenWidth, screenHeight, core.currentConfig());
    }

    /**
     * Saves a complete settings GUI draft through the core config path.
     *
     * @param config settings draft
     * @return command result
     */
    public CommandResult saveSettingsDraft(ReinodoceConfig config) {
        CommandResult result = core.replaceConfig(config);
        if (result.success()) {
            overlays.clearHudMessagesIfOutputHidden(config.getOutputMode());
            overlays.clearPinnedMessagesIfOverlayHidden(config.isPinnedOverlayEnabled());
        }
        return result;
    }

    /**
     * Returns the current client settings snapshot for config-screen entry points.
     *
     * @return current configuration
     */
    public ReinodoceConfig currentConfig() {
        return core.currentConfig();
    }

    @Override
    protected ReinodoceCoreService coreService() {
        return core;
    }

    @Override
    public List<String> statusLines() {
        List<String> lines = new ArrayList<>(core.statusLines());
        lines.addAll(InlineMediaDiagnostics.statusLines(inlineMediaTokenRegistry, inlineMediaCache));
        return lines;
    }

    @Override
    public CommandResult exportDiagnostics() {
        return core.exportDiagnostics(InlineMediaDiagnostics.report(
                inlineMediaTokenRegistry,
                inlineMediaCache));
    }

    @Override
    public CommandResult setOutputMode(String mode) {
        CommandResult result = core.setOutputMode(mode);
        if (result.success()) {
            overlays.clearHudMessagesIfOutputHidden(mode);
        }
        return result;
    }

    @Override
    public CommandResult setPinnedOverlayEnabled(boolean enabled) {
        CommandResult result = core.setPinnedOverlayEnabled(enabled);
        if (result.success()) {
            overlays.clearPinnedMessagesIfOverlayHidden(enabled);
        }
        return result;
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
    public CommandResult reload() {
        CommandResult result = core.reload();
        if (result.success()) {
            overlays.clearHudMessagesIfOutputHidden(core.currentConfig().getOutputMode());
            overlays.clearPinnedMessagesIfOverlayHidden(core.currentConfig().isPinnedOverlayEnabled());
        }
        return result;
    }
}

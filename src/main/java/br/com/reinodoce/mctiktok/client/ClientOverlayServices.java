package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.client.hud.HudMessageStore;
import br.com.reinodoce.mctiktok.client.hud.LocalHudOverlay;
import br.com.reinodoce.mctiktok.client.pinned.LocalPinnedMessageOverlay;
import br.com.reinodoce.mctiktok.client.pinned.MinecraftPinnedMessageGateway;
import br.com.reinodoce.mctiktok.client.pinned.PinnedMessageStore;
import br.com.reinodoce.mctiktok.config.HudPosition;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.pinned.PinnedMessageSink;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Owns client-only overlay stores and renderers for mirrored LIVE output.
 */
final class ClientOverlayServices {
    private final HudMessageStore hudMessageStore;
    private final LocalHudOverlay localHudOverlay;
    private final PinnedMessageStore pinnedMessageStore;
    private final LocalPinnedMessageOverlay localPinnedMessageOverlay;

    ClientOverlayServices() {
        this(new HudMessageStore());
    }

    ClientOverlayServices(HudMessageStore hudMessageStore) {
        this.hudMessageStore = hudMessageStore;
        this.localHudOverlay = new LocalHudOverlay(hudMessageStore);
        this.pinnedMessageStore = new PinnedMessageStore();
        this.localPinnedMessageOverlay = new LocalPinnedMessageOverlay(pinnedMessageStore);
    }

    HudMessageStore chatHudStore() {
        return hudMessageStore;
    }

    PinnedMessageSink pinnedMessageSink(MinecraftPlatformBridge platformBridge) {
        return new MinecraftPinnedMessageGateway(platformBridge, pinnedMessageStore);
    }

    void renderOverlays(GuiGraphics graphics, int screenWidth, int screenHeight, ReinodoceConfig config) {
        renderHud(graphics, screenWidth, screenHeight, config);
        renderPinnedOverlay(graphics, screenWidth, screenHeight, config);
    }

    void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight, ReinodoceConfig config) {
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

    void clearHudMessagesIfOutputHidden(String outputMode) {
        if (OutputMode.fromString(outputMode) != OutputMode.HUD) {
            hudMessageStore.clear();
        }
    }

    void clearPinnedMessagesIfOverlayHidden(boolean enabled) {
        if (!enabled) {
            pinnedMessageStore.clear();
        }
    }

    private void renderPinnedOverlay(GuiGraphics graphics, int screenWidth, int screenHeight, ReinodoceConfig config) {
        if (!config.isPinnedOverlayEnabled()) {
            return;
        }
        localPinnedMessageOverlay.render(
                graphics,
                screenWidth,
                screenHeight,
                HudPosition.fromString(config.getPinnedOverlayPosition()),
                config.getPinnedOverlayMessages());
    }
}

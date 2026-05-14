package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.hud.HudMessageStore;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;

/**
 * Chat sink implementation that formats TikTok events and schedules Minecraft chat writes on the client thread.
 */
public class MinecraftChatGateway implements ChatEventSink {
    private final MinecraftPlatformBridge platformBridge;
    private final LiveMessageFormatter formatter;
    private final InlineMediaCache inlineMediaCache;
    private final HudMessageStore hudMessageStore;

    /**
     * Creates a gateway.
     *
     * @param platformBridge Minecraft client adapter
     * @param formatter chat formatter
     * @param inlineMediaCache inline media cache used for rich message prefetching
     * @param hudMessageStore HUD output message store
     */
    public MinecraftChatGateway(
            MinecraftPlatformBridge platformBridge,
            LiveMessageFormatter formatter,
            InlineMediaCache inlineMediaCache,
            HudMessageStore hudMessageStore
    ) {
        this.platformBridge = platformBridge;
        this.formatter = formatter;
        this.inlineMediaCache = inlineMediaCache;
        this.hudMessageStore = hudMessageStore;
    }

    @Override
    public void sendLiveComment(ReinodoceConfig config, String username, String message) {
        send(config, formatter.formatLiveComment(ChatMessageStyle.from(config), username, message));
    }

    @Override
    public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatLiveComment(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendStarComment(ReinodoceConfig config, String username, String message) {
        send(config, formatter.formatStarComment(ChatMessageStyle.from(config), username, message));
    }

    @Override
    public void sendStarComment(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatStarComment(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count) {
        send(config, formatter.formatSyntheticGift(ChatMessageStyle.from(config), username, giftName, count));
    }

    @Override
    public void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticGift(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticFollow(ReinodoceConfig config, String username) {
        send(config, formatter.formatSyntheticFollow(ChatMessageStyle.from(config), username));
    }

    @Override
    public void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticFollow(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticJoin(ReinodoceConfig config, String username) {
        send(config, formatter.formatSyntheticJoin(ChatMessageStyle.from(config), username));
    }

    @Override
    public void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticJoin(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel) {
        send(config, formatter.formatSyntheticMemberLevel(ChatMessageStyle.from(config), username, memberLevel));
    }

    @Override
    public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
        sendTracked(config, formatter.formatSyntheticMemberLevel(ChatMessageStyle.from(config), message, memberLevel));
    }

    @Override
    public void sendSystem(String message, boolean success) {
        send(formatter.formatSystem(message, success), true);
    }

    /**
     * Sends a preformatted component to Minecraft chat on the client thread.
     *
     * @param component component to add to chat
     */
    public void send(Component component) {
        send(component, true);
    }

    private void send(Component component, boolean logToChat) {
        platformBridge.runOnClientThread(() -> platformBridge.addChatMessage(component, logToChat));
    }

    private void send(ReinodoceConfig config, Component component) {
        OutputMode mode = OutputMode.fromString(config == null ? "" : config.getOutputMode());
        Component plainComponent = Component.literal(component.getString());
        switch (mode) {
            case CHAT -> send(component, config != null && config.isChatLogEnabled());
            case ACTIONBAR -> platformBridge.runOnClientThread(() -> platformBridge.showActionBarMessage(plainComponent));
            case HUD -> platformBridge.runOnClientThread(() -> hudMessageStore.add(plainComponent, config.getHudLines()));
            case OFF -> {
            }
            default -> send(component, config != null && config.isChatLogEnabled());
        }
    }

    private void sendTracked(ReinodoceConfig config, FormattedLiveComment formatted) {
        if (formatted == null) {
            return;
        }

        RichLiveMessage richMessage = formatted.richMessage();
        String outputMode = config == null ? "" : config.getOutputMode();
        if (richMessage != null && OutputMode.fromString(outputMode) == OutputMode.CHAT) {
            inlineMediaCache.prefetch(richMessage);
        }

        send(config, formatted.component());
    }
}

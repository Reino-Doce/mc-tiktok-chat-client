package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
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

    /**
     * Creates a gateway.
     *
     * @param platformBridge Minecraft client adapter
     * @param formatter chat formatter
     * @param inlineMediaCache inline media cache used for rich message prefetching
     */
    public MinecraftChatGateway(
            MinecraftPlatformBridge platformBridge,
            LiveMessageFormatter formatter,
            InlineMediaCache inlineMediaCache
    ) {
        this.platformBridge = platformBridge;
        this.formatter = formatter;
        this.inlineMediaCache = inlineMediaCache;
    }

    @Override
    public void sendLiveComment(ReinodoceConfig config, String username, String message) {
        send(formatter.formatLiveComment(ChatMessageStyle.from(config), username, message), config.isChatLogEnabled());
    }

    @Override
    public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatLiveComment(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendStarComment(ReinodoceConfig config, String username, String message) {
        send(formatter.formatStarComment(ChatMessageStyle.from(config), username, message), config.isChatLogEnabled());
    }

    @Override
    public void sendStarComment(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatStarComment(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count) {
        send(formatter.formatSyntheticGift(ChatMessageStyle.from(config), username, giftName, count), config.isChatLogEnabled());
    }

    @Override
    public void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticGift(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticFollow(ReinodoceConfig config, String username) {
        send(formatter.formatSyntheticFollow(ChatMessageStyle.from(config), username), config.isChatLogEnabled());
    }

    @Override
    public void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticFollow(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticJoin(ReinodoceConfig config, String username) {
        send(formatter.formatSyntheticJoin(ChatMessageStyle.from(config), username), config.isChatLogEnabled());
    }

    @Override
    public void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticJoin(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel) {
        send(formatter.formatSyntheticMemberLevel(ChatMessageStyle.from(config), username, memberLevel), config.isChatLogEnabled());
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

    private void sendTracked(ReinodoceConfig config, FormattedLiveComment formatted) {
        if (formatted == null) {
            return;
        }

        RichLiveMessage richMessage = formatted.richMessage();
        if (richMessage != null) {
            inlineMediaCache.prefetch(richMessage);
        }

        send(formatted.component(), config.isChatLogEnabled());
    }
}

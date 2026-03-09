package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;

public class MinecraftChatGateway {
    private final MinecraftPlatformBridge platformBridge;
    private final LiveMessageFormatter formatter;
    private final InlineMediaCache inlineMediaCache;

    public MinecraftChatGateway(
            MinecraftPlatformBridge platformBridge,
            LiveMessageFormatter formatter,
            InlineMediaCache inlineMediaCache
    ) {
        this.platformBridge = platformBridge;
        this.formatter = formatter;
        this.inlineMediaCache = inlineMediaCache;
    }

    public void sendLiveComment(String prefix, String username, String message) {
        send(formatter.formatLiveComment(prefix, username, message));
    }

    public void sendLiveComment(String prefix, RichLiveMessage message) {
        sendTracked(formatter.formatLiveComment(prefix, message));
    }

    public void sendStarComment(String prefix, String username, String message) {
        send(formatter.formatStarComment(prefix, username, message));
    }

    public void sendStarComment(String prefix, RichLiveMessage message) {
        sendTracked(formatter.formatStarComment(prefix, message));
    }

    public void sendSyntheticGift(String prefix, String username, String giftName, int count) {
        send(formatter.formatSyntheticGift(prefix, username, giftName, count));
    }

    public void sendSyntheticGift(String prefix, RichLiveMessage message) {
        sendTracked(formatter.formatSyntheticGift(prefix, message));
    }

    public void sendSyntheticFollow(String prefix, String username) {
        send(formatter.formatSyntheticFollow(prefix, username));
    }

    public void sendSyntheticFollow(String prefix, RichLiveMessage message) {
        sendTracked(formatter.formatSyntheticFollow(prefix, message));
    }

    public void sendSyntheticJoin(String prefix, String username) {
        send(formatter.formatSyntheticJoin(prefix, username));
    }

    public void sendSyntheticJoin(String prefix, RichLiveMessage message) {
        sendTracked(formatter.formatSyntheticJoin(prefix, message));
    }

    public void sendSyntheticMemberLevel(String prefix, String username, int memberLevel) {
        send(formatter.formatSyntheticMemberLevel(prefix, username, memberLevel));
    }

    public void sendSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel) {
        sendTracked(formatter.formatSyntheticMemberLevel(prefix, message, memberLevel));
    }

    public void sendSystem(String message, boolean success) {
        send(formatter.formatSystem(message, success));
    }

    public void send(Component component) {
        platformBridge.runOnClientThread(() -> platformBridge.addChatMessage(component));
    }

    private void sendTracked(FormattedLiveComment formatted) {
        if (formatted == null) {
            return;
        }

        RichLiveMessage richMessage = formatted.richMessage();
        if (richMessage != null) {
            inlineMediaCache.prefetch(richMessage);
        }

        platformBridge.runOnClientThread(() -> platformBridge.addChatMessage(formatted.component()));
    }
}

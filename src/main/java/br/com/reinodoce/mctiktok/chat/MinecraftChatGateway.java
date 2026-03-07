package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;

public class MinecraftChatGateway {
    private final MinecraftPlatformBridge platformBridge;
    private final LiveMessageFormatter formatter;

    public MinecraftChatGateway(MinecraftPlatformBridge platformBridge, LiveMessageFormatter formatter) {
        this.platformBridge = platformBridge;
        this.formatter = formatter;
    }

    public void sendLiveComment(String prefix, String username, String message) {
        send(formatter.formatLiveComment(prefix, username, message));
    }

    public void sendSyntheticGift(String prefix, String username, String giftName, int count) {
        send(formatter.formatSyntheticGift(prefix, username, giftName, count));
    }

    public void sendSyntheticFollow(String prefix, String username) {
        send(formatter.formatSyntheticFollow(prefix, username));
    }

    public void sendSyntheticJoin(String prefix, String username) {
        send(formatter.formatSyntheticJoin(prefix, username));
    }

    public void sendSyntheticMemberLevel(String prefix, String username, int memberLevel) {
        send(formatter.formatSyntheticMemberLevel(prefix, username, memberLevel));
    }

    public void sendSystem(String message, boolean success) {
        send(formatter.formatSystem(message, success));
    }

    public void send(Component component) {
        platformBridge.runOnClientThread(() -> platformBridge.addChatMessage(component));
    }
}

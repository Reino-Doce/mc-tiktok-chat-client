package br.com.reinodoce.mctiktok.platform;

import net.minecraft.network.chat.Component;

public interface MinecraftPlatformBridge {
    void runOnClientThread(Runnable runnable);

    void addChatMessage(Component component);

    boolean isClientReady();
}

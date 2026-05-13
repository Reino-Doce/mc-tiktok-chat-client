package br.com.reinodoce.mctiktok.platform;

import net.minecraft.network.chat.Component;

/**
 * Minimal Minecraft client adapter used by core services without depending directly on Forge runtime state.
 */
public interface MinecraftPlatformBridge {
    /**
     * Schedules work on the Minecraft client thread.
     *
     * @param runnable work to run
     */
    void runOnClientThread(Runnable runnable);

    /**
     * Adds a component to the local player's chat HUD.
     *
     * @param component chat component to add
     */
    void addChatMessage(Component component);

    /**
     * Reports whether client GUI state is ready for chat messages.
     *
     * @return true when chat can be written safely
     */
    boolean isClientReady();
}

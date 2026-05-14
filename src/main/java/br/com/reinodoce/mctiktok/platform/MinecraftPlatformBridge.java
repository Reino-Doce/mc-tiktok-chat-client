package br.com.reinodoce.mctiktok.platform;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;

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
     * @param logToChat whether Minecraft should write the component to its chat log
     */
    void addChatMessage(Component component, boolean logToChat);

    /**
     * Plays a local alert sound.
     */
    default void playAlertSound() {
    }

    /**
     * Shows a local alert toast.
     *
     * @param title toast title
     * @param message toast message
     */
    default void showAlertToast(Component title, Component message) {
    }

    /**
     * Reports whether client GUI state is ready for chat messages.
     *
     * @return true when chat can be written safely
     */
    boolean isClientReady();

    /**
     * Reports the selected Minecraft language code.
     *
     * @return Minecraft language code such as {@code en_us}
     */
    default String selectedLanguageCode() {
        return "en_us";
    }

    /**
     * Returns the Minecraft logs directory.
     *
     * @return logs directory path
     */
    default Path logsDirectory() {
        return Path.of("logs");
    }
}

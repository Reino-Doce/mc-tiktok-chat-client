package br.com.reinodoce.mctiktok.platform;

import br.com.reinodoce.mctiktok.alert.AlertToastPayload;
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
     * Shows a local actionbar message.
     *
     * @param component actionbar component
     */
    default void showActionBarMessage(Component component) {
    }

    /**
     * Plays a local alert sound.
     */
    default void playAlertSound() {
    }

    /**
     * Plays a local alert sound.
     *
     * @param soundId persisted alert sound identifier
     */
    default void playAlertSound(String soundId) {
        playAlertSound();
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
     * Shows a local alert toast with optional media hints. Platforms without custom image toast rendering should
     * safely fall back to the standard title/message toast.
     *
     * @param title toast title
     * @param message toast message
     * @param payload rendered toast payload and media hints
     */
    default void showAlertToast(Component title, Component message, AlertToastPayload payload) {
        showAlertToast(title, message);
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

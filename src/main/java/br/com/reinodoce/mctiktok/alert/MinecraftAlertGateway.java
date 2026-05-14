package br.com.reinodoce.mctiktok.alert;

import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Alert sink that schedules local Minecraft sound and toast effects on the client thread.
 */
public class MinecraftAlertGateway implements AlertSink {
    private final MinecraftPlatformBridge platformBridge;

    /**
     * Creates a gateway.
     *
     * @param platformBridge Minecraft client adapter
     */
    public MinecraftAlertGateway(MinecraftPlatformBridge platformBridge) {
        this.platformBridge = Objects.requireNonNull(platformBridge, "platformBridge");
    }

    @Override
    public void playAlertSound() {
        platformBridge.runOnClientThread(platformBridge::playAlertSound);
    }

    @Override
    public void playAlertSound(String soundId) {
        platformBridge.runOnClientThread(() -> platformBridge.playAlertSound(soundId));
    }

    @Override
    public void showAlertToast(String title, String message) {
        Component titleComponent = Component.literal(title);
        Component messageComponent = Component.literal(message);
        platformBridge.runOnClientThread(() -> platformBridge.showAlertToast(titleComponent, messageComponent));
    }
}

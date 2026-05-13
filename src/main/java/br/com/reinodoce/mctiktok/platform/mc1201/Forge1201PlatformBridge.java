package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Forge 1.20.1 implementation of the Minecraft client adapter.
 */
@SuppressWarnings("PMD.CloseResource")
public class Forge1201PlatformBridge implements MinecraftPlatformBridge {
    @Override
    public void runOnClientThread(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

    @Override
    public void addChatMessage(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui != null) {
            minecraft.gui.getChat().addMessage(component);
        }
    }

    @Override
    public boolean isClientReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.gui != null;
    }
}

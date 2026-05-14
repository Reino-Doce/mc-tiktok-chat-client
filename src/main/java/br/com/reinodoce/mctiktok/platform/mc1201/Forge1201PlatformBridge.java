package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

/**
 * Forge 1.20.1 implementation of the Minecraft client adapter.
 */
@SuppressWarnings("PMD.CloseResource")
public class Forge1201PlatformBridge implements MinecraftPlatformBridge {
    private static final int SILENT_ADD_MESSAGE_PARAMETER_COUNT = 5;
    private static final int GUI_TAG_PARAMETER_INDEX = 3;
    private static final int REFRESH_PARAMETER_INDEX = 4;
    private static final float ALERT_SOUND_PITCH = 1.0F;
    private static final float ALERT_SOUND_VOLUME = 1.0F;
    private static final ResourceLocation ALERT_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("ui.toast.in");
    private static final Method SILENT_ADD_MESSAGE = findSilentAddMessageMethod();

    @Override
    public void runOnClientThread(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

    @Override
    public void addChatMessage(Component component, boolean logToChat) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui != null) {
            ChatComponent chat = minecraft.gui.getChat();
            if (logToChat || !addSilentChatMessage(chat, component, minecraft.gui.getGuiTicks())) {
                chat.addMessage(component);
            }
        }
    }

    @Override
    public void showActionBarMessage(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.gui != null) {
            minecraft.gui.setOverlayMessage(component, false);
        }
    }

    @Override
    public void playAlertSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(this::playAlertSound);
            return;
        }
        playAlertSound(minecraft);
    }

    private static void playAlertSound(Minecraft minecraft) {
        if (minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(createAlertSoundInstance());
        }
    }

    static SoundInstance createAlertSoundInstance() {
        return new SimpleSoundInstance(
                ALERT_SOUND_LOCATION,
                SoundSource.MASTER,
                ALERT_SOUND_VOLUME,
                ALERT_SOUND_PITCH,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true);
    }

    @Override
    public void showAlertToast(Component title, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getToasts() != null) {
            SystemToast.addOrUpdate(
                    minecraft.getToasts(),
                    SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    title,
                    message);
        }
    }

    @Override
    public boolean isClientReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.gui != null;
    }

    @Override
    public String selectedLanguageCode() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) {
            return MinecraftPlatformBridge.super.selectedLanguageCode();
        }
        String selected = minecraft.getLanguageManager().getSelected();
        return selected == null || selected.isBlank()
                ? MinecraftPlatformBridge.super.selectedLanguageCode()
                : selected;
    }

    @Override
    public Path logsDirectory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) {
            return MinecraftPlatformBridge.super.logsDirectory();
        }
        return minecraft.gameDirectory.toPath().resolve("logs");
    }

    private static Method findSilentAddMessageMethod() {
        for (Method method : ChatComponent.class.getDeclaredMethods()) {
            if (isSilentAddMessageMethod(method)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static boolean isSilentAddMessageMethod(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (!Modifier.isPrivate(method.getModifiers())
                || method.getReturnType() != Void.TYPE
                || params.length != SILENT_ADD_MESSAGE_PARAMETER_COUNT) {
            return false;
        }
        return hasSilentAddMessageParameterTypes(params);
    }

    private static boolean hasSilentAddMessageParameterTypes(Class<?>[] params) {
        boolean messageParams = params[0] == Component.class
                && params[1] == MessageSignature.class
                && params[2] == Integer.TYPE;
        boolean stateParams = params[GUI_TAG_PARAMETER_INDEX] == GuiMessageTag.class
                && params[REFRESH_PARAMETER_INDEX] == Boolean.TYPE;
        return messageParams && stateParams;
    }

    private static boolean addSilentChatMessage(ChatComponent chat, Component component, int guiTicks) {
        if (SILENT_ADD_MESSAGE == null) {
            return false;
        }
        try {
            SILENT_ADD_MESSAGE.invoke(chat, component, null, guiTicks, null, false);
            return true;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to add silent chat message", exception);
            return false;
        }
    }
}

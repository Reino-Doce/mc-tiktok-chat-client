package br.com.reinodoce.mctiktok.alert;

import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MinecraftAlertGatewayTest {
    private static final String TITLE = "Alert";
    private static final String MESSAGE = "alice sent a gift";
    private static final String CUSTOM_SOUND = "minecraft:entity.experience_orb.pickup";

    @Test
    void soundPlaybackIsScheduledOnClientThread() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftAlertGateway gateway = new MinecraftAlertGateway(bridge);

        gateway.playAlertSound();

        assertEquals(1, bridge.pendingTasks());
        assertEquals(0, bridge.sounds());

        bridge.runNextClientTask();

        assertEquals(1, bridge.sounds());
        assertFalse(bridge.soundRanOffClientThread());
    }

    @Test
    void customSoundPlaybackIsScheduledOnClientThread() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftAlertGateway gateway = new MinecraftAlertGateway(bridge);

        gateway.playAlertSound(CUSTOM_SOUND);

        assertEquals(1, bridge.pendingTasks());
        assertEquals("", bridge.soundId());

        bridge.runNextClientTask();

        assertEquals(1, bridge.sounds());
        assertEquals(CUSTOM_SOUND, bridge.soundId());
        assertFalse(bridge.soundRanOffClientThread());
    }

    @Test
    void toastSchedulingKeepsExistingToastPayload() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftAlertGateway gateway = new MinecraftAlertGateway(bridge);

        gateway.showAlertToast(TITLE, MESSAGE);

        assertEquals(1, bridge.pendingTasks());
        assertEquals("", bridge.toastTitle().getString());

        bridge.runNextClientTask();

        assertEquals(TITLE, bridge.toastTitle().getString());
        assertEquals(MESSAGE, bridge.toastMessage().getString());
        assertFalse(bridge.toastRanOffClientThread());
    }

    private static final class RecordingPlatformBridge implements MinecraftPlatformBridge {
        private final Queue<Runnable> clientTasks = new ArrayDeque<>();
        private int recordedSounds;
        private boolean onClientThread;
        private boolean recordedSoundRanOffClientThread;
        private boolean recordedToastRanOffClientThread;
        private String recordedSoundId = "";
        private Component recordedToastTitle = Component.literal("");
        private Component recordedToastMessage = Component.literal("");

        @Override
        public void runOnClientThread(Runnable runnable) {
            clientTasks.add(() -> {
                onClientThread = true;
                try {
                    runnable.run();
                } finally {
                    onClientThread = false;
                }
            });
        }

        @Override
        public void addChatMessage(Component component, boolean logToChat) {
        }

        @Override
        public void playAlertSound() {
            recordedSoundRanOffClientThread = !onClientThread;
            recordedSounds++;
        }

        @Override
        public void playAlertSound(String soundId) {
            recordedSoundRanOffClientThread = !onClientThread;
            recordedSounds++;
            recordedSoundId = soundId;
        }

        @Override
        public void showAlertToast(Component title, Component message) {
            recordedToastRanOffClientThread = !onClientThread;
            recordedToastTitle = title;
            recordedToastMessage = message;
        }

        @Override
        public boolean isClientReady() {
            return true;
        }

        private int pendingTasks() {
            return clientTasks.size();
        }

        private int sounds() {
            return recordedSounds;
        }

        private boolean soundRanOffClientThread() {
            return recordedSoundRanOffClientThread;
        }

        private String soundId() {
            return recordedSoundId;
        }

        private boolean toastRanOffClientThread() {
            return recordedToastRanOffClientThread;
        }

        private Component toastTitle() {
            return recordedToastTitle;
        }

        private Component toastMessage() {
            return recordedToastMessage;
        }

        private void runNextClientTask() {
            Runnable task = clientTasks.remove();
            task.run();
        }
    }
}

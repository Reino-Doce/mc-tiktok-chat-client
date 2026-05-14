package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.client.hud.HudMessageStore;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.core.ReinodoceCoreService;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceClientServiceTest {
    private static final int HUD_LINES = 3;
    private static final String CHAT_MODE = "chat";
    private static final String HUD_MODE = "hud";
    private static final String OFF_MODE = "off";
    private static final String EN_US = "en_us";
    private static final String RETAINED_MESSAGE = "retained";

    @TempDir
    Path tempDir;

    @Test
    void outputCommandClearsRetainedHudMessagesWhenLeavingHudMode() {
        HudMessageStore hudMessageStore = new HudMessageStore();
        ReinodoceClientService service = service(hudMessageStore);
        service.setOutputMode(HUD_MODE);
        hudMessageStore.add(Component.literal(RETAINED_MESSAGE), HUD_LINES);

        CommandResult result = service.setOutputMode(CHAT_MODE);

        assertTrue(result.success());
        assertEquals(0, hudMessageStore.snapshot(HUD_LINES).size());
    }

    @Test
    void outputCommandClearsRetainedHudMessagesWhenOutputBecomesOff() {
        HudMessageStore hudMessageStore = new HudMessageStore();
        ReinodoceClientService service = service(hudMessageStore);
        hudMessageStore.add(Component.literal(RETAINED_MESSAGE), HUD_LINES);

        CommandResult result = service.setOutputMode(OFF_MODE);

        assertTrue(result.success());
        assertEquals(0, hudMessageStore.snapshot(HUD_LINES).size());
    }

    @Test
    void guiDraftSaveClearsRetainedHudMessagesWhenLeavingHudMode() {
        HudMessageStore hudMessageStore = new HudMessageStore();
        ReinodoceClientService service = service(hudMessageStore);
        service.setOutputMode(HUD_MODE);
        hudMessageStore.add(Component.literal(RETAINED_MESSAGE), HUD_LINES);
        ReinodoceConfig draft = service.currentConfig();
        draft.setOutputMode(CHAT_MODE);

        CommandResult result = service.saveSettingsDraft(draft);

        assertTrue(result.success());
        assertEquals(CHAT_MODE, service.currentConfig().getOutputMode());
        assertEquals(0, hudMessageStore.snapshot(HUD_LINES).size());
    }

    @Test
    void guiDraftSaveKeepsRetainedHudMessagesWhenHudModeStaysActive() {
        HudMessageStore hudMessageStore = new HudMessageStore();
        ReinodoceClientService service = service(hudMessageStore);
        service.setOutputMode(HUD_MODE);
        hudMessageStore.add(Component.literal(RETAINED_MESSAGE), HUD_LINES);
        ReinodoceConfig draft = service.currentConfig();
        draft.setHudLines(HUD_LINES + 1);

        CommandResult result = service.saveSettingsDraft(draft);

        assertTrue(result.success());
        assertEquals(HUD_MODE, service.currentConfig().getOutputMode());
        assertEquals(1, hudMessageStore.snapshot(HUD_LINES).size());
    }

    private ReinodoceClientService service(HudMessageStore hudMessageStore) {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve("config.json"));
        ReinodoceCoreService coreService = new ReinodoceCoreService(
                ChatEventSink.noop(),
                repository,
                () -> EN_US,
                tempDir.resolve("logs"));
        return new ReinodoceClientService(new RecordingPlatformBridge(), coreService, hudMessageStore);
    }

    private static final class RecordingPlatformBridge implements MinecraftPlatformBridge {
        @Override
        public void runOnClientThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void addChatMessage(Component component, boolean logToChat) {
        }

        @Override
        public boolean isClientReady() {
            return true;
        }
    }
}

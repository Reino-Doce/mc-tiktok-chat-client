package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.i18n.Translations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceCoreServiceTest {
    private static final String EN_US = "en_us";

    @TempDir
    Path tempDir;

    @Test
    void connectLastRequiresSavedUsername() {
        ReinodoceCoreService service = new ReinodoceCoreService(
                ChatEventSink.noop(),
                new ReinodoceConfigRepository(tempDir.resolve("missing.json")),
                () -> EN_US);

        CommandResult result = service.connectLast();

        assertFalse(result.success());
        assertEquals(Translations.tr("reinodoce.command.connect.missing_saved"), result.message());
    }

    @Test
    void autoConnectSettingPersists() {
        Path configFile = tempDir.resolve("config.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);

        CommandResult result = service.setAutoConnectOnStart(true);
        ReinodoceConfig loaded = repository.load();

        assertTrue(result.success());
        assertTrue(loaded.isAutoConnectOnStart());
    }

    @Test
    void alertSettingsPersist() {
        Path configFile = tempDir.resolve("alerts.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);

        CommandResult soundResult = service.setAlertSound(AlertEventType.GIFT, true);
        CommandResult toastResult = service.setAlertToast(AlertEventType.FOLLOW, true);
        CommandResult minValueResult = service.setAlertGiftMinValue(100);
        ReinodoceConfig loaded = repository.load();

        assertTrue(soundResult.success());
        assertTrue(toastResult.success());
        assertTrue(minValueResult.success());
        assertTrue(loaded.isAlertSoundEnabled(AlertEventType.GIFT));
        assertTrue(loaded.isAlertToastEnabled(AlertEventType.FOLLOW));
        assertEquals(100, loaded.getAlertGiftMinValue());
    }

    @Test
    void outputSettingsPersist() {
        Path configFile = tempDir.resolve("output.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);

        CommandResult modeResult = service.setOutputMode("hud");
        CommandResult positionResult = service.setHudPosition("bottom-right");
        CommandResult linesResult = service.setHudLines(4);
        ReinodoceConfig loaded = repository.load();

        assertTrue(modeResult.success());
        assertTrue(positionResult.success());
        assertTrue(linesResult.success());
        assertEquals("hud", loaded.getOutputMode());
        assertEquals("bottom-right", loaded.getHudPosition());
        assertEquals(4, loaded.getHudLines());
    }
}

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
    private static final String HUD_MODE = "hud";

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

        CommandResult modeResult = service.setOutputMode(HUD_MODE);
        CommandResult positionResult = service.setHudPosition("bottom-right");
        CommandResult linesResult = service.setHudLines(4);
        ReinodoceConfig loaded = repository.load();

        assertTrue(modeResult.success());
        assertTrue(positionResult.success());
        assertTrue(linesResult.success());
        assertEquals(HUD_MODE, loaded.getOutputMode());
        assertEquals("bottom-right", loaded.getHudPosition());
        assertEquals(4, loaded.getHudLines());
    }

    @Test
    void guiDraftPersistsThroughConfigRepository() {
        Path configFile = tempDir.resolve("settings-gui.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);
        ReinodoceConfig draft = ReinodoceConfig.defaults();
        draft.setLastUsername("  Reino_Doce  ");
        draft.setReconnectSeconds(0);
        draft.setOutputMode(HUD_MODE);
        draft.setHudPosition("bottom-left");
        draft.setHudLines(ReinodoceConfig.MAX_HUD_LINES + 1);
        draft.setChatPrefix("TikTok");
        draft.setChatFormat("{prefix} {username}: {message}");
        draft.setChatEmotesEnabled(false);
        draft.setSyntheticGiftMinValue(50);
        draft.setSyntheticGiftComboMode("single");
        draft.setSyntheticFollowEnabled(true);
        draft.setSyntheticJoinEnabled(true);
        draft.setSyntheticMemberLevelEnabled(true);
        draft.setRuleFollowerOnly(true);
        draft.setRuleMinMemberLevel(2);

        CommandResult result = service.replaceConfig(draft);
        ReinodoceConfig loaded = repository.load();

        assertTrue(result.success());
        assertEquals("Reino_Doce", loaded.getLastUsername());
        assertEquals(0, loaded.getReconnectSeconds());
        assertEquals(HUD_MODE, loaded.getOutputMode());
        assertEquals("bottom-left", loaded.getHudPosition());
        assertEquals(ReinodoceConfig.MAX_HUD_LINES, loaded.getHudLines());
        assertEquals("TikTok", loaded.getChatPrefix());
        assertEquals("{prefix} {username}: {message}", loaded.getChatFormat());
        assertFalse(loaded.isChatEmotesEnabled());
        assertEquals(50, loaded.getSyntheticGiftMinValue());
        assertEquals("single", loaded.getSyntheticGiftComboMode());
        assertTrue(loaded.isSyntheticFollowEnabled());
        assertTrue(loaded.isSyntheticJoinEnabled());
        assertTrue(loaded.isSyntheticMemberLevelEnabled());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(2, loaded.getRuleMinMemberLevel());
    }
}

package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import br.com.reinodoce.mctiktok.tiktok.MemberLevelResolver;
import br.com.reinodoce.mctiktok.tiktok.TikTokClientFacade;
import br.com.reinodoce.mctiktok.tiktok.TikTokRuntimeServices;
import br.com.reinodoce.mctiktok.util.MessageDeduplicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceCoreServiceTest {
    private static final String EN_US = "en_us";
    private static final String PT_BR = "pt_br";
    private static final String DE_DE = "de_de";
    private static final String HUD_MODE = "hud";
    private static final String CUSTOM_SOUND_ID = "minecraft:entity.experience_orb.pickup";
    private static final String CUSTOM_TEMPLATE = "{username} -> {giftName} ({count}/{diamonds})";
    private static final String CUSTOM_IMAGE = "https://cdn.example/alert.png";

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
        CommandResult soundIdResult = service.setAlertSoundId(
                AlertEventType.GIFT,
                CUSTOM_SOUND_ID);
        CommandResult toastResult = service.setAlertToast(AlertEventType.FOLLOW, true);
        CommandResult templateResult = service.setAlertToastTemplate(AlertEventType.GIFT, CUSTOM_TEMPLATE);
        CommandResult mediaModeResult = service.setAlertMediaMode(AlertEventType.GIFT, "gift");
        CommandResult customImageResult = service.setAlertCustomImage(AlertEventType.FOLLOW, CUSTOM_IMAGE);
        CommandResult minValueResult = service.setAlertGiftMinValue(100);
        ReinodoceConfig loaded = repository.load();

        assertTrue(soundResult.success());
        assertTrue(soundIdResult.success());
        assertTrue(toastResult.success());
        assertTrue(templateResult.success());
        assertTrue(mediaModeResult.success());
        assertTrue(customImageResult.success());
        assertTrue(minValueResult.success());
        assertTrue(loaded.isAlertSoundEnabled(AlertEventType.GIFT));
        assertEquals(CUSTOM_SOUND_ID, loaded.getAlertSoundId(AlertEventType.GIFT));
        assertTrue(loaded.isAlertToastEnabled(AlertEventType.FOLLOW));
        assertEquals(CUSTOM_TEMPLATE, loaded.getAlertToastTemplate(AlertEventType.GIFT));
        assertEquals("gift", loaded.getAlertMediaMode(AlertEventType.GIFT));
        assertEquals(CUSTOM_IMAGE, loaded.getAlertCustomImage(AlertEventType.FOLLOW));
        assertEquals(100, loaded.getAlertGiftMinValue());
    }

    @Test
    void invalidAlertTemplateAndMediaModeAreRejectedWithoutPersisting() {
        Path configFile = tempDir.resolve("invalid-alert-toast-format.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);
        service.setAlertToastTemplate(AlertEventType.GIFT, CUSTOM_TEMPLATE);
        service.setAlertMediaMode(AlertEventType.GIFT, "profile");

        CommandResult templateResult = service.setAlertToastTemplate(AlertEventType.GIFT, "{username} {bad}");
        CommandResult mediaModeResult = service.setAlertMediaMode(AlertEventType.GIFT, "bad-mode");
        ReinodoceConfig loaded = repository.load();

        assertFalse(templateResult.success());
        assertFalse(mediaModeResult.success());
        assertEquals(CUSTOM_TEMPLATE, loaded.getAlertToastTemplate(AlertEventType.GIFT));
        assertEquals("profile", loaded.getAlertMediaMode(AlertEventType.GIFT));
    }

    @Test
    void invalidAlertSoundIdIsRejectedWithoutPersisting() {
        Path configFile = tempDir.resolve("invalid-alert-sound-id.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);
        service.setAlertSoundId(AlertEventType.GIFT, CUSTOM_SOUND_ID);

        CommandResult result = service.setAlertSoundId(AlertEventType.GIFT, "bad sound");
        ReinodoceConfig loaded = repository.load();

        assertFalse(result.success());
        assertEquals(
                Translations.tr("reinodoce.command.alert_sound_id.invalid", "bad sound"),
                result.message());
        assertEquals(CUSTOM_SOUND_ID, loaded.getAlertSoundId(AlertEventType.GIFT));
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
    void languageSettingPersistsOverrideAndAutoUsesMinecraftLanguage() {
        Path configFile = tempDir.resolve("language.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        AtomicReference<String> minecraftLanguage = new AtomicReference<>(PT_BR);
        ReinodoceCoreService service = new ReinodoceCoreService(
                ChatEventSink.noop(), repository, minecraftLanguage::get);

        assertEquals(
                Translations.tr("reinodoce.command.language.auto", PT_BR),
                service.languageLines().get(0));

        CommandResult overrideResult = service.setLanguage("de-DE");
        ReinodoceConfig loadedOverride = repository.load();
        minecraftLanguage.set("fr_fr");

        assertTrue(overrideResult.success());
        assertEquals(DE_DE, loadedOverride.getLanguage());
        assertEquals(
                Translations.tr("reinodoce.command.language.set_override", DE_DE, DE_DE),
                overrideResult.message());
        assertEquals(
                Translations.tr("reinodoce.command.language.override", DE_DE, DE_DE),
                service.languageLines().get(0));

        CommandResult autoResult = service.setLanguage("auto");
        ReinodoceConfig loadedAuto = repository.load();

        assertTrue(autoResult.success());
        assertEquals("auto", loadedAuto.getLanguage());
        assertEquals(
                Translations.tr("reinodoce.command.language.set_auto", "fr_fr"),
                autoResult.message());
    }

    @Test
    void invalidLanguageSettingIsRejectedWithoutPersisting() {
        Path configFile = tempDir.resolve("invalid-language.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceCoreService service = new ReinodoceCoreService(ChatEventSink.noop(), repository, () -> EN_US);
        service.setLanguage(PT_BR);

        CommandResult result = service.setLanguage("not a locale");
        ReinodoceConfig loaded = repository.load();

        assertFalse(result.success());
        assertEquals(Translations.tr("reinodoce.command.language.invalid", "not a locale"), result.message());
        assertEquals(PT_BR, loaded.getLanguage());
    }

    @Test
    void reloadReconnectsWhenEffectiveLanguageChanges() {
        Path configFile = tempDir.resolve("reload-language-changed.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        AtomicReference<String> minecraftLanguage = new AtomicReference<>(PT_BR);
        RecordingTikTokClientFacade facade = new RecordingTikTokClientFacade();
        ReinodoceCoreService service = new ReinodoceCoreService(repository, minecraftLanguage::get, facade);
        service.currentConfig();
        facade.reset();
        ReinodoceConfig updated = ReinodoceConfig.defaults();
        updated.setLanguage(DE_DE);
        repository.save(updated);

        CommandResult result = service.reload();

        assertTrue(result.success());
        assertEquals(1, facade.configUpdates());
        assertEquals(1, facade.reconnects());
    }

    @Test
    void reloadDoesNotReconnectWhenEffectiveLanguageStaysTheSame() {
        Path configFile = tempDir.resolve("reload-language-unchanged.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        AtomicReference<String> minecraftLanguage = new AtomicReference<>(PT_BR);
        RecordingTikTokClientFacade facade = new RecordingTikTokClientFacade();
        ReinodoceCoreService service = new ReinodoceCoreService(repository, minecraftLanguage::get, facade);
        service.currentConfig();
        facade.reset();
        ReinodoceConfig updated = ReinodoceConfig.defaults();
        updated.setLanguage(PT_BR);
        repository.save(updated);

        CommandResult result = service.reload();

        assertTrue(result.success());
        assertEquals(1, facade.configUpdates());
        assertEquals(0, facade.reconnects());
    }

    @Test
    void replaceConfigReconnectsWhenEffectiveLanguageChanges() {
        Path configFile = tempDir.resolve("replace-language-changed.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        AtomicReference<String> minecraftLanguage = new AtomicReference<>(PT_BR);
        RecordingTikTokClientFacade facade = new RecordingTikTokClientFacade();
        ReinodoceCoreService service = new ReinodoceCoreService(repository, minecraftLanguage::get, facade);
        service.currentConfig();
        facade.reset();
        ReinodoceConfig draft = ReinodoceConfig.defaults();
        draft.setLanguage(DE_DE);

        CommandResult result = service.replaceConfig(draft);

        assertTrue(result.success());
        assertEquals(1, facade.configUpdates());
        assertEquals(1, facade.reconnects());
    }

    @Test
    void replaceConfigDoesNotReconnectWhenEffectiveLanguageStaysTheSame() {
        Path configFile = tempDir.resolve("replace-language-unchanged.json");
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        AtomicReference<String> minecraftLanguage = new AtomicReference<>(PT_BR);
        RecordingTikTokClientFacade facade = new RecordingTikTokClientFacade();
        ReinodoceCoreService service = new ReinodoceCoreService(repository, minecraftLanguage::get, facade);
        service.currentConfig();
        facade.reset();
        ReinodoceConfig draft = ReinodoceConfig.defaults();
        draft.setLanguage(PT_BR);

        CommandResult result = service.replaceConfig(draft);

        assertTrue(result.success());
        assertEquals(1, facade.configUpdates());
        assertEquals(0, facade.reconnects());
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
        draft.setLanguage("ja-JP");
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
        assertEquals("ja_jp", loaded.getLanguage());
        assertEquals(50, loaded.getSyntheticGiftMinValue());
        assertEquals("single", loaded.getSyntheticGiftComboMode());
        assertTrue(loaded.isSyntheticFollowEnabled());
        assertTrue(loaded.isSyntheticJoinEnabled());
        assertTrue(loaded.isSyntheticMemberLevelEnabled());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(2, loaded.getRuleMinMemberLevel());
    }

    private static final class RecordingTikTokClientFacade extends TikTokClientFacade {
        private int configUpdateCount;
        private int reconnectCount;

        RecordingTikTokClientFacade() {
            super(
                    ReinodoceConfig::defaults,
                    new TikTokRuntimeServices(
                            ChatEventSink.noop(),
                            new SessionEventLogger(Path.of("build/test-session-logs/core-service")),
                            new AlertService(AlertSink.noop())),
                    new MessageRuleEngine(),
                    new MemberLevelResolver(),
                    new MessageDeduplicator(Duration.ofSeconds(1)));
        }

        @Override
        public void onConfigUpdated() {
            configUpdateCount++;
        }

        @Override
        public void reconnectForConfigChange() {
            reconnectCount++;
        }

        void reset() {
            configUpdateCount = 0;
            reconnectCount = 0;
        }

        int configUpdates() {
            return configUpdateCount;
        }

        int reconnects() {
            return reconnectCount;
        }
    }
}

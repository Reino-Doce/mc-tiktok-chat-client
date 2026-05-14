package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void guiDraftSavePersistsExpandedSettingsDomains() {
        ReinodoceClientService service = service(new HudMessageStore());
        ReinodoceConfig draft = service.currentConfig();
        configureExpandedDraft(draft);

        CommandResult result = service.saveSettingsDraft(draft);

        assertTrue(result.success());
        assertExpandedDraft(service.currentConfig());
    }

    private ReinodoceClientService service(HudMessageStore hudMessageStore) {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve("config.json"));
        ReinodoceCoreService coreService = new ReinodoceCoreService(
                ChatEventSink.noop(),
                repository,
                () -> EN_US,
                tempDir.resolve("logs"));
        return new ReinodoceClientService(
                new RecordingPlatformBridge(),
                coreService,
                new ClientOverlayServices(hudMessageStore));
    }

    private static void configureExpandedDraft(ReinodoceConfig draft) {
        draft.setLastUsername("streamer");
        draft.setAutoConnectOnStart(true);
        draft.setReconnectSeconds(12);
        draft.setOutputMode(HUD_MODE);
        draft.setHudPosition("bottom-right");
        draft.setHudLines(5);
        draft.setPinnedOverlayEnabled(false);
        draft.setPinnedOverlayPosition("bottom-left");
        draft.setPinnedMessagesInOutput(true);
        draft.setPinnedOverlayMessages(2);
        draft.setChatLogEnabled(true);
        draft.setChatPrefix("[TikTok]");
        draft.setChatFormat("{prefix} <{username}> {message}");
        draft.setChatEmotesEnabled(false);
        draft.setRuleFollowerOnly(true);
        draft.setRuleMinMemberLevel(3);
        draft.setRuleBlockedWords(List.of("spam", "caps"));
        draft.setRuleBlockedUsers(List.of("alice", "bob"));
        draft.setRuleMaxMessageLength(80);
        draft.setRuleDuplicateCooldownSeconds(9);
        draft.setSyntheticGiftMinValue(7);
        draft.setSyntheticGiftComboMode("single");
        draft.setSyntheticFollowEnabled(true);
        draft.setSyntheticJoinEnabled(true);
        draft.setSyntheticMemberLevelEnabled(true);
        draft.setAlertGiftMinValue(25);
        draft.setAlertSoundEnabled(AlertEventType.GIFT, true);
        draft.setAlertSoundId(AlertEventType.GIFT, "minecraft:entity.experience_orb.pickup");
        draft.setAlertToastEnabled(AlertEventType.GIFT, true);
        draft.setAlertToastTemplate(AlertEventType.GIFT, "{username} sent {giftName}");
        draft.setAlertMediaMode(AlertEventType.GIFT, "gift");
        draft.setAlertCustomImage(AlertEventType.GIFT, "resource://reinodoce/gift");
        draft.setAlertSoundEnabled(AlertEventType.FOLLOW, true);
        draft.setAlertToastEnabled(AlertEventType.JOIN, true);
        draft.setAlertMediaMode(AlertEventType.MEMBER_LEVEL, "profile");
        draft.setLanguage("pt_br");
        draft.setSessionLoggingEnabled(true);
        draft.setSessionLoggingFormat("text");
    }

    private static void assertExpandedDraft(ReinodoceConfig saved) {
        assertEquals("streamer", saved.getLastUsername());
        assertTrue(saved.isAutoConnectOnStart());
        assertEquals(12, saved.getReconnectSeconds());
        assertEquals(HUD_MODE, saved.getOutputMode());
        assertEquals("bottom-right", saved.getHudPosition());
        assertEquals(5, saved.getHudLines());
        assertFalse(saved.isPinnedOverlayEnabled());
        assertEquals("bottom-left", saved.getPinnedOverlayPosition());
        assertTrue(saved.isPinnedMessagesInOutput());
        assertEquals(2, saved.getPinnedOverlayMessages());
        assertTrue(saved.isChatLogEnabled());
        assertEquals("[TikTok]", saved.getChatPrefix());
        assertEquals("{prefix} <{username}> {message}", saved.getChatFormat());
        assertFalse(saved.isChatEmotesEnabled());
        assertTrue(saved.isRuleFollowerOnly());
        assertEquals(3, saved.getRuleMinMemberLevel());
        assertEquals(List.of("spam", "caps"), saved.getRuleBlockedWords());
        assertEquals(List.of("alice", "bob"), saved.getRuleBlockedUsers());
        assertEquals(80, saved.getRuleMaxMessageLength());
        assertEquals(9, saved.getRuleDuplicateCooldownSeconds());
        assertEquals(7, saved.getSyntheticGiftMinValue());
        assertEquals("single", saved.getSyntheticGiftComboMode());
        assertTrue(saved.isSyntheticFollowEnabled());
        assertTrue(saved.isSyntheticJoinEnabled());
        assertTrue(saved.isSyntheticMemberLevelEnabled());
        assertEquals(25, saved.getAlertGiftMinValue());
        assertTrue(saved.isAlertSoundEnabled(AlertEventType.GIFT));
        assertEquals("minecraft:entity.experience_orb.pickup", saved.getAlertSoundId(AlertEventType.GIFT));
        assertTrue(saved.isAlertToastEnabled(AlertEventType.GIFT));
        assertEquals("{username} sent {giftName}", saved.getAlertToastTemplate(AlertEventType.GIFT));
        assertEquals("gift", saved.getAlertMediaMode(AlertEventType.GIFT));
        assertEquals("resource://reinodoce/gift", saved.getAlertCustomImage(AlertEventType.GIFT));
        assertTrue(saved.isAlertSoundEnabled(AlertEventType.FOLLOW));
        assertTrue(saved.isAlertToastEnabled(AlertEventType.JOIN));
        assertEquals("profile", saved.getAlertMediaMode(AlertEventType.MEMBER_LEVEL));
        assertEquals("pt_br", saved.getLanguage());
        assertTrue(saved.isSessionLoggingEnabled());
        assertEquals("text", saved.getSessionLoggingFormat());
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

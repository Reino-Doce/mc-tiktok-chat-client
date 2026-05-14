package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceConfigRepositoryTest {
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String UNKNOWN = "unknown";
    private static final List<String> BLOCKED_USERS = List.of(ALICE, BOB);
    private static final List<String> BLOCKED_WORDS = List.of("spam", "ads");
    private static final String DEFAULT_SOUND_ID = "default";
    private static final String CUSTOM_SOUND_ID = "minecraft:entity.experience_orb.pickup";
    private static final String CUSTOM_TEMPLATE = "{username} sent {giftName} x{count} ({diamonds})";
    private static final String CUSTOM_IMAGE = "https://cdn.example/alert.png";

    @TempDir
    Path tempDir;

    @Test
    void missingFileFallsBackToDefaults() {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve("missing.json"));

        ReinodoceConfig loaded = repository.load();

        assertEquals("", loaded.getLastUsername());
        assertFalse(loaded.isAutoConnectOnStart());
        assertEquals("[LIVE]", loaded.getChatPrefix());
        assertEquals("{prefix}  <{username}> {message}", loaded.getChatFormat());
        assertEquals(5, loaded.getReconnectSeconds());
        assertEquals(0, loaded.getRuleMinMemberLevel());
        assertTrue(loaded.getRuleBlockedWords().isEmpty());
        assertTrue(loaded.getRuleBlockedUsers().isEmpty());
        assertEquals(0, loaded.getRuleMaxMessageLength());
        assertEquals(0, loaded.getRuleDuplicateCooldownSeconds());
        assertEquals(1, loaded.getSyntheticGiftMinValue());
        assertEquals("bulk", loaded.getSyntheticGiftComboMode());
        assertFalse(loaded.isSyntheticFollowEnabled());
        assertFalse(loaded.isSyntheticJoinEnabled());
        assertFalse(loaded.isSyntheticMemberLevelEnabled());
        assertDefaultAlerts(loaded);
        assertDefaultOutputSettings(loaded);
        assertFalse(loaded.isRuleFollowerOnly());
    }

    @Test
    void settersApplyDocumentedSanitizationRules() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        configureInvalidSanitizationValues(config);

        assertEquals("streamer", config.getLastUsername());
        assertEquals(0, config.getReconnectSeconds());
        assertEquals(0, config.getRuleMinMemberLevel());
        assertEquals(BLOCKED_WORDS, config.getRuleBlockedWords());
        assertEquals(BLOCKED_USERS, config.getRuleBlockedUsers());
        assertEquals(0, config.getRuleMaxMessageLength());
        assertEquals(0, config.getRuleDuplicateCooldownSeconds());
        assertEquals(0, config.getSyntheticGiftMinValue());
        assertEquals("bulk", config.getSyntheticGiftComboMode());
        assertSanitizedAlertSettings(config);
        assertEquals(0, config.getAlertGiftMinValue());
        assertEquals("chat", config.getOutputMode());
        assertEquals("top-left", config.getHudPosition());
        assertEquals(12, config.getHudLines());
        assertEquals("jsonl", config.getSessionLoggingFormat());
        assertEquals("[LIVE]", config.getChatPrefix());
        assertEquals("{prefix}  <{username}> {message}", config.getChatFormat());
        assertEquals("pt_br", config.getLanguage());

        config.setLanguage("not a locale");

        assertEquals("auto", config.getLanguage());
    }

    private static void configureInvalidSanitizationValues(ReinodoceConfig config) {
        config.setLastUsername("  streamer  ");
        config.setReconnectSeconds(-1);
        config.setRuleMinMemberLevel(-2);
        config.setRuleBlockedWords(List.of(" Spam ", "", "SPAM", "Ads"));
        config.setRuleBlockedUsers(List.of("@Alice", "bad user", "ALICE", BOB));
        config.setRuleMaxMessageLength(-4);
        config.setRuleDuplicateCooldownSeconds(-5);
        config.setSyntheticGiftMinValue(-3);
        config.setSyntheticGiftComboMode(UNKNOWN);
        configureInvalidAlertSoundIds(config);
        config.setAlertToastTemplate(AlertEventType.GIFT, "{username} {bad}");
        config.setAlertMediaMode(AlertEventType.GIFT, UNKNOWN);
        config.setAlertCustomImage(AlertEventType.GIFT, " \n ");
        config.setAlertGiftMinValue(-7);
        config.setOutputMode(UNKNOWN);
        config.setHudPosition(UNKNOWN);
        config.setHudLines(99);
        config.setSessionLoggingFormat(UNKNOWN);
        config.setChatPrefix("   ");
        config.setChatFormat("{username}: {message}");
        config.setLanguage(" PT-BR ");
    }

    @Test
    void chatFormatRejectsUnknownTokens() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        config.setChatFormat("{prefix} <{username}> {message} {bad}");

        assertEquals("{prefix}  <{username}> {message}", config.getChatFormat());
    }

    @Test
    void saveAndLoadRoundTripPreservesDocumentedFields() throws IOException {
        Path configFile = tempDir.resolve("config").resolve(ReinodoceConfigRepository.DEFAULT_FILE_NAME);
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        configureRoundTripConfig(config);

        repository.save(config);
        String savedJson = Files.readString(configFile);
        ReinodoceConfig loaded = repository.load();

        assertCorrectedSyntheticFieldNames(savedJson);
        assertRoundTripRules(loaded);
        assertRoundTripSyntheticSettings(loaded);
        assertRoundTripAlertSettings(loaded);
        assertEquals("hud", loaded.getOutputMode());
        assertEquals("bottom-right", loaded.getHudPosition());
        assertEquals(4, loaded.getHudLines());
        assertEquals("[RD]", loaded.getChatPrefix());
        assertEquals("{prefix} {username}: {message}", loaded.getChatFormat());
        assertFalse(loaded.isChatEmotesEnabled());
        assertTrue(loaded.isChatLogEnabled());
        assertEquals("pt_br", loaded.getLanguage());
        assertTrue(loaded.isSessionLoggingEnabled());
        assertEquals("text", loaded.getSessionLoggingFormat());
    }

    @Test
    void oldConfigWithoutLanguageLoadsAsAuto() throws IOException {
        Path configFile = tempDir.resolve("old-config.json");
        Files.writeString(configFile, """
                {
                  "lastUsername": "alice",
                  "chatPrefix": "[OLD]"
                }
                """);
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);

        ReinodoceConfig loaded = repository.load();

        assertEquals(ALICE, loaded.getLastUsername());
        assertEquals("[OLD]", loaded.getChatPrefix());
        assertEquals("auto", loaded.getLanguage());
    }

    @Test
    void invalidAlertSoundIdsLoadAsDefault() throws IOException {
        Path configFile = tempDir.resolve("invalid-alert-sound-id.json");
        Files.writeString(configFile, """
                {
                  "alertGiftSoundId": "bad sound",
                  "alertFollowSoundId": "minecraft:entity.experience_orb.pickup",
                  "alertJoinSoundId": "",
                  "alertMemberLevelSoundId": null
                }
                """);
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);

        ReinodoceConfig loaded = repository.load();

        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.GIFT));
        assertEquals(CUSTOM_SOUND_ID, loaded.getAlertSoundId(AlertEventType.FOLLOW));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.JOIN));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.MEMBER_LEVEL));
    }

    private static void configureInvalidAlertSoundIds(ReinodoceConfig config) {
        config.setAlertSoundId(AlertEventType.GIFT, " Minecraft:Entity.Experience_Orb.Pickup ");
        config.setAlertSoundId(AlertEventType.FOLLOW, "bad sound");
    }

    private static void assertSanitizedAlertSoundIds(ReinodoceConfig config) {
        assertEquals(CUSTOM_SOUND_ID, config.getAlertSoundId(AlertEventType.GIFT));
        assertEquals(DEFAULT_SOUND_ID, config.getAlertSoundId(AlertEventType.FOLLOW));
    }

    private static void assertSanitizedAlertSettings(ReinodoceConfig config) {
        assertSanitizedAlertSoundIds(config);
        assertEquals("", config.getAlertToastTemplate(AlertEventType.GIFT));
        assertEquals("none", config.getAlertMediaMode(AlertEventType.GIFT));
        assertEquals("", config.getAlertCustomImage(AlertEventType.GIFT));
    }

    private void configureRoundTripConfig(ReinodoceConfig config) {
        config.setLastUsername(ALICE);
        config.setAutoConnectOnStart(true);
        config.setReconnectSeconds(9);
        config.setRuleFollowerOnly(true);
        config.setRuleMinMemberLevel(3);
        config.setRuleBlockedWords(BLOCKED_WORDS);
        config.setRuleBlockedUsers(BLOCKED_USERS);
        config.setRuleMaxMessageLength(160);
        config.setRuleDuplicateCooldownSeconds(3);
        config.setSyntheticGiftMinValue(5);
        config.setSyntheticGiftComboMode("single");
        config.setSyntheticFollowEnabled(true);
        config.setSyntheticJoinEnabled(true);
        config.setSyntheticMemberLevelEnabled(true);
        config.setAlertSoundEnabled(AlertEventType.GIFT, true);
        config.setAlertSoundId(AlertEventType.GIFT, CUSTOM_SOUND_ID);
        config.setAlertToastEnabled(AlertEventType.GIFT, true);
        config.setAlertToastTemplate(AlertEventType.GIFT, CUSTOM_TEMPLATE);
        config.setAlertMediaMode(AlertEventType.GIFT, "gift");
        config.setAlertCustomImage(AlertEventType.GIFT, CUSTOM_IMAGE);
        config.setAlertGiftMinValue(100);
        config.setAlertToastEnabled(AlertEventType.FOLLOW, true);
        config.setAlertMediaMode(AlertEventType.FOLLOW, "profile");
        config.setAlertSoundEnabled(AlertEventType.JOIN, true);
        config.setAlertSoundId(AlertEventType.JOIN, "minecraft:block.note_block.pling");
        config.setAlertToastTemplate(AlertEventType.JOIN, "{username} joined");
        config.setAlertToastEnabled(AlertEventType.MEMBER_LEVEL, true);
        config.setAlertSoundId(AlertEventType.MEMBER_LEVEL, "reinodoce_mctiktok:member_level");
        config.setAlertCustomImage(AlertEventType.MEMBER_LEVEL, "default");
        config.setOutputMode("hud");
        config.setHudPosition("bottom-right");
        config.setHudLines(4);
        config.setChatPrefix("[RD]");
        config.setChatFormat("{prefix} {username}: {message}");
        config.setChatEmotesEnabled(false);
        config.setChatLogEnabled(true);
        config.setLanguage("pt-BR");
        config.setSessionLoggingEnabled(true);
        config.setSessionLoggingFormat("text");
    }

    private void assertRoundTripRules(ReinodoceConfig loaded) {
        assertEquals(ALICE, loaded.getLastUsername());
        assertTrue(loaded.isAutoConnectOnStart());
        assertEquals(9, loaded.getReconnectSeconds());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(3, loaded.getRuleMinMemberLevel());
        assertEquals(BLOCKED_WORDS, loaded.getRuleBlockedWords());
        assertEquals(BLOCKED_USERS, loaded.getRuleBlockedUsers());
        assertEquals(160, loaded.getRuleMaxMessageLength());
        assertEquals(3, loaded.getRuleDuplicateCooldownSeconds());
    }

    private void assertRoundTripSyntheticSettings(ReinodoceConfig loaded) {
        assertEquals(5, loaded.getSyntheticGiftMinValue());
        assertEquals("single", loaded.getSyntheticGiftComboMode());
        assertTrue(loaded.isSyntheticFollowEnabled());
        assertTrue(loaded.isSyntheticJoinEnabled());
        assertTrue(loaded.isSyntheticMemberLevelEnabled());
    }

    private void assertRoundTripAlertSettings(ReinodoceConfig loaded) {
        assertTrue(loaded.isAlertSoundEnabled(AlertEventType.GIFT));
        assertEquals(CUSTOM_SOUND_ID, loaded.getAlertSoundId(AlertEventType.GIFT));
        assertTrue(loaded.isAlertToastEnabled(AlertEventType.GIFT));
        assertEquals(CUSTOM_TEMPLATE, loaded.getAlertToastTemplate(AlertEventType.GIFT));
        assertEquals("gift", loaded.getAlertMediaMode(AlertEventType.GIFT));
        assertEquals(CUSTOM_IMAGE, loaded.getAlertCustomImage(AlertEventType.GIFT));
        assertEquals(100, loaded.getAlertGiftMinValue());
        assertFalse(loaded.isAlertSoundEnabled(AlertEventType.FOLLOW));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.FOLLOW));
        assertTrue(loaded.isAlertToastEnabled(AlertEventType.FOLLOW));
        assertEquals("profile", loaded.getAlertMediaMode(AlertEventType.FOLLOW));
        assertTrue(loaded.isAlertSoundEnabled(AlertEventType.JOIN));
        assertEquals("minecraft:block.note_block.pling", loaded.getAlertSoundId(AlertEventType.JOIN));
        assertFalse(loaded.isAlertToastEnabled(AlertEventType.JOIN));
        assertEquals("{username} joined", loaded.getAlertToastTemplate(AlertEventType.JOIN));
        assertFalse(loaded.isAlertSoundEnabled(AlertEventType.MEMBER_LEVEL));
        assertEquals("reinodoce_mctiktok:member_level", loaded.getAlertSoundId(AlertEventType.MEMBER_LEVEL));
        assertTrue(loaded.isAlertToastEnabled(AlertEventType.MEMBER_LEVEL));
        assertEquals("", loaded.getAlertCustomImage(AlertEventType.MEMBER_LEVEL));
    }

    private void assertDefaultAlerts(ReinodoceConfig loaded) {
        assertFalse(loaded.isAlertSoundEnabled(AlertEventType.GIFT));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.GIFT));
        assertFalse(loaded.isAlertToastEnabled(AlertEventType.GIFT));
        assertEquals("", loaded.getAlertToastTemplate(AlertEventType.GIFT));
        assertEquals("none", loaded.getAlertMediaMode(AlertEventType.GIFT));
        assertEquals("", loaded.getAlertCustomImage(AlertEventType.GIFT));
        assertEquals(1, loaded.getAlertGiftMinValue());
        assertFalse(loaded.isAlertSoundEnabled(AlertEventType.FOLLOW));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.FOLLOW));
        assertFalse(loaded.isAlertToastEnabled(AlertEventType.FOLLOW));
        assertFalse(loaded.isAlertSoundEnabled(AlertEventType.JOIN));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.JOIN));
        assertFalse(loaded.isAlertToastEnabled(AlertEventType.JOIN));
        assertFalse(loaded.isAlertSoundEnabled(AlertEventType.MEMBER_LEVEL));
        assertEquals(DEFAULT_SOUND_ID, loaded.getAlertSoundId(AlertEventType.MEMBER_LEVEL));
        assertFalse(loaded.isAlertToastEnabled(AlertEventType.MEMBER_LEVEL));
    }

    private void assertDefaultOutputSettings(ReinodoceConfig loaded) {
        assertEquals("chat", loaded.getOutputMode());
        assertEquals("top-left", loaded.getHudPosition());
        assertEquals(6, loaded.getHudLines());
        assertTrue(loaded.isChatEmotesEnabled());
        assertFalse(loaded.isChatLogEnabled());
        assertEquals("auto", loaded.getLanguage());
        assertFalse(loaded.isSessionLoggingEnabled());
        assertEquals("jsonl", loaded.getSessionLoggingFormat());
    }

    private void assertCorrectedSyntheticFieldNames(String savedJson) {
        assertTrue(savedJson.contains("\"syntheticGiftMinValue\""));
        assertTrue(savedJson.contains("\"syntheticGiftComboMode\""));
        assertTrue(savedJson.contains("\"syntheticFollowEnabled\""));
        assertTrue(savedJson.contains("\"syntheticJoinEnabled\""));
        assertTrue(savedJson.contains("\"syntheticMemberLevelEnabled\""));
        assertTrue(savedJson.contains("\"alertGiftSoundEnabled\""));
        assertTrue(savedJson.contains("\"alertGiftSoundId\""));
        assertTrue(savedJson.contains("\"alertGiftToastEnabled\""));
        assertTrue(savedJson.contains("\"alertGiftToastTemplate\""));
        assertTrue(savedJson.contains("\"alertGiftMediaMode\""));
        assertTrue(savedJson.contains("\"alertGiftCustomImage\""));
        assertTrue(savedJson.contains("\"alertGiftMinValue\""));
        assertTrue(savedJson.contains("\"alertFollowSoundId\""));
        assertTrue(savedJson.contains("\"alertFollowToastEnabled\""));
        assertTrue(savedJson.contains("\"alertFollowMediaMode\""));
        assertTrue(savedJson.contains("\"alertJoinSoundEnabled\""));
        assertTrue(savedJson.contains("\"alertJoinSoundId\""));
        assertTrue(savedJson.contains("\"alertJoinToastTemplate\""));
        assertTrue(savedJson.contains("\"alertMemberLevelSoundId\""));
        assertTrue(savedJson.contains("\"alertMemberLevelToastEnabled\""));
        assertTrue(savedJson.contains("\"alertMemberLevelCustomImage\""));
        assertTrue(savedJson.contains("\"outputMode\""));
        assertTrue(savedJson.contains("\"hudPosition\""));
        assertTrue(savedJson.contains("\"hudLines\""));
        assertTrue(savedJson.contains("\"language\""));
    }
}

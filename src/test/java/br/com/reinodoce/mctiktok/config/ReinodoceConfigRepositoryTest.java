package br.com.reinodoce.mctiktok.config;

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
    private static final List<String> BLOCKED_USERS = List.of(ALICE, BOB);
    private static final List<String> BLOCKED_WORDS = List.of("spam", "ads");

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
        assertTrue(loaded.isChatEmotesEnabled());
        assertFalse(loaded.isChatLogEnabled());
        assertFalse(loaded.isRuleFollowerOnly());
    }

    @Test
    void settersApplyDocumentedSanitizationRules() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        config.setLastUsername("  streamer  ");
        config.setReconnectSeconds(-1);
        config.setRuleMinMemberLevel(-2);
        config.setRuleBlockedWords(List.of(" Spam ", "", "SPAM", "Ads"));
        config.setRuleBlockedUsers(List.of("@Alice", "bad user", "ALICE", BOB));
        config.setRuleMaxMessageLength(-4);
        config.setRuleDuplicateCooldownSeconds(-5);
        config.setSyntheticGiftMinValue(-3);
        config.setSyntheticGiftComboMode("unknown");
        config.setChatPrefix("   ");
        config.setChatFormat("{username}: {message}");

        assertEquals("streamer", config.getLastUsername());
        assertEquals(0, config.getReconnectSeconds());
        assertEquals(0, config.getRuleMinMemberLevel());
        assertEquals(BLOCKED_WORDS, config.getRuleBlockedWords());
        assertEquals(BLOCKED_USERS, config.getRuleBlockedUsers());
        assertEquals(0, config.getRuleMaxMessageLength());
        assertEquals(0, config.getRuleDuplicateCooldownSeconds());
        assertEquals(0, config.getSyntheticGiftMinValue());
        assertEquals("bulk", config.getSyntheticGiftComboMode());
        assertEquals("[LIVE]", config.getChatPrefix());
        assertEquals("{prefix}  <{username}> {message}", config.getChatFormat());
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
        assertEquals("[RD]", loaded.getChatPrefix());
        assertEquals("{prefix} {username}: {message}", loaded.getChatFormat());
        assertFalse(loaded.isChatEmotesEnabled());
        assertTrue(loaded.isChatLogEnabled());
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
        config.setChatPrefix("[RD]");
        config.setChatFormat("{prefix} {username}: {message}");
        config.setChatEmotesEnabled(false);
        config.setChatLogEnabled(true);
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

    private void assertCorrectedSyntheticFieldNames(String savedJson) {
        assertTrue(savedJson.contains("\"syntheticGiftMinValue\""));
        assertTrue(savedJson.contains("\"syntheticGiftComboMode\""));
        assertTrue(savedJson.contains("\"syntheticFollowEnabled\""));
        assertTrue(savedJson.contains("\"syntheticJoinEnabled\""));
        assertTrue(savedJson.contains("\"syntheticMemberLevelEnabled\""));
    }
}

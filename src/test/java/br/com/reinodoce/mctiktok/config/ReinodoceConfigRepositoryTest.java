package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceConfigRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileFallsBackToDefaults() {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve("missing.json"));

        ReinodoceConfig loaded = repository.load();

        assertEquals("", loaded.getLastUsername());
        assertEquals("[LIVE]", loaded.getChatPrefix());
        assertEquals("{prefix}  <{username}> {message}", loaded.getChatFormat());
        assertEquals(5, loaded.getReconnectSeconds());
        assertEquals(0, loaded.getRuleMinMemberLevel());
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
        config.setSyntheticGiftMinValue(-3);
        config.setSyntheticGiftComboMode("unknown");
        config.setChatPrefix("   ");
        config.setChatFormat("{username}: {message}");

        assertEquals("streamer", config.getLastUsername());
        assertEquals(0, config.getReconnectSeconds());
        assertEquals(0, config.getRuleMinMemberLevel());
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
        config.setLastUsername("alice");
        config.setReconnectSeconds(9);
        config.setRuleFollowerOnly(true);
        config.setRuleMinMemberLevel(3);
        config.setSyntheticGiftMinValue(5);
        config.setSyntheticGiftComboMode("single");
        config.setSyntheticFollowEnabled(true);
        config.setSyntheticJoinEnabled(true);
        config.setSyntheticMemberLevelEnabled(true);
        config.setChatPrefix("[RD]");
        config.setChatFormat("{prefix} {username}: {message}");
        config.setChatEmotesEnabled(false);
        config.setChatLogEnabled(true);

        repository.save(config);
        String savedJson = Files.readString(configFile);
        ReinodoceConfig loaded = repository.load();

        assertCorrectedSyntheticFieldNames(savedJson);
        assertEquals("alice", loaded.getLastUsername());
        assertEquals(9, loaded.getReconnectSeconds());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(3, loaded.getRuleMinMemberLevel());
        assertEquals(5, loaded.getSyntheticGiftMinValue());
        assertEquals("single", loaded.getSyntheticGiftComboMode());
        assertTrue(loaded.isSyntheticFollowEnabled());
        assertTrue(loaded.isSyntheticJoinEnabled());
        assertTrue(loaded.isSyntheticMemberLevelEnabled());
        assertEquals("[RD]", loaded.getChatPrefix());
        assertEquals("{prefix} {username}: {message}", loaded.getChatFormat());
        assertFalse(loaded.isChatEmotesEnabled());
        assertTrue(loaded.isChatLogEnabled());
    }

    private void assertCorrectedSyntheticFieldNames(String savedJson) {
        assertTrue(savedJson.contains("\"syntheticGiftMinValue\""));
        assertTrue(savedJson.contains("\"syntheticGiftComboMode\""));
        assertTrue(savedJson.contains("\"syntheticFollowEnabled\""));
        assertTrue(savedJson.contains("\"syntheticJoinEnabled\""));
        assertTrue(savedJson.contains("\"syntheticMemberLevelEnabled\""));
    }
}

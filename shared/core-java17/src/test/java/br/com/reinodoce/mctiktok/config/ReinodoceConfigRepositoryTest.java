package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceConfigRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTripPreservesRelevantSettings() {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);

        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setLastUsername("streamer");
        config.setReconnectSeconds(5);
        config.setRuleFollowerOnly(true);
        config.setRuleMinMemberLevel(2);
        config.setSyntheticGiftMinValue(10);
        config.setSyntheticGiftComboMode("single");
        config.setSyntheticFollowEnabled(false);
        config.setSyntheticJoinEnabled(false);
        config.setSyntheticMemberLevelEnabled(true);
        config.setChatLogEnabled(true);
        config.setChatPrefix("[LIVE]");

        repository.save(config);
        String savedJson = Files.readString(tempDir.resolve("reinodoce-mc-tiktok-client.json"), StandardCharsets.UTF_8);
        ReinodoceConfig loaded = repository.load();

        assertTrue(savedJson.contains("\"syntheticGiftMinValue\""));
        assertTrue(savedJson.contains("\"syntheticGiftComboMode\""));
        assertTrue(savedJson.contains("\"syntheticFollowEnabled\""));
        assertTrue(savedJson.contains("\"syntheticJoinEnabled\""));
        assertTrue(savedJson.contains("\"syntheticMemberLevelEnabled\""));
        assertFalse(savedJson.contains("\"synteticGiftMinValue\""));
        assertFalse(savedJson.contains("\"synteticGiftComboMode\""));
        assertFalse(savedJson.contains("\"synteticFollowEnabled\""));
        assertFalse(savedJson.contains("\"synteticJoinEnabled\""));
        assertFalse(savedJson.contains("\"synteticMemberLevelEnabled\""));
        assertEquals("streamer", loaded.getLastUsername());
        assertEquals(5, loaded.getReconnectSeconds());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(2, loaded.getRuleMinMemberLevel());
        assertEquals(10, loaded.getSyntheticGiftMinValue());
        assertEquals("single", loaded.getSyntheticGiftComboMode());
        assertFalse(loaded.isSyntheticFollowEnabled());
        assertFalse(loaded.isSyntheticJoinEnabled());
        assertTrue(loaded.isSyntheticMemberLevelEnabled());
        assertTrue(loaded.isChatLogEnabled());
        assertEquals("[LIVE]", loaded.getChatPrefix());
    }

    @Test
    void loadLegacySyntheticFieldNamesMigratesToCorrectedNames() throws IOException {
        Path file = tempDir.resolve("reinodoce-mc-tiktok-client.json");
        Files.writeString(file, """
                {
                  "synteticGiftMinValue": 7,
                  "synteticGiftComboMode": "single",
                  "synteticFollowEnabled": true,
                  "synteticJoinEnabled": true,
                  "synteticMemberLevelEnabled": true
                }
                """, StandardCharsets.UTF_8);

        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);
        ReinodoceConfig loaded = repository.load();

        assertEquals(7, loaded.getSyntheticGiftMinValue());
        assertEquals("single", loaded.getSyntheticGiftComboMode());
        assertTrue(loaded.isSyntheticFollowEnabled());
        assertTrue(loaded.isSyntheticJoinEnabled());
        assertTrue(loaded.isSyntheticMemberLevelEnabled());

        repository.save(loaded);
        String savedJson = Files.readString(file, StandardCharsets.UTF_8);

        assertTrue(savedJson.contains("\"syntheticGiftMinValue\""));
        assertFalse(savedJson.contains("\"synteticGiftMinValue\""));
    }

    @Test
    void loadMalformedJsonFallsBackToDefaults() throws IOException {
        Path file = tempDir.resolve("reinodoce-mc-tiktok-client.json");
        Files.writeString(file, "{ malformed", StandardCharsets.UTF_8);

        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);
        ReinodoceConfig loaded = repository.load();

        assertEquals("", loaded.getLastUsername());
        assertEquals(5, loaded.getReconnectSeconds());
        assertEquals("bulk", loaded.getSyntheticGiftComboMode());
        assertFalse(loaded.isSyntheticFollowEnabled());
        assertFalse(loaded.isSyntheticJoinEnabled());
        assertFalse(loaded.isSyntheticMemberLevelEnabled());
        assertFalse(loaded.isChatLogEnabled());
        assertEquals("[LIVE]", loaded.getChatPrefix());
    }

    @Test
    void loadExistingZeroReconnectPreservesDisabledState() throws IOException {
        Path file = tempDir.resolve("reinodoce-mc-tiktok-client.json");
        Files.writeString(file, """
                {
                  "lastUsername": "streamer",
                  "reconnectSeconds": 0,
                  "syntheticGiftComboMode": "bulk",
                  "chatPrefix": "[LIVE]"
                }
                """, StandardCharsets.UTF_8);

        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);
        ReinodoceConfig loaded = repository.load();

        assertEquals("streamer", loaded.getLastUsername());
        assertEquals(0, loaded.getReconnectSeconds());
    }

    @Test
    void loadPartialJsonPreservesDocumentedDefaults() throws IOException {
        Path file = tempDir.resolve("reinodoce-mc-tiktok-client.json");
        Files.writeString(file, """
                {
                  "lastUsername": "streamer"
                }
                """, StandardCharsets.UTF_8);

        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);
        ReinodoceConfig loaded = repository.load();

        assertEquals("streamer", loaded.getLastUsername());
        assertEquals(5, loaded.getReconnectSeconds());
        assertEquals(1, loaded.getSyntheticGiftMinValue());
        assertEquals("bulk", loaded.getSyntheticGiftComboMode());
        assertTrue(loaded.isChatEmotesEnabled());
        assertFalse(loaded.isChatLogEnabled());
        assertEquals("[LIVE]", loaded.getChatPrefix());
    }
}

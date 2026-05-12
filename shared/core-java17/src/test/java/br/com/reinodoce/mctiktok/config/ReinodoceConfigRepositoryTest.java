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
        config.setSynteticGiftMinValue(10);
        config.setSynteticGiftComboMode("single");
        config.setSynteticFollowEnabled(false);
        config.setSynteticJoinEnabled(false);
        config.setSynteticMemberLevelEnabled(true);
        config.setChatPrefix("[LIVE]");

        repository.save(config);
        ReinodoceConfig loaded = repository.load();

        assertEquals("streamer", loaded.getLastUsername());
        assertEquals(5, loaded.getReconnectSeconds());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(2, loaded.getRuleMinMemberLevel());
        assertEquals(10, loaded.getSynteticGiftMinValue());
        assertEquals("single", loaded.getSynteticGiftComboMode());
        assertFalse(loaded.isSynteticFollowEnabled());
        assertFalse(loaded.isSynteticJoinEnabled());
        assertTrue(loaded.isSynteticMemberLevelEnabled());
        assertEquals("[LIVE]", loaded.getChatPrefix());
    }

    @Test
    void loadMalformedJsonFallsBackToDefaults() throws IOException {
        Path file = tempDir.resolve("reinodoce-mc-tiktok-client.json");
        Files.writeString(file, "{ malformed", StandardCharsets.UTF_8);

        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);
        ReinodoceConfig loaded = repository.load();

        assertEquals("", loaded.getLastUsername());
        assertEquals(5, loaded.getReconnectSeconds());
        assertEquals("bulk", loaded.getSynteticGiftComboMode());
        assertEquals("[LIVE]", loaded.getChatPrefix());
    }

    @Test
    void loadExistingZeroReconnectPreservesDisabledState() throws IOException {
        Path file = tempDir.resolve("reinodoce-mc-tiktok-client.json");
        Files.writeString(file, """
                {
                  "lastUsername": "streamer",
                  "reconnectSeconds": 0,
                  "synteticGiftComboMode": "bulk",
                  "chatPrefix": "[LIVE]"
                }
                """, StandardCharsets.UTF_8);

        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir);
        ReinodoceConfig loaded = repository.load();

        assertEquals("streamer", loaded.getLastUsername());
        assertEquals(0, loaded.getReconnectSeconds());
    }
}

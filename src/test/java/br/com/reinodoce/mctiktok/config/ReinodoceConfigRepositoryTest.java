package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

        assertEquals("[LIVE]", loaded.getChatPrefix());
        assertTrue(loaded.isChatEmotesEnabled());
        assertFalse(loaded.isRuleFollowerOnly());
    }

    @Test
    void saveAndLoadRoundTripPreservesDocumentedFields() {
        Path configFile = tempDir.resolve("config").resolve(ReinodoceConfigRepository.DEFAULT_FILE_NAME);
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setLastUsername("alice");
        config.setReconnectSeconds(9);
        config.setRuleFollowerOnly(true);
        config.setRuleMinMemberLevel(3);
        config.setSynteticGiftMinValue(5);
        config.setSynteticGiftComboMode("single");
        config.setSynteticFollowEnabled(true);
        config.setSynteticJoinEnabled(true);
        config.setSynteticMemberLevelEnabled(true);
        config.setChatPrefix("[RD]");
        config.setChatEmotesEnabled(false);

        repository.save(config);
        ReinodoceConfig loaded = repository.load();

        assertEquals("alice", loaded.getLastUsername());
        assertEquals(9, loaded.getReconnectSeconds());
        assertTrue(loaded.isRuleFollowerOnly());
        assertEquals(3, loaded.getRuleMinMemberLevel());
        assertEquals(5, loaded.getSynteticGiftMinValue());
        assertEquals("single", loaded.getSynteticGiftComboMode());
        assertTrue(loaded.isSynteticFollowEnabled());
        assertTrue(loaded.isSynteticJoinEnabled());
        assertTrue(loaded.isSynteticMemberLevelEnabled());
        assertEquals("[RD]", loaded.getChatPrefix());
        assertFalse(loaded.isChatEmotesEnabled());
    }
}

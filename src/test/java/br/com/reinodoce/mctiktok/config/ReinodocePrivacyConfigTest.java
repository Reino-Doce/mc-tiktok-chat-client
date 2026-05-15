package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodocePrivacyConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void privacyControlsDefaultToExistingBehavior() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        assertEquals(0, config.getSessionLoggingRetentionDays());
        assertEquals(0, config.getSessionLoggingRetentionFiles());
        assertFalse(config.isSessionLoggingAnonymized());
        assertFalse(config.isSessionLoggingMetadataOnly());
        assertFalse(config.isMaskUsernamesInOutput());
    }

    @Test
    void privacyControlSettersSanitizeNumericValues() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        config.setSessionLoggingRetentionDays(-1);
        config.setSessionLoggingRetentionFiles(-2);

        assertEquals(0, config.getSessionLoggingRetentionDays());
        assertEquals(0, config.getSessionLoggingRetentionFiles());
    }

    @Test
    void saveAndLoadRoundTripPreservesPrivacyControls() throws IOException {
        Path configFile = tempDir.resolve(ReinodoceConfigRepository.DEFAULT_FILE_NAME);
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(configFile);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSessionLoggingRetentionDays(14);
        config.setSessionLoggingRetentionFiles(10);
        config.setSessionLoggingAnonymized(true);
        config.setSessionLoggingMetadataOnly(true);
        config.setMaskUsernamesInOutput(true);

        repository.save(config);
        String savedJson = Files.readString(configFile);
        ReinodoceConfig loaded = repository.load();

        assertTrue(savedJson.contains("\"sessionLoggingRetentionDays\""));
        assertTrue(savedJson.contains("\"sessionLoggingRetentionFiles\""));
        assertTrue(savedJson.contains("\"sessionLoggingAnonymized\""));
        assertTrue(savedJson.contains("\"sessionLoggingMetadataOnly\""));
        assertTrue(savedJson.contains("\"maskUsernamesInOutput\""));
        assertEquals(14, loaded.getSessionLoggingRetentionDays());
        assertEquals(10, loaded.getSessionLoggingRetentionFiles());
        assertTrue(loaded.isSessionLoggingAnonymized());
        assertTrue(loaded.isSessionLoggingMetadataOnly());
        assertTrue(loaded.isMaskUsernamesInOutput());
    }
}

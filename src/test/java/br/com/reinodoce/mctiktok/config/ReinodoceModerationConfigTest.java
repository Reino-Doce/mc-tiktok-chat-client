package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceModerationConfigTest {
    private static final List<String> ALLOWED_USERS = List.of("alice", "bob");

    @TempDir
    Path tempDir;

    @Test
    void newModerationRulesDefaultToDisabled() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        assertFalse(config.isRuleEmoteOnlyFilterEnabled());
        assertFalse(config.isRuleLinkFilterEnabled());
        assertEquals(0, config.getRuleUserCooldownSeconds());
        assertFalse(config.isRuleAllowlistMode());
        assertTrue(config.getRuleAllowedUsers().isEmpty());
    }

    @Test
    void newModerationRulesAreSanitized() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        config.setRuleUserCooldownSeconds(-6);
        config.setRuleAllowedUsers(List.of("@Alice", "bad user", "ALICE", "bob"));

        assertEquals(0, config.getRuleUserCooldownSeconds());
        assertEquals(ALLOWED_USERS, config.getRuleAllowedUsers());
    }

    @Test
    void newModerationRulesRoundTripThroughRepository() throws IOException {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve("config.json"));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setRuleEmoteOnlyFilterEnabled(true);
        config.setRuleLinkFilterEnabled(true);
        config.setRuleUserCooldownSeconds(4);
        config.setRuleAllowlistMode(true);
        config.setRuleAllowedUsers(ALLOWED_USERS);

        repository.save(config);
        ReinodoceConfig loaded = repository.load();

        assertTrue(loaded.isRuleEmoteOnlyFilterEnabled());
        assertTrue(loaded.isRuleLinkFilterEnabled());
        assertEquals(4, loaded.getRuleUserCooldownSeconds());
        assertTrue(loaded.isRuleAllowlistMode());
        assertEquals(ALLOWED_USERS, loaded.getRuleAllowedUsers());
    }
}

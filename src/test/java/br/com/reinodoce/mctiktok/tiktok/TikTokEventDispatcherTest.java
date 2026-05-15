package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikTokEventDispatcherTest {
    @Test
    void syntheticSummaryFlushHonorsCurrentTogglesAndOutputMode() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSyntheticFollowEnabled(true);
        config.setSyntheticJoinEnabled(true);

        assertTrue(TikTokEventDispatcher.shouldSendSyntheticSummary(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW));
        assertTrue(TikTokEventDispatcher.shouldSendSyntheticSummary(
                config, BurstOutputController.SyntheticBurstKind.JOIN));

        config.setSyntheticFollowEnabled(false);

        assertFalse(TikTokEventDispatcher.shouldSendSyntheticSummary(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW));
        assertTrue(TikTokEventDispatcher.shouldSendSyntheticSummary(
                config, BurstOutputController.SyntheticBurstKind.JOIN));

        config.setOutputMode("off");

        assertFalse(TikTokEventDispatcher.shouldSendSyntheticSummary(
                config, BurstOutputController.SyntheticBurstKind.JOIN));
    }
}

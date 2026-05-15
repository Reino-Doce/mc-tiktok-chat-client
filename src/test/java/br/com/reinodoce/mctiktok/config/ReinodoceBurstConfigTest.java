package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinodoceBurstConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void burstControlsDefaultToDisabled() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        assertFalse(config.isBurstControlEnabled());
        assertEquals(0, config.getBurstCommentsPerSecond());
        assertEquals(0, config.getBurstSyntheticAggregationSeconds());
    }

    @Test
    void burstControlsClampToSupportedRanges() {
        ReinodoceConfig config = ReinodoceConfig.defaults();

        config.setBurstCommentsPerSecond(-1);
        config.setBurstSyntheticAggregationSeconds(-1);

        assertEquals(0, config.getBurstCommentsPerSecond());
        assertEquals(0, config.getBurstSyntheticAggregationSeconds());

        config.setBurstCommentsPerSecond(ReinodoceConfig.MAX_BURST_COMMENTS_PER_SECOND + 1);
        config.setBurstSyntheticAggregationSeconds(
                ReinodoceConfig.MAX_BURST_SYNTHETIC_AGGREGATION_SECONDS + 1);

        assertEquals(ReinodoceConfig.MAX_BURST_COMMENTS_PER_SECOND, config.getBurstCommentsPerSecond());
        assertEquals(
                ReinodoceConfig.MAX_BURST_SYNTHETIC_AGGREGATION_SECONDS,
                config.getBurstSyntheticAggregationSeconds());
    }

    @Test
    void burstControlsRoundTripThroughRepository() throws IOException {
        ReinodoceConfigRepository repository = new ReinodoceConfigRepository(tempDir.resolve("config.json"));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setBurstControlEnabled(true);
        config.setBurstCommentsPerSecond(8);
        config.setBurstSyntheticAggregationSeconds(5);

        repository.save(config);
        ReinodoceConfig loaded = repository.load();

        assertTrue(loaded.isBurstControlEnabled());
        assertEquals(8, loaded.getBurstCommentsPerSecond());
        assertEquals(5, loaded.getBurstSyntheticAggregationSeconds());
    }
}

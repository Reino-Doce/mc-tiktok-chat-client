package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BurstOutputControllerTest {
    private static final int COMMENT_LIMIT = 2;
    private static final int SYNTHETIC_WINDOW_SECONDS = 3;
    private static final long EXPECTED_DELAY_MILLIS = 3_000L;

    private final AtomicLong clockMillis = new AtomicLong();

    @Test
    void disabledControlsDoNotLimitComments() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setBurstCommentsPerSecond(1);
        BurstOutputController controller = controller();

        assertTrue(controller.shouldShowComment(config));
        assertTrue(controller.shouldShowComment(config));
    }

    @Test
    void limitsCommentsPerVisibleRouteEachSecond() {
        ReinodoceConfig config = burstConfig();
        BurstOutputController controller = controller();

        assertTrue(controller.shouldShowComment(config));
        assertTrue(controller.shouldShowComment(config));
        assertFalse(controller.shouldShowComment(config));

        config.setOutputMode("hud");
        assertTrue(controller.shouldShowComment(config));

        config.setOutputMode("chat");
        clockMillis.addAndGet(1_000L);
        assertTrue(controller.shouldShowComment(config));
    }

    @Test
    void aggregatesSyntheticEventsDuringConfiguredWindow() {
        ReinodoceConfig config = burstConfig();
        config.setBurstSyntheticAggregationSeconds(SYNTHETIC_WINDOW_SECONDS);
        AtomicLong scheduledDelay = new AtomicLong();
        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        AtomicReference<BurstOutputController.SyntheticSummary> emittedSummary = new AtomicReference<>();
        BurstOutputController controller = new BurstOutputController((task, delayMillis) -> {
            scheduledDelay.set(delayMillis);
            scheduledTask.set(task);
        }, clockMillis::get);

        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));
        assertEquals(EXPECTED_DELAY_MILLIS, scheduledDelay.get());
        assertFalse(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));
        assertFalse(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));

        assertNotNull(scheduledTask.get());
        scheduledTask.get().run();

        assertNotNull(emittedSummary.get());
        assertEquals(3L, emittedSummary.get().groupedCount());
        assertEquals(2L, emittedSummary.get().suppressedCount());
    }

    @Test
    void clearDropsPendingSyntheticSummary() {
        ReinodoceConfig config = burstConfig();
        config.setBurstSyntheticAggregationSeconds(SYNTHETIC_WINDOW_SECONDS);
        AtomicReference<BurstOutputController.SyntheticSummary> emittedSummary = new AtomicReference<>();
        BurstOutputController controller = controller();

        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.JOIN, emittedSummary::set));
        assertFalse(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.JOIN, emittedSummary::set));

        controller.clear();

        assertNull(emittedSummary.get());
    }

    @Test
    void staleSyntheticFlushDoesNotDrainNewWindow() {
        ReinodoceConfig config = burstConfig();
        config.setBurstSyntheticAggregationSeconds(SYNTHETIC_WINDOW_SECONDS);
        List<Runnable> scheduledTasks = new ArrayList<>();
        AtomicReference<BurstOutputController.SyntheticSummary> emittedSummary = new AtomicReference<>();
        BurstOutputController controller = new BurstOutputController((task, delayMillis) -> scheduledTasks.add(task),
                clockMillis::get);

        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.JOIN, emittedSummary::set));
        assertFalse(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.JOIN, emittedSummary::set));
        controller.clear();
        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.JOIN, emittedSummary::set));
        assertFalse(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.JOIN, emittedSummary::set));

        scheduledTasks.get(0).run();

        assertNull(emittedSummary.get());

        scheduledTasks.get(1).run();

        assertNotNull(emittedSummary.get());
        assertEquals(2L, emittedSummary.get().groupedCount());
        assertEquals(1L, emittedSummary.get().suppressedCount());
    }

    @Test
    void expiredSyntheticWindowFlushesBeforeNewWindowOpens() {
        ReinodoceConfig config = burstConfig();
        config.setBurstSyntheticAggregationSeconds(SYNTHETIC_WINDOW_SECONDS);
        List<Runnable> scheduledTasks = new ArrayList<>();
        AtomicReference<BurstOutputController.SyntheticSummary> emittedSummary = new AtomicReference<>();
        BurstOutputController controller = new BurstOutputController((task, delayMillis) -> scheduledTasks.add(task),
                clockMillis::get);

        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));
        assertFalse(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));
        clockMillis.addAndGet(EXPECTED_DELAY_MILLIS);

        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));

        assertNotNull(emittedSummary.get());
        assertEquals(2L, emittedSummary.get().groupedCount());
        assertEquals(1L, emittedSummary.get().suppressedCount());

        emittedSummary.set(null);
        scheduledTasks.get(0).run();

        assertNull(emittedSummary.get());
    }

    @Test
    void offModeDoesNotAggregateSyntheticSummaries() {
        ReinodoceConfig config = burstConfig();
        config.setBurstSyntheticAggregationSeconds(SYNTHETIC_WINDOW_SECONDS);
        config.setOutputMode("off");
        List<Runnable> scheduledTasks = new ArrayList<>();
        AtomicReference<BurstOutputController.SyntheticSummary> emittedSummary = new AtomicReference<>();
        BurstOutputController controller = new BurstOutputController((task, delayMillis) -> scheduledTasks.add(task),
                clockMillis::get);

        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));
        assertTrue(controller.shouldShowSynthetic(
                config, BurstOutputController.SyntheticBurstKind.FOLLOW, emittedSummary::set));

        assertTrue(scheduledTasks.isEmpty());
        assertNull(emittedSummary.get());
    }

    private BurstOutputController controller() {
        return new BurstOutputController((task, delayMillis) -> {
        }, clockMillis::get);
    }

    private static ReinodoceConfig burstConfig() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setBurstControlEnabled(true);
        config.setBurstCommentsPerSecond(COMMENT_LIMIT);
        return config;
    }
}

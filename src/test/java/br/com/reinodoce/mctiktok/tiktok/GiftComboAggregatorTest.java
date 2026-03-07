package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftComboAggregatorTest {

    @Test
    void ignoreModeEmitsOnlyDelta() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            GiftComboAggregator aggregator = new GiftComboAggregator(scheduler, emission -> {
            });
            GiftComboAggregator.GiftKey key = new GiftComboAggregator.GiftKey(1L, 7);

            List<GiftComboAggregator.GiftEmission> first = aggregator.handleCombo(
                    GiftComboMode.IGNORE,
                    new GiftComboAggregator.GiftSnapshot(key, "alice", "Rose", 1, 1, 100L),
                    false
            );
            List<GiftComboAggregator.GiftEmission> second = aggregator.handleCombo(
                    GiftComboMode.IGNORE,
                    new GiftComboAggregator.GiftSnapshot(key, "alice", "Rose", 1, 3, 101L),
                    false
            );
            List<GiftComboAggregator.GiftEmission> duplicate = aggregator.handleCombo(
                    GiftComboMode.IGNORE,
                    new GiftComboAggregator.GiftSnapshot(key, "alice", "Rose", 1, 3, 102L),
                    false
            );

            assertEquals(1, first.get(0).count());
            assertEquals(2, second.get(0).count());
            assertTrue(duplicate.isEmpty());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void singleModeEmitsOnlyWhenComboIncrements() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            GiftComboAggregator aggregator = new GiftComboAggregator(scheduler, emission -> {
            });
            GiftComboAggregator.GiftKey key = new GiftComboAggregator.GiftKey(1L, 8);

            List<GiftComboAggregator.GiftEmission> first = aggregator.handleCombo(
                    GiftComboMode.SINGLE,
                    new GiftComboAggregator.GiftSnapshot(key, "alice", "Rose", 1, 1, 200L),
                    false
            );
            List<GiftComboAggregator.GiftEmission> duplicate = aggregator.handleCombo(
                    GiftComboMode.SINGLE,
                    new GiftComboAggregator.GiftSnapshot(key, "alice", "Rose", 1, 1, 201L),
                    false
            );
            List<GiftComboAggregator.GiftEmission> next = aggregator.handleCombo(
                    GiftComboMode.SINGLE,
                    new GiftComboAggregator.GiftSnapshot(key, "alice", "Rose", 1, 2, 202L),
                    false
            );

            assertEquals(1, first.size());
            assertTrue(duplicate.isEmpty());
            assertEquals(2, next.get(0).count());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void bulkModeEmitsConsolidatedOnFinished() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        List<GiftComboAggregator.GiftEmission> asyncFlush = new ArrayList<>();
        try {
            GiftComboAggregator aggregator = new GiftComboAggregator(scheduler, asyncFlush::add);
            GiftComboAggregator.GiftKey key = new GiftComboAggregator.GiftKey(5L, 9);

            List<GiftComboAggregator.GiftEmission> start = aggregator.handleCombo(
                    GiftComboMode.BULK,
                    new GiftComboAggregator.GiftSnapshot(key, "bob", "Galaxy", 10, 2, 300L),
                    false
            );
            List<GiftComboAggregator.GiftEmission> finished = aggregator.handleCombo(
                    GiftComboMode.BULK,
                    new GiftComboAggregator.GiftSnapshot(key, "bob", "Galaxy", 10, 5, 301L),
                    true
            );

            assertTrue(start.isEmpty());
            assertEquals(1, finished.size());
            assertEquals(5, finished.get(0).count());
            assertTrue(asyncFlush.isEmpty());
        } finally {
            scheduler.shutdownNow();
        }
    }
}

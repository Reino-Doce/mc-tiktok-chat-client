package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Tracks local visible-output burst limits without changing accepted event accounting.
 */
final class BurstOutputController {
    private static final int DISABLED_LIMIT = 0;
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long NO_WINDOW_STARTED = Long.MIN_VALUE;
    private static final long NO_SUPPRESSED_EVENTS = 0L;
    private static final long FIRST_VISIBLE_EVENT = 1L;

    private final DelayedTaskScheduler scheduler;
    private final LongSupplier clockMillis;
    private final Map<String, CommentWindow> commentWindows = new HashMap<>();
    private final EnumMap<SyntheticBurstKind, SyntheticWindow> syntheticWindows =
            new EnumMap<>(SyntheticBurstKind.class);

    BurstOutputController(ScheduledExecutorService scheduler) {
        this((task, delayMillis) -> scheduler.schedule(task, delayMillis, TimeUnit.MILLISECONDS),
                System::currentTimeMillis);
    }

    BurstOutputController(DelayedTaskScheduler scheduler, LongSupplier clockMillis) {
        this.scheduler = scheduler;
        this.clockMillis = clockMillis;
        syntheticWindows.put(SyntheticBurstKind.FOLLOW, new SyntheticWindow());
        syntheticWindows.put(SyntheticBurstKind.JOIN, new SyntheticWindow());
    }

    synchronized boolean shouldShowComment(ReinodoceConfig config) {
        if (!isBurstControlActive(config) || config.getBurstCommentsPerSecond() <= DISABLED_LIMIT) {
            return true;
        }
        String route = OutputMode.fromString(config.getOutputMode()).id();
        CommentWindow window = commentWindows.computeIfAbsent(route, ignored -> new CommentWindow());
        long now = clockMillis.getAsLong();
        if (window.startedAtMillis == NO_WINDOW_STARTED || now - window.startedAtMillis >= MILLIS_PER_SECOND) {
            window.startedAtMillis = now;
            window.visibleCount = 0;
        }
        if (window.visibleCount >= config.getBurstCommentsPerSecond()) {
            return false;
        }
        window.visibleCount++;
        return true;
    }

    boolean shouldShowSynthetic(
            ReinodoceConfig config, SyntheticBurstKind kind, Consumer<SyntheticSummary> summaryConsumer
    ) {
        Optional<SyntheticSummary> expiredSummary;
        long generation;
        long windowMillis;
        synchronized (this) {
            if (!isBurstControlActive(config)
                    || isOutputOff(config)
                    || config.getBurstSyntheticAggregationSeconds() <= DISABLED_LIMIT) {
                return true;
            }
            SyntheticWindow window = syntheticWindows.get(kind);
            long now = clockMillis.getAsLong();
            windowMillis = (long) config.getBurstSyntheticAggregationSeconds() * MILLIS_PER_SECOND;
            if (window.isOpen(now, windowMillis)) {
                window.suppressedCount++;
                return false;
            }
            expiredSummary = window.drain(kind);
            generation = window.open(now);
        }
        expiredSummary.ifPresent(summaryConsumer);
        scheduler.schedule(() -> drainSyntheticSummary(kind, generation).ifPresent(summaryConsumer), windowMillis);
        return true;
    }

    synchronized Optional<SyntheticSummary> drainSyntheticSummary(SyntheticBurstKind kind, long generation) {
        SyntheticWindow window = syntheticWindows.get(kind);
        if (!window.hasGeneration(generation)) {
            return Optional.empty();
        }
        return window.drain(kind);
    }

    synchronized void clear() {
        commentWindows.clear();
        for (SyntheticWindow window : syntheticWindows.values()) {
            window.clear();
        }
    }

    private static boolean isBurstControlActive(ReinodoceConfig config) {
        return config != null && config.isBurstControlEnabled();
    }

    private static boolean isOutputOff(ReinodoceConfig config) {
        return config != null && OutputMode.fromString(config.getOutputMode()) == OutputMode.OFF;
    }

    @FunctionalInterface
    interface DelayedTaskScheduler {
        void schedule(Runnable task, long delayMillis);
    }

    enum SyntheticBurstKind {
        FOLLOW,
        JOIN
    }

    record SyntheticSummary(SyntheticBurstKind kind, long groupedCount, long suppressedCount) {
    }

    private static final class CommentWindow {
        private long startedAtMillis = NO_WINDOW_STARTED;
        private int visibleCount;
    }

    private static final class SyntheticWindow {
        private long startedAtMillis = NO_WINDOW_STARTED;
        private long suppressedCount;
        private long generation;

        boolean isOpen(long nowMillis, long windowMillis) {
            return startedAtMillis != NO_WINDOW_STARTED && nowMillis - startedAtMillis < windowMillis;
        }

        long open(long nowMillis) {
            startedAtMillis = nowMillis;
            suppressedCount = NO_SUPPRESSED_EVENTS;
            generation++;
            return generation;
        }

        boolean hasGeneration(long expectedGeneration) {
            return startedAtMillis != NO_WINDOW_STARTED && generation == expectedGeneration;
        }

        void clear() {
            startedAtMillis = NO_WINDOW_STARTED;
            suppressedCount = NO_SUPPRESSED_EVENTS;
            generation++;
        }

        Optional<SyntheticSummary> drain(SyntheticBurstKind kind) {
            long suppressed = suppressedCount;
            clear();
            if (suppressed <= NO_SUPPRESSED_EVENTS) {
                return Optional.empty();
            }
            return Optional.of(new SyntheticSummary(kind, suppressed + FIRST_VISIBLE_EVENT, suppressed));
        }
    }
}

package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.rules.GiftComboMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GiftComboAggregator {
    private static final long BULK_FLUSH_SECONDS = 5L;

    private final ScheduledExecutorService scheduler;
    private final Consumer<GiftEmission> bulkFlushConsumer;
    private final Map<GiftKey, ComboState> states = new ConcurrentHashMap<>();

    public GiftComboAggregator(ScheduledExecutorService scheduler, Consumer<GiftEmission> bulkFlushConsumer) {
        this.scheduler = scheduler;
        this.bulkFlushConsumer = bulkFlushConsumer;
    }

    public synchronized List<GiftEmission> handleCombo(GiftComboMode mode, GiftSnapshot snapshot, boolean finished) {
        return switch (mode) {
            case IGNORE -> handleIgnoreCombo(snapshot, finished);
            case SINGLE -> handleSingleCombo(snapshot, finished);
            case BULK -> handleBulkCombo(snapshot, finished);
        };
    }

    public synchronized List<GiftEmission> handleGift(GiftComboMode mode, GiftSnapshot snapshot) {
        GiftKey key = snapshot.key();
        ComboState state = states.get(key);
        int count = Math.max(1, snapshot.comboCount());

        if (state == null) {
            return List.of(new GiftEmission(
                    snapshot.username(),
                    snapshot.avatarUrl(),
                    snapshot.giftName(),
                    snapshot.giftIconUrl(),
                    snapshot.diamondCost(),
                    count,
                    snapshot.messageId()
            ));
        }

        int knownCount = Math.max(1, state.snapshot.comboCount());
        int effectiveCount = Math.max(count, knownCount);
        removeState(key);

        if (mode == GiftComboMode.BULK) {
            return List.of(new GiftEmission(
                    snapshot.username(),
                    snapshot.avatarUrl(),
                    snapshot.giftName(),
                    snapshot.giftIconUrl(),
                    snapshot.diamondCost(),
                    effectiveCount,
                    snapshot.messageId()
            ));
        }

        return List.of();
    }

    public synchronized void clear() {
        for (ComboState state : states.values()) {
            state.cancel();
        }
        states.clear();
    }

    private List<GiftEmission> handleIgnoreCombo(GiftSnapshot snapshot, boolean finished) {
        GiftKey key = snapshot.key();
        ComboState state = states.get(key);
        int previousCount = state == null ? 0 : state.snapshot.comboCount();
        int currentCount = Math.max(1, snapshot.comboCount());
        int delta = Math.max(0, currentCount - previousCount);

        if (finished) {
            removeState(key);
        } else {
            states.put(key, new ComboState(snapshot, null, 0));
        }

        if (delta <= 0) {
            return List.of();
        }

        return List.of(new GiftEmission(
                snapshot.username(),
                snapshot.avatarUrl(),
                snapshot.giftName(),
                snapshot.giftIconUrl(),
                snapshot.diamondCost(),
                delta,
                snapshot.messageId()
        ));
    }

    private List<GiftEmission> handleSingleCombo(GiftSnapshot snapshot, boolean finished) {
        GiftKey key = snapshot.key();
        ComboState existing = states.get(key);
        int previousCount = existing == null ? 0 : Math.max(1, existing.snapshot.comboCount());
        int currentCount = Math.max(1, snapshot.comboCount());

        if (finished) {
            removeState(key);
        } else {
            states.put(key, new ComboState(snapshot, null, 0));
        }

        if (currentCount <= previousCount) {
            return List.of();
        }

        return List.of(new GiftEmission(
                snapshot.username(),
                snapshot.avatarUrl(),
                snapshot.giftName(),
                snapshot.giftIconUrl(),
                snapshot.diamondCost(),
                currentCount,
                snapshot.messageId()
        ));
    }

    private List<GiftEmission> handleBulkCombo(GiftSnapshot snapshot, boolean finished) {
        GiftKey key = snapshot.key();
        ComboState existing = states.get(key);
        int maxCount = existing == null
                ? Math.max(1, snapshot.comboCount())
                : Math.max(Math.max(1, snapshot.comboCount()), Math.max(1, existing.snapshot.comboCount()));
        GiftSnapshot normalizedSnapshot = new GiftSnapshot(
                snapshot.key(),
                snapshot.username(),
                snapshot.avatarUrl(),
                snapshot.giftName(),
                snapshot.giftIconUrl(),
                snapshot.diamondCost(),
                maxCount,
                snapshot.messageId()
        );
        int generation = existing == null ? 1 : existing.generation + 1;
        ScheduledFuture<?> nextFuture = scheduleBulkFlush(key, generation);
        ComboState updated = new ComboState(normalizedSnapshot, nextFuture, generation);
        states.put(key, updated);

        if (existing != null) {
            existing.cancel();
        }

        if (!finished) {
            return List.of();
        }

        removeState(key);
        return List.of(emissionFrom(normalizedSnapshot));
    }

    private ScheduledFuture<?> scheduleBulkFlush(GiftKey key, int generation) {
        return scheduler.schedule(() -> {
            GiftEmission emission = null;
            synchronized (this) {
                ComboState state = states.get(key);
                if (state == null || state.generation != generation) {
                    return;
                }
                states.remove(key);
                emission = emissionFrom(state.snapshot);
            }
            if (emission != null) {
                bulkFlushConsumer.accept(emission);
            }
        }, BULK_FLUSH_SECONDS, TimeUnit.SECONDS);
    }

    private void removeState(GiftKey key) {
        ComboState state = states.remove(key);
        if (state != null) {
            state.cancel();
        }
    }

    private GiftEmission emissionFrom(GiftSnapshot snapshot) {
        return new GiftEmission(
                snapshot.username(),
                snapshot.avatarUrl(),
                snapshot.giftName(),
                snapshot.giftIconUrl(),
                snapshot.diamondCost(),
                Math.max(1, snapshot.comboCount()),
                snapshot.messageId()
        );
    }

    private static final class ComboState {
        private final GiftSnapshot snapshot;
        private final ScheduledFuture<?> flushTask;
        private final int generation;

        private ComboState(GiftSnapshot snapshot, ScheduledFuture<?> flushTask, int generation) {
            this.snapshot = snapshot;
            this.flushTask = flushTask;
            this.generation = generation;
        }

        private void cancel() {
            if (flushTask != null) {
                flushTask.cancel(false);
            }
        }
    }

    public record GiftKey(long userId, int giftId) {
    }

    public record GiftSnapshot(
            GiftKey key,
            String username,
            String avatarUrl,
            String giftName,
            String giftIconUrl,
            int diamondCost,
            int comboCount,
            long messageId
    ) {
    }

    public record GiftEmission(
            String username,
            String avatarUrl,
            String giftName,
            String giftIconUrl,
            int diamondCost,
            int count,
            long messageId
    ) {
    }
}

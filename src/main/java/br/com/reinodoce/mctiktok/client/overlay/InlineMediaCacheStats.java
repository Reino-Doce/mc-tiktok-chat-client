package br.com.reinodoce.mctiktok.client.overlay;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

final class InlineMediaCacheStats {
    private final AtomicLong downloadsStarted = new AtomicLong();
    private final AtomicLong downloadsSucceeded = new AtomicLong();
    private final AtomicLong downloadsFailed = new AtomicLong();
    private final AtomicLong diskHits = new AtomicLong();
    private final AtomicLong diskReloads = new AtomicLong();
    private final AtomicLong memoryHits = new AtomicLong();
    private final AtomicLong ttlEvictions = new AtomicLong();
    private final AtomicLong capacityEvictions = new AtomicLong();

    void recordDownloadStarted() {
        downloadsStarted.incrementAndGet();
    }

    void recordDownloadSucceeded() {
        downloadsSucceeded.incrementAndGet();
    }

    void recordDownloadFailed() {
        downloadsFailed.incrementAndGet();
    }

    void recordDiskHit() {
        diskHits.incrementAndGet();
    }

    void recordDiskReload() {
        diskReloads.incrementAndGet();
    }

    void recordMemoryHit() {
        memoryHits.incrementAndGet();
    }

    void recordTtlEviction() {
        ttlEvictions.incrementAndGet();
    }

    void recordCapacityEviction() {
        capacityEvictions.incrementAndGet();
    }

    InlineMediaCache.Snapshot snapshotFor(Collection<InlineMediaCacheEntry> entries) {
        EntryCounts counts = countEntries(entries);
        return new InlineMediaCache.Snapshot(
                counts.resident,
                counts.ready,
                counts.loading,
                counts.error,
                downloadsStarted.get(),
                downloadsSucceeded.get(),
                downloadsFailed.get(),
                diskHits.get(),
                diskReloads.get(),
                memoryHits.get(),
                ttlEvictions.get(),
                capacityEvictions.get());
    }

    private static EntryCounts countEntries(Collection<InlineMediaCacheEntry> entries) {
        EntryCounts counts = new EntryCounts();
        for (InlineMediaCacheEntry entry : entries) {
            counts.tally(entry);
        }
        return counts;
    }

    private static final class EntryCounts {
        int ready;
        int loading;
        int error;
        int resident;

        void tally(InlineMediaCacheEntry entry) {
            switch (entry.state) {
                case READY -> ready++;
                case LOADING -> loading++;
                case ERROR -> error++;
                case NEW -> { /* NEW entries are not yet counted in any active bucket. */ }
                default -> { /* defensive guard for future enum values. */ }
            }
            if (entry.texture != null) {
                resident++;
            }
        }
    }
}

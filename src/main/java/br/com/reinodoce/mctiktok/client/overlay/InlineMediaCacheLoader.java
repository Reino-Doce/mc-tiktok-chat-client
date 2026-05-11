package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;

import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;

final class InlineMediaCacheLoader {
    private static final int ERROR_RETRY_SECONDS = 30;
    private static final long ERROR_RETRY_MILLIS = Duration.ofSeconds(ERROR_RETRY_SECONDS).toMillis();

    private final Map<String, InlineMediaCacheEntry> entries;
    private final ExecutorService loader;
    private final InlineMediaDownloader downloader;
    private final InlineMediaCacheStats stats;

    InlineMediaCacheLoader(
            Map<String, InlineMediaCacheEntry> entries,
            ExecutorService loader,
            InlineMediaDownloader downloader,
            InlineMediaCacheStats stats
    ) {
        this.entries = entries;
        this.loader = loader;
        this.downloader = downloader;
        this.stats = stats;
    }

    InlineMediaCacheEntry ensureAvailable(
            RichLiveMessage.InlineMediaSegment segment, boolean countStats, boolean visibleAccess
    ) {
        String sourceUrl = segment.sourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        InlineMediaCacheEntry entry = entries.computeIfAbsent(
                InlineMediaCacheKeys.cacheKey(sourceUrl),
                key -> new InlineMediaCacheEntry(key, segment.kind().id(), sourceUrl));
        entry.touch(now, visibleAccess);
        synchronized (entry.monitor) {
            return scheduleIfNeeded(entry, now, countStats);
        }
    }

    private InlineMediaCacheEntry scheduleIfNeeded(
            InlineMediaCacheEntry entry, long now, boolean countStats
    ) {
        if (entry.state == InlineMediaCacheEntry.EntryState.READY) {
            if (countStats) {
                stats.recordMemoryHit();
            }
            return entry;
        }
        if (entry.state == InlineMediaCacheEntry.EntryState.LOADING || backingOffFromError(entry, now)) {
            return entry;
        }
        entry.state = InlineMediaCacheEntry.EntryState.LOADING;
        dispatchLoad(entry, countStats);
        return entry;
    }

    private static boolean backingOffFromError(InlineMediaCacheEntry entry, long now) {
        return entry.state == InlineMediaCacheEntry.EntryState.ERROR
                && now - entry.lastFailureAt < ERROR_RETRY_MILLIS;
    }

    private void dispatchLoad(InlineMediaCacheEntry entry, boolean countStats) {
        if (Files.exists(entry.payloadPath())) {
            if (countStats) {
                stats.recordDiskHit();
            }
            stats.recordDiskReload();
            loader.submit(() -> downloader.loadFromDisk(entry));
        } else {
            stats.recordDownloadStarted();
            loader.submit(() -> downloader.downloadFromNetwork(entry));
        }
    }
}

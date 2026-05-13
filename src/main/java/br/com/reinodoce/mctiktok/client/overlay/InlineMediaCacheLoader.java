package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

final class InlineMediaCacheLoader {
    private static final int ERROR_RETRY_SECONDS = 30;
    private static final int DISK_TOUCH_INTERVAL_MINUTES = 5;
    private static final long ERROR_RETRY_MILLIS = Duration.ofSeconds(ERROR_RETRY_SECONDS).toMillis();
    private static final long DISK_TOUCH_INTERVAL_MILLIS =
            Duration.ofMinutes(DISK_TOUCH_INTERVAL_MINUTES).toMillis();

    private final Map<String, InlineMediaCacheEntry> entries;
    private final ExecutorService loadExecutor;
    private final ExecutorService maintenanceExecutor;
    private final InlineMediaDownloader downloader;
    private final InlineMediaCacheStats stats;

    InlineMediaCacheLoader(
            Map<String, InlineMediaCacheEntry> entries,
            ExecutorService loadExecutor,
            ExecutorService maintenanceExecutor,
            InlineMediaDownloader downloader,
            InlineMediaCacheStats stats
    ) {
        this.entries = entries;
        this.loadExecutor = loadExecutor;
        this.maintenanceExecutor = maintenanceExecutor;
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
            refreshDiskAccessMetadata(entry, now);
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
            loadExecutor.submit(() -> downloader.loadFromDisk(entry));
        } else {
            stats.recordDownloadStarted();
            loadExecutor.submit(() -> downloader.downloadFromNetwork(entry));
        }
    }

    private void refreshDiskAccessMetadata(InlineMediaCacheEntry entry, long now) {
        if (now - entry.lastMetadataTouchAt < DISK_TOUCH_INTERVAL_MILLIS) {
            return;
        }
        entry.lastMetadataTouchAt = now;
        try {
            maintenanceExecutor.execute(() -> touchMetadata(entry, now));
        } catch (RejectedExecutionException exception) {
            entry.lastMetadataTouchAt = 0L;
            ReinodoceLogger.LOGGER.debug(
                    "Failed to schedule inline media metadata update for {}", entry.sourceReference(), exception);
        }
    }

    private static void touchMetadata(InlineMediaCacheEntry entry, long now) {
        try {
            InlineMediaMetadataStore.writeReady(entry, InlineMediaMetadataStore.readWithEntryDefaults(entry), now);
        } catch (IOException | IllegalStateException exception) {
            ReinodoceLogger.LOGGER.debug(
                    "Failed to update inline media access metadata for {}", entry.sourceReference(), exception);
        }
    }
}

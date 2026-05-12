package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

final class InlineMediaDiskCacheCleaner {
    static final long MAX_DISK_BYTES = 64L * 1024L * 1024L;
    static final Duration DISK_ENTRY_TTL = Duration.ofDays(7);

    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);

    private final Map<String, InlineMediaCacheEntry> entries;
    private final ExecutorService cleanupExecutor;
    private final AtomicBoolean cleanupRunning = new AtomicBoolean();
    private long lastCleanupAt;

    InlineMediaDiskCacheCleaner(Map<String, InlineMediaCacheEntry> entries, ExecutorService cleanupExecutor) {
        this.entries = entries;
        this.cleanupExecutor = cleanupExecutor;
    }

    void cleanup(long now) {
        cleanup(InlineMediaCacheEntry.cacheRootIfAvailable(), now);
    }

    void cleanup(Path root, long now) {
        if (root == null || !shouldSchedule(now)) {
            return;
        }
        try {
            cleanupExecutor.execute(() -> runCleanup(root, now));
            markScheduled(now);
        } catch (RejectedExecutionException exception) {
            cleanupRunning.set(false);
            ReinodoceLogger.LOGGER.debug("Failed to schedule inline media disk cache cleanup", exception);
        }
    }

    static void cleanup(Path root, Map<String, InlineMediaCacheEntry> entries, long now, Limits limits) {
        if (!Files.isDirectory(root)) {
            return;
        }
        List<DiskEntry> diskEntries = listDiskEntries(root, entries, now);
        long totalBytes = evictExpiredEntries(diskEntries, now, limits.ttlMillis());
        evictEntriesOverQuota(diskEntries, totalBytes, limits.maxBytes());
    }

    private synchronized boolean shouldSchedule(long now) {
        if (lastCleanupAt != 0L && now - lastCleanupAt < CLEANUP_INTERVAL.toMillis()) {
            return false;
        }
        return cleanupRunning.compareAndSet(false, true);
    }

    private synchronized void markScheduled(long now) {
        lastCleanupAt = now;
    }

    private void runCleanup(Path root, long now) {
        try {
            cleanup(root, entries, now, new Limits(MAX_DISK_BYTES, DISK_ENTRY_TTL.toMillis()));
        } finally {
            cleanupRunning.set(false);
        }
    }

    static void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(InlineMediaDiskCacheCleaner::deletePath);
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to delete inline media cache directory {}", directory, exception);
        }
    }

    private static long totalBytes(List<DiskEntry> diskEntries) {
        long total = 0L;
        for (DiskEntry entry : diskEntries) {
            total += entry.bytes();
        }
        return total;
    }

    private static long evictExpiredEntries(List<DiskEntry> diskEntries, long now, long ttlMillis) {
        long totalBytes = totalBytes(diskEntries);
        for (DiskEntry entry : diskEntries) {
            if (entry.evictable() && now - entry.lastUsedAt() > ttlMillis) {
                deleteDirectory(entry.path());
                totalBytes -= entry.bytes();
            }
        }
        return totalBytes;
    }

    private static void evictEntriesOverQuota(List<DiskEntry> diskEntries, long totalBytes, long maxBytes) {
        long remainingBytes = totalBytes;
        diskEntries.sort(Comparator.comparingLong(DiskEntry::lastUsedAt));
        for (DiskEntry entry : diskEntries) {
            remainingBytes = evictEntryOverQuota(entry, remainingBytes, maxBytes);
        }
    }

    private static long evictEntryOverQuota(DiskEntry entry, long totalBytes, long maxBytes) {
        if (totalBytes <= maxBytes || !entry.evictable() || !Files.exists(entry.path())) {
            return totalBytes;
        }
        deleteDirectory(entry.path());
        return totalBytes - entry.bytes();
    }

    private static List<DiskEntry> listDiskEntries(
            Path root, Map<String, InlineMediaCacheEntry> entries, long now
    ) {
        List<DiskEntry> diskEntries = new ArrayList<>();
        try (Stream<Path> paths = Files.list(root)) {
            paths.filter(Files::isDirectory)
                    .forEach(path -> diskEntries.add(toDiskEntry(path, entryFor(path, entries), now)));
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to inspect inline media disk cache {}", root, exception);
        }
        return diskEntries;
    }

    private static InlineMediaCacheEntry entryFor(Path path, Map<String, InlineMediaCacheEntry> entries) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return null;
        }
        return entries.get(fileName.toString());
    }

    private static boolean isEvictionCandidate(InlineMediaCacheEntry entry, long now) {
        return entry == null
                || (entry.state != InlineMediaCacheEntry.EntryState.LOADING && !entry.isRecentlyVisible(now));
    }

    private static DiskEntry toDiskEntry(Path path, InlineMediaCacheEntry entry, long now) {
        return new DiskEntry(
                path,
                directorySize(path),
                lastUsedAt(path, entry),
                isEvictionCandidate(entry, now));
    }

    private static long directorySize(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.filter(Files::isRegularFile)
                    .mapToLong(InlineMediaDiskCacheCleaner::fileSize)
                    .sum();
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to size inline media cache directory {}", path, exception);
            return 0L;
        }
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static long lastUsedAt(Path path, InlineMediaCacheEntry entry) {
        long metadataLastUsedAt = metadataLastUsedAt(path);
        long memoryLastUsedAt = entry == null ? 0L : entry.lastAccessAt;
        long explicitLastUsedAt = Math.max(metadataLastUsedAt, memoryLastUsedAt);
        return explicitLastUsedAt > 0L ? explicitLastUsedAt : filesystemLastModifiedAt(path);
    }

    private static long metadataLastUsedAt(Path path) {
        try {
            InlineMediaMetadata metadata = InlineMediaMetadata.read(path.resolve("metadata.json"));
            return Math.max(metadata.lastUsedAt(), metadata.fetchedAt());
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static long filesystemLastModifiedAt(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.mapToLong(InlineMediaDiskCacheCleaner::lastModifiedAt)
                    .max()
                    .orElse(0L);
        } catch (IOException exception) {
            return lastModifiedAt(path);
        }
    }

    private static long lastModifiedAt(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to delete inline media cache path {}", path, exception);
        }
    }

    record Limits(long maxBytes, long ttlMillis) {
    }

    private record DiskEntry(Path path, long bytes, long lastUsedAt, boolean evictable) {
    }
}

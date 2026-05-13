package br.com.reinodoce.mctiktok.client.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineMediaDiskCacheCleanerTest {

    @TempDir
    Path tempDir;

    @Test
    void cleanupDeletesExpiredDiskEntries() throws IOException {
        Path expired = cacheEntry("expired", 1_000, 10L);
        Path fresh = cacheEntry("fresh", 1_000, 9_000L);

        InlineMediaDiskCacheCleaner.cleanup(
                tempDir,
                Map.of(),
                10_000L,
                new InlineMediaDiskCacheCleaner.Limits(10_000L, 5_000L));

        assertFalse(Files.exists(expired));
        assertTrue(Files.exists(fresh));
    }

    @Test
    void cleanupDeletesOldestEntriesWhenOverQuota() throws IOException {
        Path oldest = cacheEntry("oldest", 8_000, 1_000L);
        Path newest = cacheEntry("newest", 8_000, 2_000L);

        InlineMediaDiskCacheCleaner.cleanup(
                tempDir,
                Map.of(),
                3_000L,
                new InlineMediaDiskCacheCleaner.Limits(10_000L, 10_000L));

        assertFalse(Files.exists(oldest));
        assertTrue(Files.exists(newest));
    }

    @Test
    void cleanupUsesMemoryAccessTimeBeforeDiskTtlEviction() throws IOException {
        Path active = cacheEntry("active", 1_000, 1_000L);
        InlineMediaCacheEntry entry = readyEntry("active", 9_000L);

        InlineMediaDiskCacheCleaner.cleanup(
                tempDir,
                Map.of("active", entry),
                10_000L,
                new InlineMediaDiskCacheCleaner.Limits(10_000L, 5_000L));

        assertTrue(Files.exists(active));
    }

    @Test
    void cleanupFallsBackToFilesystemTimeWhenMetadataIsMissing() throws IOException {
        Path fresh = cacheEntryWithoutMetadata("fresh-missing-metadata", 1_000, 9_000L);
        Path expired = cacheEntryWithoutMetadata("expired-missing-metadata", 1_000, 1_000L);

        InlineMediaDiskCacheCleaner.cleanup(
                tempDir,
                Map.of(),
                10_000L,
                new InlineMediaDiskCacheCleaner.Limits(10_000L, 5_000L));

        assertTrue(Files.exists(fresh));
        assertFalse(Files.exists(expired));
    }

    @Test
    void quotaCountsProtectedDirectoriesBeforeEvictingCandidates() throws IOException {
        Path protectedEntry = cacheEntry("protected", 8_000, 2_000L);
        Path oldCandidate = cacheEntry("old-candidate", 4_000, 1_000L);
        Map<String, InlineMediaCacheEntry> entries = new HashMap<>();
        InlineMediaCacheEntry protectedMemoryEntry = readyEntry("protected", 2_000L);
        protectedMemoryEntry.protectedUntilAt = 4_000L;
        entries.put("protected", protectedMemoryEntry);

        InlineMediaDiskCacheCleaner.cleanup(
                tempDir,
                entries,
                3_000L,
                new InlineMediaDiskCacheCleaner.Limits(10_000L, 10_000L));

        assertTrue(Files.exists(protectedEntry));
        assertFalse(Files.exists(oldCandidate));
    }

    @Test
    void rejectedAsyncCleanupDoesNotConsumeThrottleWindow() throws IOException {
        Path expired = cacheEntry("expired-after-rejection", 1_000, 1_000L);
        InlineMediaDiskCacheCleaner cleaner =
                new InlineMediaDiskCacheCleaner(Map.of(), new RejectOnceExecutorService());
        long now = InlineMediaDiskCacheCleaner.DISK_ENTRY_TTL.toMillis() + 10_000L;

        cleaner.cleanup(tempDir, now);
        assertTrue(Files.exists(expired));

        cleaner.cleanup(tempDir, now + 1L);
        assertFalse(Files.exists(expired));
    }

    private Path cacheEntry(String key, int payloadBytes, long lastUsedAt) throws IOException {
        Path directory = tempDir.resolve(key);
        Files.createDirectories(directory);
        Files.write(directory.resolve("payload"), new byte[payloadBytes]);
        new InlineMediaMetadata(
                "sha256:" + key,
                "remote_emote",
                "image/png",
                payloadBytes,
                16,
                16,
                lastUsedAt,
                lastUsedAt,
                InlineMediaMetadata.STATUS_READY).writeTo(directory.resolve("metadata.json"));
        return directory;
    }

    private Path cacheEntryWithoutMetadata(String key, int payloadBytes, long lastModifiedAt) throws IOException {
        Path directory = tempDir.resolve(key);
        Files.createDirectories(directory);
        Path payload = directory.resolve("payload");
        Files.write(payload, new byte[payloadBytes]);
        FileTime fileTime = FileTime.fromMillis(lastModifiedAt);
        Files.setLastModifiedTime(payload, fileTime);
        Files.setLastModifiedTime(directory, fileTime);
        return directory;
    }

    private InlineMediaCacheEntry readyEntry(String key, long lastAccessAt) {
        InlineMediaCacheEntry entry = new InlineMediaCacheEntry(key, "remote_emote", "https://cdn.example/" + key);
        entry.state = InlineMediaCacheEntry.EntryState.READY;
        entry.lastAccessAt = lastAccessAt;
        return entry;
    }

    private static final class RejectOnceExecutorService extends AbstractExecutorService {
        private boolean rejected;

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            if (!rejected) {
                rejected = true;
                throw new RejectedExecutionException("first cleanup is rejected");
            }
            command.run();
        }
    }
}

package br.com.reinodoce.mctiktok.client.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
}

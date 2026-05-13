package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.time.Duration;

final class InlineMediaCacheEntry {
    private static final int FALLBACK_DIMENSION = 16;
    private static final Duration VISIBILITY_GRACE = Duration.ofSeconds(15);

    final Object monitor = new Object();
    final String key;
    final String kind;
    final String sourceUrl;
    volatile EntryState state = EntryState.NEW;
    volatile ResourceLocation texture;
    volatile int width = FALLBACK_DIMENSION;
    volatile int height = FALLBACK_DIMENSION;
    volatile long lastAccessAt = System.currentTimeMillis();
    volatile long lastMetadataTouchAt;
    volatile long protectedUntilAt;
    volatile long lastFailureAt;

    InlineMediaCacheEntry(String key, String kind, String sourceUrl) {
        this.key = key;
        this.kind = kind;
        this.sourceUrl = sourceUrl;
    }

    Path payloadPath() {
        return directory().resolve("payload");
    }

    Path metadataPath() {
        return directory().resolve("metadata.json");
    }

    Path directoryPath() {
        Path root = cacheRootIfAvailable();
        if (root == null) {
            return null;
        }
        return root.resolve(key);
    }

    String sourceReference() {
        return "sha256:" + key;
    }

    void touch(long now, boolean visibleAccess) {
        lastAccessAt = now;
        if (visibleAccess) {
            protectedUntilAt = Math.max(protectedUntilAt, now + VISIBILITY_GRACE.toMillis());
        }
    }

    boolean isRecentlyVisible(long now) {
        return protectedUntilAt > now;
    }

    private Path directory() {
        return cacheRoot()
                .resolve(key);
    }

    static Path cacheRoot() {
        Path root = cacheRootIfAvailable();
        if (root == null) {
            throw new IllegalStateException("Minecraft game directory is not available");
        }
        return root;
    }

    static Path cacheRootIfAvailable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) {
            return null;
        }
        return minecraft.gameDirectory.toPath()
                .resolve("cache")
                .resolve(ReinodoceMcTiktokMod.MOD_ID)
                .resolve("inline-media");
    }

    enum EntryState {
        NEW,
        LOADING,
        READY,
        ERROR
    }
}

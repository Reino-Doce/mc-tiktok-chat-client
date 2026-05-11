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
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("cache")
                .resolve(ReinodoceMcTiktokMod.MOD_ID)
                .resolve("inline-media")
                .resolve(key);
    }

    enum EntryState {
        NEW,
        LOADING,
        READY,
        ERROR
    }
}

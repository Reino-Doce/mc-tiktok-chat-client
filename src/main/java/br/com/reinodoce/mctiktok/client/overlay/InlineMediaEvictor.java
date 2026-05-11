package br.com.reinodoce.mctiktok.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class InlineMediaEvictor {
    private static final int MAX_MEMORY_ENTRIES = 256;
    private static final Duration ENTRY_TTL = Duration.ofMinutes(30);

    private final Map<String, InlineMediaCacheEntry> entries;
    private final InlineMediaCacheStats stats;

    InlineMediaEvictor(Map<String, InlineMediaCacheEntry> entries, InlineMediaCacheStats stats) {
        this.entries = entries;
        this.stats = stats;
    }

    void cleanup(long now) {
        evictTtlExpired(now);
        evictForCapacity(now);
    }

    private void evictTtlExpired(long now) {
        List<InlineMediaCacheEntry> ttlCandidates = new ArrayList<>();
        for (InlineMediaCacheEntry entry : entries.values()) {
            if (shouldEvictByTtl(entry, now)) {
                ttlCandidates.add(entry);
            }
        }
        for (InlineMediaCacheEntry entry : ttlCandidates) {
            evictEntry(entry, EvictionReason.TTL);
        }
    }

    private void evictForCapacity(long now) {
        int overflow = entries.size() - MAX_MEMORY_ENTRIES;
        if (overflow <= 0) {
            return;
        }
        List<InlineMediaCacheEntry> candidates = new ArrayList<>();
        for (InlineMediaCacheEntry entry : entries.values()) {
            if (isCapacityEvictionCandidate(entry, now)) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.comparingLong(candidate -> candidate.lastAccessAt));
        for (InlineMediaCacheEntry entry : candidates) {
            if (entries.size() <= MAX_MEMORY_ENTRIES) {
                break;
            }
            evictEntry(entry, EvictionReason.CAPACITY);
        }
    }

    private static boolean shouldEvictByTtl(InlineMediaCacheEntry entry, long now) {
        return entry.state != InlineMediaCacheEntry.EntryState.LOADING
                && !entry.isRecentlyVisible(now)
                && now - entry.lastAccessAt > ENTRY_TTL.toMillis();
    }

    private static boolean isCapacityEvictionCandidate(InlineMediaCacheEntry entry, long now) {
        return entry.state != InlineMediaCacheEntry.EntryState.LOADING && !entry.isRecentlyVisible(now);
    }

    private void evictEntry(InlineMediaCacheEntry entry, EvictionReason reason) {
        InlineMediaCacheEntry removed = entries.remove(entry.key);
        if (removed == null) {
            return;
        }
        if (reason == EvictionReason.TTL) {
            stats.recordTtlEviction();
        } else {
            stats.recordCapacityEviction();
        }
        releaseTexture(removed);
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void releaseTexture(InlineMediaCacheEntry removed) {
        ResourceLocation texture = removed.texture;
        removed.texture = null;
        if (texture == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> minecraft.getTextureManager().release(texture));
        }
    }

    private enum EvictionReason {
        TTL,
        CAPACITY
    }
}

package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.ExecutorsFactory;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class InlineMediaCache {
    public static final int FALLBACK_DIMENSION = 16;

    public static final ResourceLocation DEFAULT_AVATAR_TEXTURE = Objects.requireNonNull(
            ResourceLocation.fromNamespaceAndPath(
                    InlineMediaUrls.DEFAULT_NAMESPACE, InlineMediaUrls.DEFAULT_AVATAR_PATH));
    public static final ResourceLocation LOADING_TEXTURE = Objects.requireNonNull(
            ResourceLocation.fromNamespaceAndPath(
                    ReinodoceMcTiktokMod.MOD_ID, "textures/gui/emote_loading.png"));
    public static final ResourceLocation ERROR_TEXTURE = Objects.requireNonNull(
            ResourceLocation.fromNamespaceAndPath(
                    ReinodoceMcTiktokMod.MOD_ID, "textures/gui/emote_error.png"));

    private final Map<String, InlineMediaCacheEntry> entries = new ConcurrentHashMap<>();
    private final ExecutorService executor = ExecutorsFactory.newSingleThreadExecutor("reinodoce-inline-media");
    private final InlineMediaCacheStats stats = new InlineMediaCacheStats();
    private final InlineMediaDownloader downloader = new InlineMediaDownloader(stats);
    private final InlineMediaCacheLoader cacheLoader = new InlineMediaCacheLoader(entries, executor, downloader, stats);
    private final InlineMediaEvictor evictor = new InlineMediaEvictor(entries, stats);

    public InlineMediaCache() {
        ImageIoBootstrap.ensureInitialized();
    }

    public void prefetch(RichLiveMessage message) {
        if (message == null) {
            return;
        }
        evictor.cleanup(System.currentTimeMillis());
        for (RichLiveMessage.InlineMediaSegment segment : message.inlineMediaSegments()) {
            if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
                continue;
            }
            cacheLoader.ensureAvailable(segment, true, false);
        }
    }

    public TextureHandle resolve(RichLiveMessage.InlineMediaSegment segment) {
        if (segment == null || segment.sourceUrl().isBlank()) {
            return resolveBlank(segment);
        }
        if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
            return TextureHandle.ready(
                    resourceLocationFor(segment.sourceUrl()), FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        }
        InlineMediaCacheEntry entry = cacheLoader.ensureAvailable(segment, false, true);
        if (entry == null) {
            return TextureHandle.error(ERROR_TEXTURE, FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        }
        return handleForState(segment, entry);
    }

    public Dimensions dimensionsFor(RichLiveMessage.InlineMediaSegment segment) {
        if (segment == null || segment.sourceUrl().isBlank()) {
            return new Dimensions(FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        }
        if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
            return new Dimensions(FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        }
        InlineMediaCacheEntry entry = cacheLoader.ensureAvailable(segment, false, true);
        if (entry == null) {
            return new Dimensions(FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        }
        return new Dimensions(Math.max(1, entry.width), Math.max(1, entry.height));
    }

    public Snapshot snapshot() {
        return stats.snapshotFor(entries.values());
    }

    static ResourceLocation placeholderTextureFor(RichLiveMessage.InlineMediaSegment segment, boolean error) {
        if (segment != null && segment.kind() == RichLiveMessage.InlineMediaKind.AVATAR) {
            return DEFAULT_AVATAR_TEXTURE;
        }
        return error ? ERROR_TEXTURE : LOADING_TEXTURE;
    }

    private static TextureHandle resolveBlank(RichLiveMessage.InlineMediaSegment segment) {
        if (segment != null && segment.kind() == RichLiveMessage.InlineMediaKind.AVATAR) {
            return TextureHandle.ready(DEFAULT_AVATAR_TEXTURE, FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        }
        return TextureHandle.error(ERROR_TEXTURE, FALLBACK_DIMENSION, FALLBACK_DIMENSION);
    }

    private static TextureHandle handleForState(
            RichLiveMessage.InlineMediaSegment segment, InlineMediaCacheEntry entry
    ) {
        return switch (entry.state) {
            case READY -> TextureHandle.ready(entry.texture, entry.width, entry.height);
            case ERROR -> TextureHandle.error(
                    placeholderTextureFor(segment, true), FALLBACK_DIMENSION, FALLBACK_DIMENSION);
            case LOADING, NEW -> TextureHandle.loading(
                    placeholderTextureFor(segment, false), FALLBACK_DIMENSION, FALLBACK_DIMENSION);
        };
    }

    private static ResourceLocation resourceLocationFor(String url) {
        InlineMediaUrls.ResourceReference reference = InlineMediaUrls.parseResourceUrl(url);
        return ResourceLocation.fromNamespaceAndPath(reference.namespace(), reference.path());
    }

    public record Snapshot(
            int resident,
            int ready,
            int loading,
            int error,
            long downloadsStarted,
            long downloadsSucceeded,
            long downloadsFailed,
            long diskHits,
            long diskReloads,
            long memoryHits,
            long ttlEvictions,
            long capacityEvictions
    ) {
    }

    public record Dimensions(int width, int height) {
    }

    public record TextureHandle(
            ResourceLocation texture, int sourceWidth, int sourceHeight, boolean loading, boolean error
    ) {
        public static TextureHandle ready(ResourceLocation texture, int sourceWidth, int sourceHeight) {
            return new TextureHandle(texture, sourceWidth, sourceHeight, false, false);
        }

        public static TextureHandle loading(ResourceLocation texture, int sourceWidth, int sourceHeight) {
            return new TextureHandle(texture, sourceWidth, sourceHeight, true, false);
        }

        public static TextureHandle error(ResourceLocation texture, int sourceWidth, int sourceHeight) {
            return new TextureHandle(texture, sourceWidth, sourceHeight, false, true);
        }
    }

    void debugPutReady(String url, long lastAccessAt, long protectedUntilAt, int width, int height) {
        InlineMediaCacheEntry entry = new InlineMediaCacheEntry(InlineMediaCacheKeys.cacheKey(url), "debug", url);
        entry.state = InlineMediaCacheEntry.EntryState.READY;
        entry.texture = DEFAULT_AVATAR_TEXTURE;
        entry.width = width;
        entry.height = height;
        entry.lastAccessAt = lastAccessAt;
        entry.protectedUntilAt = protectedUntilAt;
        entries.put(entry.key, entry);
    }

    void debugPutLoading(String url, long lastAccessAt) {
        InlineMediaCacheEntry entry = new InlineMediaCacheEntry(InlineMediaCacheKeys.cacheKey(url), "debug", url);
        entry.state = InlineMediaCacheEntry.EntryState.LOADING;
        entry.lastAccessAt = lastAccessAt;
        entries.put(entry.key, entry);
    }

    void debugTouchVisible(String url, long now) {
        InlineMediaCacheEntry entry = entries.get(InlineMediaCacheKeys.cacheKey(url));
        if (entry != null) {
            entry.touch(now, true);
        }
    }

    boolean debugContains(String url) {
        return entries.containsKey(InlineMediaCacheKeys.cacheKey(url));
    }

    boolean debugIsRecentlyVisible(String url, long now) {
        InlineMediaCacheEntry entry = entries.get(InlineMediaCacheKeys.cacheKey(url));
        return entry != null && entry.isRecentlyVisible(now);
    }

    void debugCleanup(long now) {
        evictor.cleanup(now);
    }
}

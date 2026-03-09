package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import br.com.reinodoce.mctiktok.util.ExecutorsFactory;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineMediaCache {
    public static final ResourceLocation DEFAULT_AVATAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InlineMediaUrls.DEFAULT_NAMESPACE,
            InlineMediaUrls.DEFAULT_AVATAR_PATH
    );
    public static final ResourceLocation LOADING_TEXTURE = ResourceLocation.fromNamespaceAndPath(ReinodoceMcTiktokMod.MOD_ID, "textures/gui/emote_loading.png");
    public static final ResourceLocation ERROR_TEXTURE = ResourceLocation.fromNamespaceAndPath(ReinodoceMcTiktokMod.MOD_ID, "textures/gui/emote_error.png");

    private static final int MAX_MEMORY_ENTRIES = 256;
    private static final long ENTRY_TTL_MILLIS = Duration.ofMinutes(30).toMillis();
    private static final long ERROR_RETRY_MILLIS = Duration.ofSeconds(30).toMillis();
    private static final long RECENT_VISIBILITY_GRACE_MILLIS = Duration.ofSeconds(15).toMillis();

    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private final ExecutorService loader = ExecutorsFactory.newSingleThreadExecutor("reinodoce-inline-media");
    private final AtomicLong downloadsStarted = new AtomicLong();
    private final AtomicLong downloadsSucceeded = new AtomicLong();
    private final AtomicLong downloadsFailed = new AtomicLong();
    private final AtomicLong diskHits = new AtomicLong();
    private final AtomicLong diskReloads = new AtomicLong();
    private final AtomicLong memoryHits = new AtomicLong();
    private final AtomicLong ttlEvictions = new AtomicLong();
    private final AtomicLong capacityEvictions = new AtomicLong();

    public InlineMediaCache() {
        ImageIoBootstrap.ensureInitialized();
    }

    public void prefetch(RichLiveMessage message) {
        if (message == null) {
            return;
        }
        cleanup(System.currentTimeMillis());
        for (RichLiveMessage.InlineMediaSegment segment : message.inlineMediaSegments()) {
            if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
                continue;
            }
            ensureAvailable(segment, true, false);
        }
    }

    public TextureHandle resolve(RichLiveMessage.InlineMediaSegment segment) {
        if (segment == null) {
            return TextureHandle.error(ERROR_TEXTURE, 16, 16);
        }
        if (segment.sourceUrl().isBlank()) {
            if (segment.kind() == RichLiveMessage.InlineMediaKind.AVATAR) {
                return TextureHandle.ready(DEFAULT_AVATAR_TEXTURE, 16, 16);
            }
            return TextureHandle.error(ERROR_TEXTURE, 16, 16);
        }
        if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
            return TextureHandle.ready(resourceLocationFor(segment.sourceUrl()), 16, 16);
        }

        CacheEntry entry = ensureAvailable(segment, false, true);
        if (entry == null) {
            return TextureHandle.error(ERROR_TEXTURE, 16, 16);
        }

        return switch (entry.state) {
            case READY -> TextureHandle.ready(entry.texture, entry.width, entry.height);
            case ERROR -> TextureHandle.error(placeholderTextureFor(segment, true), 16, 16);
            case LOADING, NEW -> TextureHandle.loading(placeholderTextureFor(segment, false), 16, 16);
        };
    }

    public Dimensions dimensionsFor(RichLiveMessage.InlineMediaSegment segment) {
        if (segment == null || segment.sourceUrl().isBlank()) {
            return new Dimensions(16, 16);
        }
        if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
            return new Dimensions(16, 16);
        }

        CacheEntry entry = ensureAvailable(segment, false, true);
        if (entry == null) {
            return new Dimensions(16, 16);
        }
        return new Dimensions(Math.max(1, entry.width), Math.max(1, entry.height));
    }

    static ResourceLocation placeholderTextureFor(RichLiveMessage.InlineMediaSegment segment, boolean error) {
        if (segment != null && segment.kind() == RichLiveMessage.InlineMediaKind.AVATAR) {
            return DEFAULT_AVATAR_TEXTURE;
        }
        return error ? ERROR_TEXTURE : LOADING_TEXTURE;
    }

    public Snapshot snapshot() {
        int ready = 0;
        int loading = 0;
        int error = 0;
        int resident = 0;
        for (CacheEntry entry : entries.values()) {
            switch (entry.state) {
                case READY -> ready++;
                case LOADING -> loading++;
                case ERROR -> error++;
                case NEW -> {
                }
            }
            if (entry.texture != null) {
                resident++;
            }
        }
        return new Snapshot(
                resident,
                ready,
                loading,
                error,
                downloadsStarted.get(),
                downloadsSucceeded.get(),
                downloadsFailed.get(),
                diskHits.get(),
                diskReloads.get(),
                memoryHits.get(),
                ttlEvictions.get(),
                capacityEvictions.get()
        );
    }

    private CacheEntry ensureAvailable(RichLiveMessage.InlineMediaSegment segment, boolean countStats, boolean visibleAccess) {
        String sourceUrl = segment.sourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        long now = System.currentTimeMillis();
        CacheEntry entry = entries.computeIfAbsent(cacheKey(sourceUrl), key -> new CacheEntry(key, segment.kind().id(), segment.sourceKey(), sourceUrl));
        entry.touch(now, visibleAccess);

        synchronized (entry.monitor) {
            if (entry.state == EntryState.READY) {
                if (countStats) {
                    memoryHits.incrementAndGet();
                }
                return entry;
            }

            if (entry.state == EntryState.LOADING) {
                return entry;
            }

            if (entry.state == EntryState.ERROR && (now - entry.lastFailureAt) < ERROR_RETRY_MILLIS) {
                return entry;
            }

            entry.state = EntryState.LOADING;
            if (Files.exists(entry.payloadPath())) {
                if (countStats) {
                    diskHits.incrementAndGet();
                }
                diskReloads.incrementAndGet();
                loader.submit(() -> loadFromDisk(entry));
            } else {
                downloadsStarted.incrementAndGet();
                loader.submit(() -> downloadFromNetwork(entry));
            }
            return entry;
        }
    }

    private void loadFromDisk(CacheEntry entry) {
        try {
            byte[] bytes = Files.readAllBytes(entry.payloadPath());
            InlineMediaMetadata metadata = readMetadata(entry.metadataPath()).withDefaults(entry.sourceUrl, entry.kind);
            LoadedMedia loaded = decode(bytes, metadata.contentType());
            registerTexture(entry, loaded, metadata.withLastUsedAt(System.currentTimeMillis()));
            writeMetadata(entry.metadataPath(), metadata.withStatus("ready"));
        } catch (Exception exception) {
            markFailure(entry, "disk-load", exception);
        }
    }

    private void downloadFromNetwork(CacheEntry entry) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(entry.sourceUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7_500);
            connection.setReadTimeout(7_500);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "ReinoDoce-MCTikTok/0.1.0");

            byte[] bytes;
            try (InputStream inputStream = connection.getInputStream()) {
                bytes = inputStream.readAllBytes();
            }

            String contentType = connection.getContentType();
            LoadedMedia loaded = decode(bytes, contentType);
            long now = System.currentTimeMillis();
            writePayload(entry.payloadPath(), bytes);
            writeMetadata(entry.metadataPath(), new InlineMediaMetadata(
                    entry.sourceUrl,
                    entry.kind,
                    contentType == null ? "application/octet-stream" : contentType,
                    bytes.length,
                    loaded.width(),
                    loaded.height(),
                    now,
                    now,
                    "ready"
            ));
            registerTexture(entry, loaded, null);
            downloadsSucceeded.incrementAndGet();
            ReinodoceLogger.LOGGER.info(
                    "Inline media downloaded kind={} url={} contentType={} bytes={} size={}x{}",
                    entry.kind,
                    entry.sourceUrl,
                    contentType,
                    bytes.length,
                    loaded.width(),
                    loaded.height()
            );
        } catch (Exception exception) {
            markFailure(entry, "network-download", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private LoadedMedia decode(byte[] bytes, String contentType) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("ImageIO returned null for contentType=" + contentType);
            }
            NativeImage nativeImage = toNativeImage(image);
            return new LoadedMedia(nativeImage, nativeImage.getWidth(), nativeImage.getHeight());
        }
    }

    private void registerTexture(CacheEntry entry, LoadedMedia loaded, InlineMediaMetadata metadataToPersist) {
        Minecraft.getInstance().execute(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    ReinodoceMcTiktokMod.MOD_ID,
                    "dynamic/inline_media/" + UUID.nameUUIDFromBytes(entry.key.getBytes(StandardCharsets.UTF_8))
            );
            minecraft.getTextureManager().register(location, new DynamicTexture(loaded.nativeImage()));
            entry.texture = location;
            entry.width = loaded.width();
            entry.height = loaded.height();
            entry.state = EntryState.READY;
            if (metadataToPersist != null) {
                try {
                    writeMetadata(entry.metadataPath(), metadataToPersist.withStatus("ready"));
                } catch (IOException exception) {
                    ReinodoceLogger.LOGGER.warn("Failed to update inline media metadata for {}", entry.sourceUrl, exception);
                }
            }
        });
    }

    private void markFailure(CacheEntry entry, String phase, Exception exception) {
        entry.state = EntryState.ERROR;
        entry.lastFailureAt = System.currentTimeMillis();
        downloadsFailed.incrementAndGet();
        try {
            writeMetadata(entry.metadataPath(), new InlineMediaMetadata(
                    entry.sourceUrl,
                    entry.kind,
                    "unknown",
                    0L,
                    0,
                    0,
                    entry.lastFailureAt,
                    entry.lastFailureAt,
                    "error"
            ));
        } catch (IOException ignored) {
        }
        ReinodoceLogger.LOGGER.warn("Failed to {} inline media kind={} url={}", phase, entry.kind, entry.sourceUrl, exception);
    }

    private void cleanup(long now) {
        List<CacheEntry> ttlCandidates = new ArrayList<>();
        for (CacheEntry entry : entries.values()) {
            if (shouldEvictByTtl(entry, now)) {
                ttlCandidates.add(entry);
            }
        }
        for (CacheEntry entry : ttlCandidates) {
            evictEntry(entry, EvictionReason.TTL);
        }

        int overflow = entries.size() - MAX_MEMORY_ENTRIES;
        if (overflow <= 0) {
            return;
        }

        List<CacheEntry> capacityCandidates = new ArrayList<>();
        for (CacheEntry entry : entries.values()) {
            if (isCapacityEvictionCandidate(entry, now)) {
                capacityCandidates.add(entry);
            }
        }
        capacityCandidates.sort(Comparator.comparingLong(candidate -> candidate.lastAccessAt));
        for (CacheEntry entry : capacityCandidates) {
            if (entries.size() <= MAX_MEMORY_ENTRIES) {
                break;
            }
            evictEntry(entry, EvictionReason.CAPACITY);
        }
    }

    private boolean shouldEvictByTtl(CacheEntry entry, long now) {
        return entry.state != EntryState.LOADING
                && !entry.isRecentlyVisible(now)
                && now - entry.lastAccessAt > ENTRY_TTL_MILLIS;
    }

    private boolean isCapacityEvictionCandidate(CacheEntry entry, long now) {
        return entry.state != EntryState.LOADING && !entry.isRecentlyVisible(now);
    }

    private void evictEntry(CacheEntry entry, EvictionReason reason) {
        CacheEntry removed = entries.remove(entry.key);
        if (removed == null) {
            return;
        }

        if (reason == EvictionReason.TTL) {
            ttlEvictions.incrementAndGet();
        } else {
            capacityEvictions.incrementAndGet();
        }

        ResourceLocation texture = removed.texture;
        removed.texture = null;
        if (texture != null) {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null) {
                    minecraft.execute(() -> minecraft.getTextureManager().release(texture));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void writePayload(Path payloadPath, byte[] bytes) throws IOException {
        Files.createDirectories(payloadPath.getParent());
        Files.write(payloadPath, bytes);
    }

    private InlineMediaMetadata readMetadata(Path metadataPath) throws IOException {
        if (!Files.exists(metadataPath)) {
            return new InlineMediaMetadata("", "unknown", "application/octet-stream", 0L, 0, 0, 0L, 0L, "ready");
        }
        return InlineMediaMetadata.fromJson(Files.readString(metadataPath, StandardCharsets.UTF_8));
    }

    private void writeMetadata(Path metadataPath, InlineMediaMetadata metadata) throws IOException {
        Files.createDirectories(metadataPath.getParent());
        Files.writeString(metadataPath, metadata.toJson(), StandardCharsets.UTF_8);
    }

    private NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int rgba = ((argb >> 24) & 0xFF) << 24
                        | (argb & 0xFF) << 16
                        | (argb & 0xFF00)
                        | ((argb >> 16) & 0xFF);
                nativeImage.setPixelRGBA(x, y, rgba);
            }
        }
        return nativeImage;
    }

    private String cacheKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash inline media URL", exception);
        }
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

    void debugPutReady(String url, long lastAccessAt, long protectedUntilAt, int width, int height) {
        CacheEntry entry = new CacheEntry(cacheKey(url), "debug", url, url);
        entry.state = EntryState.READY;
        entry.texture = DEFAULT_AVATAR_TEXTURE;
        entry.width = width;
        entry.height = height;
        entry.lastAccessAt = lastAccessAt;
        entry.protectedUntilAt = protectedUntilAt;
        entries.put(entry.key, entry);
    }

    void debugPutLoading(String url, long lastAccessAt) {
        CacheEntry entry = new CacheEntry(cacheKey(url), "debug", url, url);
        entry.state = EntryState.LOADING;
        entry.lastAccessAt = lastAccessAt;
        entries.put(entry.key, entry);
    }

    void debugTouchVisible(String url, long now) {
        CacheEntry entry = entries.get(cacheKey(url));
        if (entry != null) {
            entry.touch(now, true);
        }
    }

    boolean debugContains(String url) {
        return entries.containsKey(cacheKey(url));
    }

    boolean debugIsRecentlyVisible(String url, long now) {
        CacheEntry entry = entries.get(cacheKey(url));
        return entry != null && entry.isRecentlyVisible(now);
    }

    void debugCleanup(long now) {
        cleanup(now);
    }

    private static ResourceLocation resourceLocationFor(String url) {
        InlineMediaUrls.ResourceReference reference = InlineMediaUrls.parseResourceUrl(url);
        return ResourceLocation.fromNamespaceAndPath(reference.namespace(), reference.path());
    }

    public record TextureHandle(ResourceLocation texture, int sourceWidth, int sourceHeight, boolean loading, boolean error) {
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

    private record LoadedMedia(NativeImage nativeImage, int width, int height) {
    }

    private enum EntryState {
        NEW,
        LOADING,
        READY,
        ERROR
    }

    private static final class CacheEntry {
        private final Object monitor = new Object();
        private final String key;
        private final String kind;
        private final String sourceKey;
        private final String sourceUrl;
        private volatile EntryState state = EntryState.NEW;
        private volatile ResourceLocation texture;
        private volatile int width = 16;
        private volatile int height = 16;
        private volatile long lastAccessAt = System.currentTimeMillis();
        private volatile long protectedUntilAt;
        private volatile long lastFailureAt;

        private CacheEntry(String key, String kind, String sourceKey, String sourceUrl) {
            this.key = key;
            this.kind = kind;
            this.sourceKey = sourceKey;
            this.sourceUrl = sourceUrl;
        }

        private Path directory() {
            return Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("cache")
                    .resolve(ReinodoceMcTiktokMod.MOD_ID)
                    .resolve("inline-media")
                    .resolve(key);
        }

        private Path payloadPath() {
            return directory().resolve("payload");
        }

        private Path metadataPath() {
            return directory().resolve("metadata.json");
        }

        private void touch(long now, boolean visibleAccess) {
            lastAccessAt = now;
            if (visibleAccess) {
                protectedUntilAt = Math.max(protectedUntilAt, now + RECENT_VISIBILITY_GRACE_MILLIS);
            }
        }

        private boolean isRecentlyVisible(long now) {
            return protectedUntilAt > now;
        }
    }

    private enum EvictionReason {
        TTL,
        CAPACITY
    }

    private record InlineMediaMetadata(
            String sourceUrl,
            String kind,
            String contentType,
            long byteSize,
            int width,
            int height,
            long fetchedAt,
            long lastUsedAt,
            String status
    ) {
        private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        private static final Pattern NUMBER_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");

        private InlineMediaMetadata withStatus(String newStatus) {
            return new InlineMediaMetadata(sourceUrl, kind, contentType, byteSize, width, height, fetchedAt, lastUsedAt, newStatus);
        }

        private InlineMediaMetadata withLastUsedAt(long newLastUsedAt) {
            return new InlineMediaMetadata(sourceUrl, kind, contentType, byteSize, width, height, fetchedAt, newLastUsedAt, status);
        }

        private InlineMediaMetadata withDefaults(String defaultSourceUrl, String defaultKind) {
            return new InlineMediaMetadata(
                    sourceUrl == null || sourceUrl.isBlank() ? defaultSourceUrl : sourceUrl,
                    kind == null || kind.isBlank() ? defaultKind : kind,
                    contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType,
                    byteSize,
                    width,
                    height,
                    fetchedAt,
                    lastUsedAt,
                    status == null || status.isBlank() ? "ready" : status
            );
        }

        private String toJson() {
            return "{\n"
                    + "  \"sourceUrl\": \"" + escape(sourceUrl) + "\",\n"
                    + "  \"kind\": \"" + escape(kind) + "\",\n"
                    + "  \"contentType\": \"" + escape(contentType) + "\",\n"
                    + "  \"byteSize\": " + byteSize + ",\n"
                    + "  \"width\": " + width + ",\n"
                    + "  \"height\": " + height + ",\n"
                    + "  \"fetchedAt\": " + fetchedAt + ",\n"
                    + "  \"lastUsedAt\": " + lastUsedAt + ",\n"
                    + "  \"status\": \"" + escape(status) + "\"\n"
                    + "}\n";
        }

        private static InlineMediaMetadata fromJson(String json) {
            return new InlineMediaMetadata(
                    extractString(json, "sourceUrl", ""),
                    extractString(json, "kind", "unknown"),
                    extractString(json, "contentType", "application/octet-stream"),
                    extractLong(json, "byteSize", 0L),
                    (int) extractLong(json, "width", 0L),
                    (int) extractLong(json, "height", 0L),
                    extractLong(json, "fetchedAt", 0L),
                    extractLong(json, "lastUsedAt", 0L),
                    extractString(json, "status", "ready")
            );
        }

        private static String extractString(String json, String name, String defaultValue) {
            Matcher matcher = STRING_PATTERN.matcher(json);
            while (matcher.find()) {
                if (name.equals(matcher.group(1))) {
                    return unescape(matcher.group(2));
                }
            }
            return defaultValue;
        }

        private static long extractLong(String json, String name, long defaultValue) {
            Matcher matcher = NUMBER_PATTERN.matcher(json);
            while (matcher.find()) {
                if (name.equals(matcher.group(1))) {
                    return Long.parseLong(matcher.group(2));
                }
            }
            return defaultValue;
        }

        private static String escape(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
        }

        private static String unescape(String value) {
            return value
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
    }
}

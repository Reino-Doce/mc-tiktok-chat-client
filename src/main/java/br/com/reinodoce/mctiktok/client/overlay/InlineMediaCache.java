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
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineMediaCache {
    public static final ResourceLocation LOADING_TEXTURE = new ResourceLocation(ReinodoceMcTiktokMod.MOD_ID, "textures/gui/emote_loading.png");
    public static final ResourceLocation ERROR_TEXTURE = new ResourceLocation(ReinodoceMcTiktokMod.MOD_ID, "textures/gui/emote_error.png");

    private static final int MAX_MEMORY_ENTRIES = 256;
    private static final long ENTRY_TTL_MILLIS = Duration.ofMinutes(30).toMillis();
    private static final long ERROR_RETRY_MILLIS = Duration.ofSeconds(30).toMillis();

    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private final ExecutorService loader = ExecutorsFactory.newSingleThreadExecutor("reinodoce-inline-media");
    private final AtomicLong downloadsStarted = new AtomicLong();
    private final AtomicLong downloadsSucceeded = new AtomicLong();
    private final AtomicLong downloadsFailed = new AtomicLong();
    private final AtomicLong diskHits = new AtomicLong();
    private final AtomicLong memoryHits = new AtomicLong();

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
            ensureAvailable(segment, true);
        }
    }

    public TextureHandle resolve(RichLiveMessage.InlineMediaSegment segment) {
        if (segment == null || segment.sourceUrl().isBlank()) {
            return TextureHandle.error(ERROR_TEXTURE, 16, 16);
        }
        if (InlineMediaUrls.isResourceUrl(segment.sourceUrl())) {
            return TextureHandle.ready(InlineMediaUrls.parseResourceUrl(segment.sourceUrl()), 16, 16);
        }

        CacheEntry entry = ensureAvailable(segment, false);
        if (entry == null) {
            return TextureHandle.error(ERROR_TEXTURE, 16, 16);
        }

        return switch (entry.state) {
            case READY -> TextureHandle.ready(entry.texture, entry.width, entry.height);
            case ERROR -> TextureHandle.error(ERROR_TEXTURE, 16, 16);
            case LOADING, NEW -> TextureHandle.loading(LOADING_TEXTURE, 16, 16);
        };
    }

    public Snapshot snapshot() {
        int ready = 0;
        int loading = 0;
        int error = 0;
        for (CacheEntry entry : entries.values()) {
            switch (entry.state) {
                case READY -> ready++;
                case LOADING -> loading++;
                case ERROR -> error++;
                case NEW -> {
                }
            }
        }
        return new Snapshot(
                ready,
                loading,
                error,
                downloadsStarted.get(),
                downloadsSucceeded.get(),
                downloadsFailed.get(),
                diskHits.get(),
                memoryHits.get()
        );
    }

    private CacheEntry ensureAvailable(RichLiveMessage.InlineMediaSegment segment, boolean countStats) {
        String sourceUrl = segment.sourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        long now = System.currentTimeMillis();
        CacheEntry entry = entries.computeIfAbsent(cacheKey(sourceUrl), key -> new CacheEntry(key, segment.kind().id(), segment.sourceKey(), sourceUrl));
        entry.lastAccessAt = now;

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
                loader.submit(() -> loadFromDisk(entry));
            } else {
                if (countStats) {
                    downloadsStarted.incrementAndGet();
                }
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
            ResourceLocation location = new ResourceLocation(
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
        if (entries.size() <= MAX_MEMORY_ENTRIES) {
            entries.entrySet().removeIf(entry -> shouldEvict(entry.getValue(), now));
            return;
        }
        entries.entrySet().removeIf(entry -> shouldEvict(entry.getValue(), now) || entries.size() > MAX_MEMORY_ENTRIES);
    }

    private boolean shouldEvict(CacheEntry entry, long now) {
        if (entry.state == EntryState.LOADING) {
            return false;
        }
        return now - entry.lastAccessAt > ENTRY_TTL_MILLIS;
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
            int ready,
            int loading,
            int error,
            long downloadsStarted,
            long downloadsSucceeded,
            long downloadsFailed,
            long diskHits,
            long memoryHits
    ) {
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

package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;
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
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

final class InlineMediaDownloader {
    private static final String USER_AGENT = "ReinoDoce-MCTikTok/0.1.0";
    private static final String HTTP_GET = "GET";
    private static final int HTTP_TIMEOUT_MILLIS = 7_500;
    private static final int ARGB_ALPHA_SHIFT = 24;
    private static final int ARGB_RED_SHIFT = 16;
    private static final int BYTE_MASK = 0xFF;
    private static final int GREEN_MASK = 0xFF00;

    private final InlineMediaCacheStats stats;

    InlineMediaDownloader(InlineMediaCacheStats stats) {
        this.stats = stats;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void loadFromDisk(InlineMediaCacheEntry entry) {
        try {
            byte[] bytes = Files.readAllBytes(entry.payloadPath());
            InlineMediaMetadata metadata = InlineMediaMetadata.read(entry.metadataPath())
                    .withDefaults(entry.sourceUrl, entry.kind);
            LoadedMedia loaded = decode(bytes, metadata.contentType());
            registerTexture(entry, loaded, metadata.withLastUsedAt(System.currentTimeMillis()));
            metadata.withStatus(InlineMediaMetadata.STATUS_READY).writeTo(entry.metadataPath());
        } catch (IOException | RuntimeException exception) {
            markFailure(entry, "disk-load", exception);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void downloadFromNetwork(InlineMediaCacheEntry entry) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(entry.sourceUrl);
            byte[] bytes = readAllBytes(connection);
            String contentType = connection.getContentType();
            LoadedMedia loaded = decode(bytes, contentType);
            writePayload(entry.payloadPath(), bytes);
            long now = System.currentTimeMillis();
            persistDownloadMetadata(entry, bytes.length, loaded, contentType, now);
            registerTexture(entry, loaded, null);
            stats.recordDownloadSucceeded();
            logDownloadSuccess(entry, contentType, bytes.length, loaded);
        } catch (IOException | RuntimeException exception) {
            markFailure(entry, "network-download", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(String sourceUrl) throws IOException {
        URLConnection raw = new URL(sourceUrl).openConnection();
        if (!(raw instanceof HttpURLConnection connection)) {
            throw new IOException("Unsupported URL scheme for inline media (not HTTP/HTTPS): " + sourceUrl);
        }
        connection.setRequestMethod(HTTP_GET);
        connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
        connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    private static byte[] readAllBytes(HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private static void persistDownloadMetadata(
            InlineMediaCacheEntry entry,
            int byteSize,
            LoadedMedia loaded,
            String contentType,
            long now
    ) throws IOException {
        String resolvedContentType = contentType == null ? InlineMediaMetadata.DEFAULT_CONTENT_TYPE : contentType;
        new InlineMediaMetadata(
                entry.sourceUrl,
                entry.kind,
                resolvedContentType,
                byteSize,
                loaded.width(),
                loaded.height(),
                now,
                now,
                InlineMediaMetadata.STATUS_READY).writeTo(entry.metadataPath());
    }

    private static LoadedMedia decode(byte[] bytes, String contentType) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("ImageIO returned null for contentType=" + contentType);
            }
            NativeImage nativeImage = toNativeImage(image);
            return new LoadedMedia(nativeImage, nativeImage.getWidth(), nativeImage.getHeight());
        }
    }

    private static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setPixelRGBA(x, y, swapArgbToRgba(image.getRGB(x, y)));
            }
        }
        return nativeImage;
    }

    private static int swapArgbToRgba(int argb) {
        int alpha = (argb >> ARGB_ALPHA_SHIFT) & BYTE_MASK;
        int red = (argb >> ARGB_RED_SHIFT) & BYTE_MASK;
        int green = argb & GREEN_MASK;
        int blue = argb & BYTE_MASK;
        return (alpha << ARGB_ALPHA_SHIFT) | (blue << ARGB_RED_SHIFT) | green | red;
    }

    private void registerTexture(
            InlineMediaCacheEntry entry, LoadedMedia loaded, InlineMediaMetadata metadataToPersist
    ) {
        Minecraft.getInstance().execute(() -> applyTexture(entry, loaded, metadataToPersist));
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void applyTexture(
            InlineMediaCacheEntry entry, LoadedMedia loaded, InlineMediaMetadata metadataToPersist
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                ReinodoceMcTiktokMod.MOD_ID,
                "dynamic/inline_media/"
                        + UUID.nameUUIDFromBytes(entry.key.getBytes(StandardCharsets.UTF_8)));
        minecraft.getTextureManager().register(location, new DynamicTexture(loaded.nativeImage()));
        entry.texture = location;
        entry.width = loaded.width();
        entry.height = loaded.height();
        entry.state = InlineMediaCacheEntry.EntryState.READY;
        if (metadataToPersist != null) {
            persistReady(entry, metadataToPersist);
        }
    }

    private static void persistReady(InlineMediaCacheEntry entry, InlineMediaMetadata metadataToPersist) {
        try {
            metadataToPersist.withStatus(InlineMediaMetadata.STATUS_READY).writeTo(entry.metadataPath());
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.warn(
                    "Failed to update inline media metadata for {}", entry.sourceUrl, exception);
        }
    }

    private void markFailure(InlineMediaCacheEntry entry, String phase, Exception exception) {
        entry.state = InlineMediaCacheEntry.EntryState.ERROR;
        entry.lastFailureAt = System.currentTimeMillis();
        stats.recordDownloadFailed();
        writeFailureMetadata(entry);
        ReinodoceLogger.LOGGER.warn(
                "Failed to {} inline media kind={} url={}",
                phase, entry.kind, entry.sourceUrl, exception);
    }

    private static void writeFailureMetadata(InlineMediaCacheEntry entry) {
        try {
            new InlineMediaMetadata(
                    entry.sourceUrl,
                    entry.kind,
                    InlineMediaMetadata.DEFAULT_KIND,
                    0L,
                    0,
                    0,
                    entry.lastFailureAt,
                    entry.lastFailureAt,
                    InlineMediaMetadata.STATUS_ERROR).writeTo(entry.metadataPath());
        } catch (IOException ignored) {
            // failure metadata is best-effort; entry is already marked ERROR.
        }
    }

    private static void writePayload(Path payloadPath, byte[] bytes) throws IOException {
        Path parent = payloadPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(payloadPath, bytes);
    }

    private static void logDownloadSuccess(
            InlineMediaCacheEntry entry, String contentType, int byteSize, LoadedMedia loaded
    ) {
        ReinodoceLogger.LOGGER.info(
                "Inline media downloaded kind={} url={} contentType={} bytes={} size={}x{}",
                entry.kind,
                entry.sourceUrl,
                contentType,
                byteSize,
                loaded.width(),
                loaded.height());
    }

    record LoadedMedia(NativeImage nativeImage, int width, int height) {
    }
}

package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

final class InlineMediaDownloader {
    private final InlineMediaCacheStats stats;

    InlineMediaDownloader(InlineMediaCacheStats stats) {
        this.stats = stats;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void loadFromDisk(InlineMediaCacheEntry entry) {
        try {
            byte[] bytes = Files.readAllBytes(entry.payloadPath());
            InlineMediaMetadata metadata = InlineMediaMetadataStore.readWithEntryDefaults(entry);
            InlineMediaLoadedMedia loaded = InlineMediaImageDecoder.decode(bytes, metadata.contentType());
            long now = System.currentTimeMillis();
            InlineMediaMetadata updatedMetadata = metadata.withLastUsedAt(now);
            registerTexture(entry, loaded, updatedMetadata);
        } catch (IOException | RuntimeException exception) {
            markFailure(entry, "disk-load", exception);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void downloadFromNetwork(InlineMediaCacheEntry entry) {
        try {
            InlineMediaNetworkClient.Response response = InlineMediaNetworkClient.download(entry.sourceUrl);
            byte[] bytes = response.bytes();
            String contentType = response.contentType();
            InlineMediaLoadedMedia loaded = InlineMediaImageDecoder.decode(bytes, contentType);
            writePayload(entry.payloadPath(), bytes);
            long now = System.currentTimeMillis();
            persistDownloadMetadata(entry, bytes.length, loaded, contentType, now);
            registerTexture(entry, loaded, null);
            stats.recordDownloadSucceeded();
            logDownloadSuccess(entry, contentType, bytes.length, loaded);
        } catch (IOException | RuntimeException exception) {
            markFailure(entry, "network-download", exception);
        }
    }

    private static void persistDownloadMetadata(
            InlineMediaCacheEntry entry,
            int byteSize,
            InlineMediaLoadedMedia loaded,
            String contentType,
            long now
    ) throws IOException {
        String resolvedContentType = contentType == null ? InlineMediaMetadata.DEFAULT_CONTENT_TYPE : contentType;
        InlineMediaMetadataStore.writeReady(entry, new InlineMediaMetadata(
                entry.sourceReference(),
                entry.kind,
                resolvedContentType,
                byteSize,
                loaded.width(),
                loaded.height(),
                now,
                now,
                InlineMediaMetadata.STATUS_READY), now);
    }

    private void registerTexture(
            InlineMediaCacheEntry entry, InlineMediaLoadedMedia loaded, InlineMediaMetadata metadataToPersist
    ) {
        Minecraft.getInstance().execute(() -> applyTexture(entry, loaded, metadataToPersist));
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void applyTexture(
            InlineMediaCacheEntry entry, InlineMediaLoadedMedia loaded, InlineMediaMetadata metadataToPersist
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
            InlineMediaMetadataStore.writeReady(entry, metadataToPersist, metadataToPersist.lastUsedAt());
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.warn(
                    "Failed to update inline media metadata for {}", entry.sourceReference(), exception);
        }
    }

    private void markFailure(InlineMediaCacheEntry entry, String phase, Exception exception) {
        entry.state = InlineMediaCacheEntry.EntryState.ERROR;
        entry.lastFailureAt = System.currentTimeMillis();
        stats.recordDownloadFailed();
        writeFailureMetadata(entry);
        ReinodoceLogger.LOGGER.warn(
                "Failed to {} inline media kind={} source={}",
                phase, entry.kind, entry.sourceReference(), exception);
    }

    private static void writeFailureMetadata(InlineMediaCacheEntry entry) {
        try {
            new InlineMediaMetadata(
                    entry.sourceReference(),
                    entry.kind,
                    InlineMediaMetadata.DEFAULT_KIND,
                    0L,
                    0,
                    0,
                    entry.lastFailureAt,
                    entry.lastFailureAt,
                    InlineMediaMetadata.STATUS_ERROR).writeTo(entry.metadataPath());
            entry.lastMetadataTouchAt = entry.lastFailureAt;
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
            InlineMediaCacheEntry entry, String contentType, int byteSize, InlineMediaLoadedMedia loaded
    ) {
        ReinodoceLogger.LOGGER.info(
                "Inline media downloaded kind={} source={} contentType={} bytes={} size={}x{}",
                entry.kind,
                entry.sourceReference(),
                contentType,
                byteSize,
                loaded.width(),
                loaded.height());
    }

    static boolean isSupportedContentType(String contentType) {
        return InlineMediaImageDecoder.isSupportedContentType(contentType);
    }

    static void validateDimensions(int width, int height) throws IOException {
        InlineMediaImageDecoder.validateDimensions(width, height);
    }
}

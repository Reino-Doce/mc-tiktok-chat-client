package br.com.reinodoce.mctiktok.client.overlay;

import java.io.IOException;

final class InlineMediaMetadataStore {
    private InlineMediaMetadataStore() {
    }

    static InlineMediaMetadata readWithEntryDefaults(InlineMediaCacheEntry entry) throws IOException {
        return InlineMediaMetadata.read(entry.metadataPath())
                .withDefaults(entry.sourceReference(), entry.kind)
                .withSourceUrl(entry.sourceReference());
    }

    static void writeReady(InlineMediaCacheEntry entry, InlineMediaMetadata metadata, long lastUsedAt)
            throws IOException {
        metadata.withDefaults(entry.sourceReference(), entry.kind)
                .withSourceUrl(entry.sourceReference())
                .withLastUsedAt(lastUsedAt)
                .withStatus(InlineMediaMetadata.STATUS_READY)
                .writeTo(entry.metadataPath());
        entry.lastMetadataTouchAt = lastUsedAt;
    }
}

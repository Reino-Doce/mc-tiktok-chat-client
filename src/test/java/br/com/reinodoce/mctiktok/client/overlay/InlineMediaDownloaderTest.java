package br.com.reinodoce.mctiktok.client.overlay;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineMediaDownloaderTest {

    @Test
    void acceptsOnlyExpectedRasterImageContentTypes() {
        assertTrue(InlineMediaDownloader.isSupportedContentType("image/png"));
        assertTrue(InlineMediaDownloader.isSupportedContentType("image/webp; charset=binary"));

        assertFalse(InlineMediaDownloader.isSupportedContentType(null));
        assertFalse(InlineMediaDownloader.isSupportedContentType("image/svg+xml"));
        assertFalse(InlineMediaDownloader.isSupportedContentType("text/html"));
    }

    @Test
    void enforcesDecodedPixelLimit() {
        assertDoesNotThrow(() -> InlineMediaDownloader.validateDimensions(512, 512));

        assertThrows(IOException.class, () -> InlineMediaDownloader.validateDimensions(0, 16));
        assertThrows(IOException.class, () -> InlineMediaDownloader.validateDimensions(2048, 2048));
    }
}

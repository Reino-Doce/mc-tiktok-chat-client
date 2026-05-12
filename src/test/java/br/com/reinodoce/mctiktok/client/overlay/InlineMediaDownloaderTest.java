package br.com.reinodoce.mctiktok.client.overlay;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

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

    @Test
    void rejectsNonPublicRemoteMediaHosts() {
        assertThrows(
                IOException.class,
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://127.0.0.1/avatar.png")));
        assertThrows(
                IOException.class,
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://192.168.1.10/avatar.png")));
    }

    @Test
    void ipv4SpecialUseBlocksOnlyDocumentedPrefixes() {
        assertThrows(
                IOException.class,
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://192.0.2.1/avatar.png")));
        assertThrows(
                IOException.class,
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://198.51.100.1/avatar.png")));
        assertThrows(
                IOException.class,
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://203.0.113.1/avatar.png")));

        assertDoesNotThrow(
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://192.0.3.1/avatar.png")));
        assertDoesNotThrow(
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://198.51.42.1/avatar.png")));
        assertDoesNotThrow(
                () -> InlineMediaAddressPolicy.validatePublicRemote(new URL("https://203.0.42.1/avatar.png")));
    }
}

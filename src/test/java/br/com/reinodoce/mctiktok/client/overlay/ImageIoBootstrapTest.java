package br.com.reinodoce.mctiktok.client.overlay;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageIoBootstrapTest {
    @Test
    void decodesRealWebpFixture() throws Exception {
        ImageIoBootstrap.ensureInitialized();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("fixtures/inline-media-sample.webp")) {
            assertNotNull(inputStream);
            BufferedImage image = ImageIO.read(inputStream);
            assertNotNull(image);
            assertTrue(image.getWidth() > 0);
            assertTrue(image.getHeight() > 0);
        }
    }
}

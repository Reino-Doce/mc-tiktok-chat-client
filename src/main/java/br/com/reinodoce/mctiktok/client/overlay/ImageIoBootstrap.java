package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import javax.imageio.ImageIO;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One-time ImageIO service-provider bootstrap for bundled inline media decoders.
 */
public final class ImageIoBootstrap {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private ImageIoBootstrap() {
    }

    /**
     * Scans ImageIO providers once so bundled WebP support is visible before media decoding.
     */
    public static void ensureInitialized() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        ImageIO.scanForPlugins();
        ReinodoceLogger.LOGGER.info("Initialized ImageIO plugins for inline media");
    }
}
